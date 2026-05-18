package com.sqldpass.service.mockexam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.extern.slf4j.Slf4j;

/**
 * 미니 모의고사 일괄 생성기.
 *
 * <p>어드민 한 번의 액션으로 examType 의 현재 풀(includedInMiniAt IS NULL 인 전문가 검수 문제)을
 * 비율 보존하며 가능한 한 많은 회차를 일괄 생성한다.
 *
 * <p>비율 정책:
 * <ul>
 *   <li>과목 분포: 각 시험 종목의 원본 분포 × 0.4 (미니 = 본 시험의 약 40%). 정처기 실기는
 *       사용자 결정으로 코드 카테고리 비중을 살린 8문 슬롯.</li>
 *   <li>출처 분포: 기출(PAST_EXAM) : AI 무료 : AI 프리미엄 = 1 : 1 : 1 (잔여는 PAST_EXAM 순서로 +1).</li>
 * </ul>
 *
 * <p>회차 수 결정: 모든 (과목, 출처) 슬롯 중 가장 빠듯한 풀이 가능 회차 수를 결정한다.
 * 풀 잔여분은 그대로 유지돼 다음 풀 보충 시 추가 회차로 만들어진다.
 *
 * <p>각 회차는 새 MockExamEntity (visibility=PREMIUM, kind=MINI, expertVerified=true) 로
 * 저장되고, 풀에서 뽑은 원본 문제는 복제(QuestionEntity 복제 생성자)되어 새 회차에 link 된다.
 * 원본 문제에는 includedInMiniAt 가 세팅돼 다음 미니 생성 시 풀에서 제외된다.
 */
@Slf4j
@Component
public class MiniMockExamCreator {

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final Random random = new Random();

