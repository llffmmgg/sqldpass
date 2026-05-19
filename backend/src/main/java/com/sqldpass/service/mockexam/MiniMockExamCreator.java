package com.sqldpass.service.mockexam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
 * <strong>과목 분포만</strong> 보존하며 가능한 한 많은 회차를 일괄 생성한다.
 *
 * <p>풀 조건 (모든 안전망):
 * <ul>
 *   <li>{@code mock_exam.visibility <> DRAFT} — 사용자에게 한 번도 노출된 적 없는 회차의 문제는
 *       풀에 들어가지 못함</li>
 *   <li>{@code mock_exam.expert_verified = true}</li>
 *   <li>{@code question.included_in_mini_at IS NULL} — 한 번 미니로 복제된 문제는 다시 사용 X</li>
 * </ul>
 *
 * <p>비율 정책: 각 시험 종목의 원본 분포 ×0.4 (정처기 실기는 ×0.5 비례 축소 10문 슬롯).
 * 출처(기출/AI 무료/AI 프리미엄) 와 난이도 구분은 의도적으로 폐기 — 풀 활용을 극대화하고
 * (과목 × 출처) 매트릭스 병목(예: 정처기 필기에 AI_PREMIUM 회차가 없어 0회 생성되던 문제)을 제거한다.
 * 출처 비율은 풀의 자연 분포를 그대로 따른다.
 *
 * <p>회차 수 결정: 모든 과목 중 (풀 / quota) 의 최솟값. 잔여 풀은 그대로 유지돼 다음 풀 보충 시
 * 추가 회차로 만들어진다.
 *
 * <p>각 회차는 새 {@link MockExamEntity} (visibility=PREMIUM, kind=MINI, expertVerified=true) 로
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

    /** 어드민 일괄 생성 결과 — 회차 ID 리스트 + 과목별 잔여 풀. */
    public record GenerationResult(
            ExamType examType,
            List<Long> createdMockExamIds,
            int createdCount,
            /** 과목 ID → 잔여 풀 크기 (회차 생성 후 남은 문제 수). 다음 풀 보충 시 추가 회차 가능 여부 가늠. */
            Map<Long, Long> remainingBySubject
    ) {}

    /**
     * 미니 모의고사 일괄 생성 — 과목 분포 보존하며 만들 수 있는 모든 회차 생성.
     * 풀이 부족해 0회차가 만들어지면 INSUFFICIENT_QUESTIONS 던짐.
     */
    @Transactional
    public GenerationResult createAllFromPool(ExamType examType) {
        if (examType == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "examType 은 필수입니다.");
        }

        LinkedHashMap<SubjectEntity, Integer> quotas = resolveSubjectQuotas(examType);
        if (quotas.isEmpty()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "미니 모의고사 분포가 정의되지 않은 examType: " + examType);
        }

        // 과목 → 셔플된 mutable 풀
        Map<Long, List<QuestionEntity>> poolBySubject = new LinkedHashMap<>();
        for (SubjectEntity subject : quotas.keySet()) {
            List<QuestionEntity> pool = new ArrayList<>(
                    questionRepository.findMiniPoolBySubject(subject.getId()));
            Collections.shuffle(pool, random);
            poolBySubject.put(subject.getId(), pool);
        }

        // 가능 회차 수 = min_{과목} (풀 / quota)
        int possibleRounds = Integer.MAX_VALUE;
        for (Map.Entry<SubjectEntity, Integer> e : quotas.entrySet()) {
            int quota = e.getValue();
            if (quota == 0) continue;
            int poolSize = poolBySubject.get(e.getKey().getId()).size();
            possibleRounds = Math.min(possibleRounds, poolSize / quota);
        }
        if (possibleRounds == 0 || possibleRounds == Integer.MAX_VALUE) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                    "현재 풀로는 미니 모의고사 1회도 만들 수 없습니다. (examType=" + examType + ")");
        }

        int nextSeq = mockExamRepository.findMaxSequenceByExamType(examType).orElse(0) + 1;
        long miniCountSoFar = mockExamRepository.countByExamTypeAndKind(examType, MockExamKind.MINI);

        log.info("미니 모의고사 일괄 생성 시작 — examType={}, possibleRounds={}, startSeq={}, baseMiniNumber={}",
                examType, possibleRounds, nextSeq, miniCountSoFar + 1);

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
            mini.toggleExpertVerified();

            int displayOrder = 1;
            for (Map.Entry<SubjectEntity, Integer> e : quotas.entrySet()) {
                int quota = e.getValue();
                List<QuestionEntity> pool = poolBySubject.get(e.getKey().getId());
                for (int j = 0; j < quota; j++) {
                    QuestionEntity origin = pool.remove(pool.size() - 1);
                    QuestionEntity copy = new QuestionEntity(origin);
                    questionRepository.save(copy);
                    mini.linkQuestion(copy, displayOrder++);
                    originIdsToMark.add(origin.getId());
                }
            }

            createdIds.add(mini.getId());
        }

        if (!originIdsToMark.isEmpty()) {
            questionRepository.markIncludedInMiniInBatch(originIdsToMark, now);
        }

        // 과목별 잔여 풀
        Map<Long, Long> remaining = new LinkedHashMap<>();
        poolBySubject.forEach((subjectId, pool) -> remaining.put(subjectId, (long) pool.size()));

        log.info("미니 모의고사 일괄 생성 완료 — examType={}, created={}, remainingPool={}",
                examType, createdIds.size(), remaining);

        return new GenerationResult(examType, createdIds, createdIds.size(), remaining);
    }

    /**
     * examType 별 미니 1회 과목 분포 (원본 시험 × 0.4 기준).
     * SubjectEntity 는 runtime 에 SubjectRepository 로 resolve.
     */
    private LinkedHashMap<SubjectEntity, Integer> resolveSubjectQuotas(ExamType examType) {
        return switch (examType) {
            case SQLD -> {
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
                // 정처기 실기: 정규 20문 분포의 ×0.5 비례 축소. 총 10문.
                // 코드 해석 5문 (C 2 + Java 1 + Python 1 + SQL 1) +
                // 이론 5문 (설계 1 + DB 1 + NetOS 1 + 보안 1 + 신기술 1) — 실기 단원 빠짐없이 커버.
                SubjectEntity root = rootSubject("정보처리기사 실기");
                LinkedHashMap<SubjectEntity, Integer> q = new LinkedHashMap<>();
                q.put(leafOf(root, "C언어"), 2);
                q.put(leafOf(root, "Java"), 1);
                q.put(leafOf(root, "Python"), 1);
                q.put(leafOf(root, "SQL"), 1);
                q.put(leafOf(root, "소프트웨어 설계"), 1);
                q.put(leafOf(root, "데이터베이스 이론"), 1);
                q.put(leafOf(root, "네트워크/OS"), 1);
                q.put(leafOf(root, "보안"), 1);
                q.put(leafOf(root, "신기술 동향"), 1);
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
