package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.EngineerWrittenTopicExamples;
import com.sqldpass.service.generation.EngineerWrittenTopicExamples.EWExample;
import com.sqldpass.service.generation.QuestionContentHasher;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

/**
 * 정보처리기사 필기 모의고사 즉석 생성기.
 *
 * 5과목 × 20문항 = 총 100문항.
 * 각 과목 내 소분류별 기출 빈도 가중치를 반영하여 문항 수를 랜덤 배분.
 */
@Slf4j
@Component
public class EngineerWrittenMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "정보처리기사 필기";
    private static final int QUESTIONS_PER_SUBJECT = 20;
    private static final int RECENT_LOOKBACK = 30;

    /**
     * 소분류 정의: (이름, 최소 문항, 최대 문항, 해당 토픽 목록).
     * 각 과목의 소분류별 min~max 합이 20을 포함하도록 설계.
     */
    record SubGroup(String name, int min, int max, List<String> topics) {}

    private static final Map<String, List<SubGroup>> SUBJECT_DISTRIBUTION = new LinkedHashMap<>();

    static {
        SUBJECT_DISTRIBUTION.put("소프트웨어 설계", List.of(
                new SubGroup("요구사항 확인", 5, 7,
                        List.of("UML 다이어그램 종류", "UML 관계", "요구사항 분석", "소프트웨어 생명주기")),
                new SubGroup("화면 설계", 3, 5,
                        List.of("UI 설계", "품질 특성 (ISO 9126 / 25010)")),
                new SubGroup("애플리케이션 설계", 5, 7,
                        List.of("디자인 패턴", "결합도와 응집도", "아키텍처 패턴", "애자일 (스크럼/XP)")),
                new SubGroup("인터페이스 설계", 2, 4,
                        List.of())  // 시드 토픽이 부족 → 다른 소분류에서 보충
        ));

        SUBJECT_DISTRIBUTION.put("소프트웨어 개발", List.of(
                new SubGroup("데이터 입출력 구현", 4, 6,
                        List.of("자료구조 - 스택/큐", "자료구조 - 트리", "자료구조 - 그래프/해싱", "정렬 알고리즘")),
                new SubGroup("통합 구현", 2, 3,
                        List.of("미들웨어")),
                new SubGroup("제품 소프트웨어 패키징", 2, 4,
                        List.of("형상관리/빌드 자동화")),
                new SubGroup("애플리케이션 테스트", 5, 7,
                        List.of("테스트 유형", "화이트박스/블랙박스 테스트", "통합 테스트")),
                new SubGroup("인터페이스 구현", 2, 3,
                        List.of("인터페이스 구현"))
        ));

        SUBJECT_DISTRIBUTION.put("데이터베이스 구축", List.of(
                new SubGroup("SQL 응용", 8, 10,
                        List.of("SQL DDL", "SQL DML / JOIN", "집계함수 / GROUP BY / HAVING",
                                "서브쿼리", "윈도우 함수", "트랜잭션 ACID / DCL / TCL")),
                new SubGroup("논리 데이터베이스 설계", 5, 7,
                        List.of("정규화", "키 종류", "관계대수", "무결성 제약")),
                new SubGroup("물리 데이터베이스 설계", 2, 3,
                        List.of("인덱스 / 반정규화")),
                new SubGroup("데이터 전환/품질", 1, 2,
                        List.of())
        ));

        SUBJECT_DISTRIBUTION.put("프로그래밍 언어 활용", List.of(
                new SubGroup("코드 출력값 추적", 8, 12,
                        List.of("C언어 - 포인터/배열", "C언어 - 비트연산/재귀", "C언어 - 문자열/구조체",
                                "Java - 클래스/상속", "Java - 컬렉션/예외처리",
                                "Python - 리스트/딕셔너리", "Python - 함수/컴프리헨션")),
                new SubGroup("운영체제", 3, 5,
                        List.of("OS - 프로세스/스케줄링", "OS - 교착상태/페이지 교체")),
                new SubGroup("네트워크", 3, 5,
                        List.of("네트워크 - OSI/프로토콜")),
                new SubGroup("리눅스 명령어", 1, 2,
                        List.of("리눅스 명령어"))
        ));

        SUBJECT_DISTRIBUTION.put("정보시스템 구축 관리", List.of(
                new SubGroup("보안", 6, 8,
                        List.of("보안 3요소 (CIA)", "암호화 알고리즘", "접근 통제",
                                "웹 보안 공격", "DoS/DDoS 공격", "네트워크 보안 솔루션")),
                new SubGroup("개발방법론/프로젝트관리", 3, 5,
                        List.of("개발 방법론 / 비용산정", "Secure SDLC / 보안 개발 방법론")),
                new SubGroup("IT인프라", 3, 4,
                        List.of("클라우드 / 스토리지", "네트워크 기술")),
                new SubGroup("신기술 동향", 3, 5,
                        List.of("신기술 동향"))
        ));
    }

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final AiProvider aiProvider;
    private final DiscordNotifier discordNotifier;
    private final Random random = new Random();

    public EngineerWrittenMockExamCreator(MockExamRepository mockExamRepository,
                                          QuestionRepository questionRepository,
                                          SubjectRepository subjectRepository,
                                          @Qualifier("generator") AiProvider aiProvider,
                                          DiscordNotifier discordNotifier) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.aiProvider = aiProvider;
        this.discordNotifier = discordNotifier;
    }

    @Transactional
    public MockExamEntity create(MockExamDifficulty mockExamDifficulty) {
        MockExamDifficulty difficulty = mockExamDifficulty != null ? mockExamDifficulty : MockExamDifficulty.NORMAL;
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.ENGINEER_WRITTEN).orElse(0) + 1;
        String name = "정처기 필기 모의고사 " + nextSeq + "회";

        int totalQuestions = SUBJECT_DISTRIBUTION.size() * QUESTIONS_PER_SUBJECT;
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("정처기 필기 모의고사 생성 시작 - sequence={}, 평균난이도={}", nextSeq, difficulty);

        SubjectEntity root = subjectRepository.findByNameAndParentIsNull(ROOT_SUBJECT_NAME)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + ROOT_SUBJECT_NAME + "' 루트 과목을 찾을 수 없습니다. V33 마이그레이션 미적용?"));

        Map<String, SubjectEntity> subjectEntities = new HashMap<>();
        for (String subjectName : SUBJECT_DISTRIBUTION.keySet()) {
            SubjectEntity leaf = subjectRepository.findByNameAndParentId(subjectName, root.getId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                            "과목 '" + subjectName + "'를 찾을 수 없습니다."));
            subjectEntities.put(subjectName, leaf);
        }

        List<QuestionEntity> picked = new ArrayList<>();
        Set<String> exhibitedHashes = new HashSet<>();
        int slotCursor = 0;

        for (Map.Entry<String, List<SubGroup>> entry : SUBJECT_DISTRIBUTION.entrySet()) {
            String subjectName = entry.getKey();
            List<SubGroup> subGroups = entry.getValue();
            SubjectEntity subject = subjectEntities.get(subjectName);

            // 1) 소분류별 문항 수 배분
            List<Integer> subGroupCounts = distributeSubGroupCounts(subGroups, QUESTIONS_PER_SUBJECT);

            // 2) 각 소분류에서 시드 추출
            List<EWExample> seeds = new ArrayList<>();
            for (int g = 0; g < subGroups.size(); g++) {
                SubGroup sg = subGroups.get(g);
                int needed = subGroupCounts.get(g);
                if (needed == 0) continue;

                List<EWExample> sgSeeds;
                if (sg.topics().isEmpty()) {
                    // 토픽이 정의되지 않은 소분류 → 과목 전체 풀에서 추출
                    sgSeeds = EngineerWrittenTopicExamples.randomFor(subjectName, needed, random);
                } else {
                    // 해당 토픽들의 시드만 필터링 후 추출
                    sgSeeds = pickSeedsFromTopics(subjectName, sg.topics(), needed);
                }
                seeds.addAll(sgSeeds);
            }

            if (seeds.size() < QUESTIONS_PER_SUBJECT) {
                // 부족분은 과목 전체 풀에서 보충
                int deficit = QUESTIONS_PER_SUBJECT - seeds.size();
                List<EWExample> extra = EngineerWrittenTopicExamples.randomFor(subjectName, deficit + seeds.size(), random);
                Set<String> existingTopics = seeds.stream().map(EWExample::topic).collect(Collectors.toSet());
                for (EWExample e : extra) {
                    if (seeds.size() >= QUESTIONS_PER_SUBJECT) break;
                    if (!existingTopics.contains(e.topic())) {
                        seeds.add(e);
                        existingTopics.add(e.topic());
                    }
                }
                // 그래도 부족하면 중복 허용
                while (seeds.size() < QUESTIONS_PER_SUBJECT && !extra.isEmpty()) {
                    seeds.add(extra.get(random.nextInt(extra.size())));
                }
            }

            // 3) 난이도 슬롯
            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + QUESTIONS_PER_SUBJECT));
            slotCursor += QUESTIONS_PER_SUBJECT;

            // 4) 회피 신호
            Pageable lookback = PageRequest.of(0, RECENT_LOOKBACK);
            List<String> recentAnswers = questionRepository.findRecentAnswersBySubjectId(subject.getId(), lookback);
            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

            // 5) AI 호출
            AiGenerationRequest request = new AiGenerationRequest(
                    subjectName, subject.getId(), seeds.get(0).topic(),
                    recentSummaries, QUESTIONS_PER_SUBJECT, ExamType.ENGINEER_WRITTEN);
            AiGenerationResponse response = aiProvider.generateEngineerWrittenQuestions(
                    request, seeds, targetDifficulties, recentSummaries, recentAnswers);
            List<GeneratedQuestion> generated = response.questions();
            if (generated == null || generated.size() < QUESTIONS_PER_SUBJECT) {
                throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' 정처기 필기 AI 생성 실패 (필요 %d, 생성 %d)",
                                subjectName, QUESTIONS_PER_SUBJECT, generated == null ? 0 : generated.size()));
            }

            // 6) hash 중복 검증 + 1회 재시도
            if (hasHashConflict(generated, QUESTIONS_PER_SUBJECT, exhibitedHashes)) {
                log.warn("정처기 필기 과목 '{}' 본문 hash 중복 감지 — 1회 재시도", subjectName);
                response = aiProvider.generateEngineerWrittenQuestions(
                        request, seeds, targetDifficulties, recentSummaries, recentAnswers);
                generated = response.questions();
                if (generated == null || generated.size() < QUESTIONS_PER_SUBJECT) {
                    throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                            String.format("'%s' 정처기 필기 재시도 실패 (필요 %d, 생성 %d)",
                                    subjectName, QUESTIONS_PER_SUBJECT, generated == null ? 0 : generated.size()));
                }
            }

            // 7) 저장
            for (int i = 0; i < QUESTIONS_PER_SUBJECT; i++) {
                GeneratedQuestion gq = generated.get(i);
                int targetDifficulty = targetDifficulties.get(i);
                String hash = QuestionContentHasher.hashOf(gq.content());
                QuestionEntity entity = toQuestionEntity(subject, gq, targetDifficulty);
                entity.assignContentHash(hash);
                picked.add(questionRepository.save(entity));
                exhibitedHashes.add(hash);
            }
        }

        MockExamEntity saved = mockExamRepository.save(
                new MockExamEntity(name, ExamType.ENGINEER_WRITTEN, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("정처기 필기 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());

        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyMockExamGenerated("정보처리기사 필기", saved, picked.size(), categoryDist);

        return saved;
    }

    /**
     * 소분류별 min~max 범위 내에서 랜덤 배분, 합계를 total로 정규화.
     */
    private List<Integer> distributeSubGroupCounts(List<SubGroup> subGroups, int total) {
        List<Integer> counts = new ArrayList<>(subGroups.size());
        for (SubGroup sg : subGroups) {
            counts.add(sg.min() + random.nextInt(sg.max() - sg.min() + 1));
        }
        // 합계 조정
        int sum = counts.stream().mapToInt(Integer::intValue).sum();
        while (sum != total) {
            int diff = total - sum;
            int idx = random.nextInt(counts.size());
            SubGroup sg = subGroups.get(idx);
            int current = counts.get(idx);
            if (diff > 0 && current < sg.max()) {
                counts.set(idx, current + 1);
                sum++;
            } else if (diff < 0 && current > sg.min()) {
                counts.set(idx, current - 1);
                sum--;
            }
        }
        return counts;
    }

    /**
     * 특정 토픽 목록에서 시드를 추출. 풀이 부족하면 가용한 만큼만.
     */
    private List<EWExample> pickSeedsFromTopics(String category, List<String> topics, int needed) {
        List<EWExample> pool = EngineerWrittenTopicExamples.EXAMPLES_BY_CATEGORY.get(category);
        if (pool == null || pool.isEmpty()) return List.of();

        List<EWExample> filtered = pool.stream()
                .filter(e -> topics.contains(e.topic()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(filtered, random);

        if (needed >= filtered.size()) {
            return new ArrayList<>(filtered);
        }
        return new ArrayList<>(filtered.subList(0, needed));
    }

    private List<Integer> buildDifficultySlots(MockExamDifficulty difficulty, int totalQuestions) {
        int[] dist = switch (difficulty) {
            case EASY -> new int[]{60, 30, 10, 0};
            case NORMAL -> new int[]{20, 50, 25, 5};
            case HARD -> new int[]{5, 25, 50, 20};
            case VERY_HARD -> new int[]{0, 10, 30, 60};
        };
        int l1 = Math.round(totalQuestions * dist[0] / 100f);
        int l2 = Math.round(totalQuestions * dist[1] / 100f);
        int l3 = Math.round(totalQuestions * dist[2] / 100f);
        int l4 = totalQuestions - l1 - l2 - l3;
        if (l4 < 0) { l3 += l4; l4 = 0; if (l3 < 0) { l2 += l3; l3 = 0; } }
        List<Integer> slots = new ArrayList<>(totalQuestions);
        for (int i = 0; i < l1; i++) slots.add(1);
        for (int i = 0; i < l2; i++) slots.add(2);
        for (int i = 0; i < l3; i++) slots.add(3);
        for (int i = 0; i < l4; i++) slots.add(4);
        Collections.shuffle(slots, random);
        return slots;
    }

    private boolean hasHashConflict(List<GeneratedQuestion> generated, int needed, Set<String> exhibited) {
        for (int i = 0; i < needed && i < generated.size(); i++) {
            String h = QuestionContentHasher.hashOf(generated.get(i).content());
            if (exhibited.contains(h) || questionRepository.existsByContentHash(h)) {
                return true;
            }
        }
        return false;
    }

    private QuestionEntity toQuestionEntity(SubjectEntity subject, GeneratedQuestion gq, int targetDifficulty) {
        int correctOption = gq.correctOption() != null ? gq.correctOption() : 1;
        return new QuestionEntity(
                subject,
                gq.content(),
                correctOption,
                gq.explanation(),
                gq.summary(),
                null,
                targetDifficulty
        );
    }
}