    public MiniMockExamCreator(MockExamRepository mockExamRepository,
                               QuestionRepository questionRepository,
                               SubjectRepository subjectRepository) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
    }

    /** 출처 3종 — 풀 분류 + 회차당 quota 분배에 사용. */
    public enum Source {
        PAST_EXAM(MockExamKind.PAST_EXAM, null),
        AI_PUBLISHED(MockExamKind.AI, MockExamVisibility.PUBLISHED),
        AI_PREMIUM(MockExamKind.AI, MockExamVisibility.PREMIUM);

        final MockExamKind kind;
        final MockExamVisibility visibility;

        Source(MockExamKind kind, MockExamVisibility visibility) {
            this.kind = kind;
            this.visibility = visibility;
        }
    }

    /** 어드민 일괄 생성 결과 — 회차 ID 리스트 + 잔여 풀 통계 + 적용된 난이도 필터. */
    public record GenerationResult(
            ExamType examType,
            List<Long> createdMockExamIds,
            int createdCount,
            /** 출처별 잔여 풀 (해당 examType 전체 과목 합). 다음 풀 보충 시 추가 회차 가능 여부 가늠. */
            Map<Source, Long> remainingBySource,
            /** 풀 필터에 적용된 난이도 (1~4). null 이면 전체 난이도 사용. */
            Integer appliedDifficulty
    ) {}

    /**
     * 미니 모의고사 일괄 생성 — 비율 보존하며 만들 수 있는 모든 회차 생성.
     * 풀이 부족해 0회차가 만들어지면 INSUFFICIENT_QUESTIONS 던짐.
     *
     * @param difficulty 1~4 (EASY/NORMAL/HARD/VERY_HARD). null 이면 전체 난이도 사용.
     */
    @Transactional
    public GenerationResult createAllFromPool(ExamType examType, Integer difficulty) {
        if (examType == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "examType 은 필수입니다.");
        }
        if (difficulty != null && (difficulty < 1 || difficulty > 4)) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "difficulty 는 1~4 사이여야 합니다.");
        }

        LinkedHashMap<SubjectEntity, Integer> quotas = resolveSubjectQuotas(examType);
        if (quotas.isEmpty()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "미니 모의고사 분포가 정의되지 않은 examType: " + examType);
        }

        // (과목, 출처) → 풀 후보 리스트 (셔플된 mutable 리스트)
        Map<Long, Map<Source, List<QuestionEntity>>> poolMap = new HashMap<>();
        for (SubjectEntity subject : quotas.keySet()) {
            Map<Source, List<QuestionEntity>> bySource = new HashMap<>();
            for (Source src : Source.values()) {
                List<QuestionEntity> pool = new ArrayList<>(
                        questionRepository.findMiniPoolBySubjectAndSource(
                                subject.getId(), src.kind, src.visibility, difficulty));
                Collections.shuffle(pool, random);
                bySource.put(src, pool);
            }
            poolMap.put(subject.getId(), bySource);
        }

        // 가능 회차 수 = min_{(subject, src 중 quota>0)} (풀 / quota)
        int possibleRounds = Integer.MAX_VALUE;
        for (Map.Entry<SubjectEntity, Integer> e : quotas.entrySet()) {
            int[] perSource = splitQuotaToSources(e.getValue());
            Map<Source, List<QuestionEntity>> bySource = poolMap.get(e.getKey().getId());
            for (Source src : Source.values()) {
                int q = perSource[src.ordinal()];
                if (q == 0) continue;
                int poolSize = bySource.get(src).size();
                int possible = poolSize / q;
                possibleRounds = Math.min(possibleRounds, possible);
            }
        }
        if (possibleRounds == 0 || possibleRounds == Integer.MAX_VALUE) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                    "현재 풀로는 미니 모의고사 1회도 만들 수 없습니다. (examType=" + examType
                            + ", difficulty=" + difficulty + ")");
        }

        int nextSeq = mockExamRepository.findMaxSequenceByExamType(examType).orElse(0) + 1;
        long miniCountSoFar = mockExamRepository.countByExamTypeAndKind(examType, MockExamKind.MINI);

        log.info("미니 모의고사 일괄 생성 시작 — examType={}, difficulty={}, possibleRounds={}, startSeq={}, baseMiniNumber={}",
                examType, difficulty, possibleRounds, nextSeq, miniCountSoFar + 1);

        List<Long> createdIds = new ArrayList<>(possibleRounds);
        List<Long> originIdsToMark = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int round = 0; round < possibleRounds; round++) {
            int miniNumber = (int) miniCountSoFar + round + 1;
            String name = autoMiniName(examType, miniNumber);

            MockExamEntity mini = new MockExamEntity(name, examType, nextSeq + round,
                    EngineerExamTemplate.LATEST);
            mockExamRepository.save(mini);
            mini.markAsMini();
            mini.changeVisibility(MockExamVisibility.PREMIUM);
            // 원본 풀이 이미 전문가 검수된 문제이므로 미니도 자동 검수 처리 — 토글이지만 초기값 false → true
            mini.toggleExpertVerified();

            int displayOrder = 1;
            for (Map.Entry<SubjectEntity, Integer> e : quotas.entrySet()) {
                int[] perSource = splitQuotaToSources(e.getValue());
                Map<Source, List<QuestionEntity>> bySource = poolMap.get(e.getKey().getId());
                for (Source src : Source.values()) {
                    int q = perSource[src.ordinal()];
                    List<QuestionEntity> pool = bySource.get(src);
                    for (int j = 0; j < q; j++) {
                        QuestionEntity origin = pool.remove(pool.size() - 1);
                        QuestionEntity copy = new QuestionEntity(origin);
                        questionRepository.save(copy);
                        mini.linkQuestion(copy, displayOrder++);
                        originIdsToMark.add(origin.getId());
                    }
                }
            }

            createdIds.add(mini.getId());
        }

        if (!originIdsToMark.isEmpty()) {
            questionRepository.markIncludedInMiniInBatch(originIdsToMark, now);
        }

        // 잔여 풀 통계 — 출처별 합 (회차 생성 후 남은 풀 크기)
        Map<Source, Long> remaining = new LinkedHashMap<>();
        for (Source src : Source.values()) {
            long total = 0;
            for (Map<Source, List<QuestionEntity>> bySource : poolMap.values()) {
                total += bySource.get(src).size();
            }
            remaining.put(src, total);
        }

        log.info("미니 모의고사 일괄 생성 완료 — examType={}, difficulty={}, created={}, remainingPool={}",
                examType, difficulty, createdIds.size(), remaining);

        return new GenerationResult(examType, createdIds, createdIds.size(), remaining, difficulty);
    }

    /**
     * 한 과목의 미니 1회 quota 를 PAST_EXAM, AI_PUBLISHED, AI_PREMIUM 으로 분배.
     * quota = 3k + r 일 때 잔여 r 은 PAST_EXAM → AI_PUBLISHED 순서로 +1.
     */
    static int[] splitQuotaToSources(int quota) {
        int base = quota / 3;
        int rem = quota % 3;
        return new int[]{
                base + (rem > 0 ? 1 : 0),
                base + (rem > 1 ? 1 : 0),
                base
        };
    }

    /**
     * examType 별 미니 1회 과목 분포 (원본 시험 × 0.4 기준).
     * SubjectEntity 는 runtime 에 SubjectRepository 로 resolve.
     */
    private LinkedHashMap<SubjectEntity, Integer> resolveSubjectQuotas(ExamType examType) {
        return switch (examType) {
            case SQLD -> {
                // SQLD 는 root subject 2개 (1과목 / 2과목) 아래에 leaf 5개. V2 seed 기준.
                SubjectEntity p1 = rootSubject("1과목: 데이터 모델링의 이해");
                SubjectEntity p2 = rootSubject("2과목: SQL 기본 및 활용");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(p1, "데이터 모델링의 이해"), 2);
                q.put(leafOf(p1, "데이터 모델과 SQL"), 2);
                q.put(leafOf(p2, "SQL 기본"), 8);
                q.put(leafOf(p2, "SQL 활용"), 6);
                q.put(leafOf(p2, "관리 구문"), 2);
                yield q;
            }
            case ADSP -> {
                SubjectEntity root = rootSubject("데이터분석 준전문가(ADsP)");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "데이터 이해"), 4);
                q.put(leafOf(root, "데이터 분석 기획"), 4);
                q.put(leafOf(root, "데이터 분석"), 12);
                yield q;
            }
            case COMPUTER_LITERACY_1 -> {
                SubjectEntity root = rootSubject("컴퓨터활용능력 1급 필기");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "컴퓨터 일반"), 8);
                q.put(leafOf(root, "스프레드시트 일반"), 8);
                q.put(leafOf(root, "데이터베이스 일반"), 8);
                yield q;
            }
            case COMPUTER_LITERACY_2 -> {
                SubjectEntity root = rootSubject("컴퓨터활용능력 2급 필기");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "컴퓨터 일반"), 8);
                q.put(leafOf(root, "스프레드시트 일반"), 8);
                yield q;
            }
            case ENGINEER_WRITTEN -> {
                // 정처기 필기: 5 과목 × 8문 = 40문. SubGroup 은 미니에서 보존하지 않고
                // 풀에서 자연스럽게 섞이도록 둠.
                SubjectEntity root = rootSubject("정보처리기사 필기");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "소프트웨어 설계"), 8);
                q.put(leafOf(root, "소프트웨어 개발"), 8);
                q.put(leafOf(root, "데이터베이스 구축"), 8);
                q.put(leafOf(root, "프로그래밍 언어 활용"), 8);
                q.put(leafOf(root, "정보시스템 구축 관리"), 8);
                yield q;
            }
            case ENGINEER_PRACTICAL -> {
                // 정처기 실기: 코드 카테고리 가중. 가중 floor 분배 후 잔여를 코드 카테고리에 추가
                // → 사용자 결정안. 총 8문 = C 3 / Java 1 / Python 1 / SQL 1 / 설계 1 / 네트워크OS 1.
                SubjectEntity root = rootSubject("정보처리기사 실기");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "C언어"), 3);
                q.put(leafOf(root, "Java"), 1);
                q.put(leafOf(root, "Python"), 1);
                q.put(leafOf(root, "SQL"), 1);
                q.put(leafOf(root, "소프트웨어 설계"), 1);
                q.put(leafOf(root, "네트워크/OS"), 1);
                yield q;
            }
        };
    }

    private SubjectEntity rootSubject(String name) {
        return subjectRepository.findByNameAndParentIsNull(name)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + name + "' 루트 과목을 찾을 수 없습니다."));
    }

    private SubjectEntity leafOf(SubjectEntity root, String leafName) {
        return subjectRepository.findByNameAndParentId(leafName, root.getId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + leafName + "' (parent='" + root.getName() + "') 과목을 찾을 수 없습니다."));
    }

    private static String autoMiniName(ExamType type, int miniNumber) {
        String label = switch (type) {
            case SQLD -> "SQLD";
            case ENGINEER_PRACTICAL -> "정처기 실기";
            case ENGINEER_WRITTEN -> "정처기 필기";
            case COMPUTER_LITERACY_1 -> "컴활 1급";
            case COMPUTER_LITERACY_2 -> "컴활 2급";
            case ADSP -> "ADsP";
        };
        return String.format("%s 미니 모의고사 %d회", label, miniNumber);
    }
}
