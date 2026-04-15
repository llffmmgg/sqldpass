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
import com.sqldpass.service.generation.AdspTopicExamples;
import com.sqldpass.service.generation.AdspTopicExamples.AdspExample;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.QuestionContentHasher;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터분석 준전문가(ADsP) 모의고사 즉석 생성기.
 *
 * - 카테고리 3개: 데이터 이해(10) / 데이터 분석 기획(10) / 데이터 분석(30) = 총 50문항
 * - 2024 제40회부터 단답형 폐지, 전 문항 4지선다 객관식
 * - 토픽별 가중 분포(출제 경향 기반)
 */
@Slf4j
@Component
public class AdspMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "데이터분석 준전문가(ADsP)";
    private static final int RECENT_LOOKBACK = 30;

    private static final String DATA_UNDERSTANDING = "데이터 이해";
    private static final String ANALYSIS_PLANNING = "데이터 분석 기획";
    private static final String DATA_ANALYSIS = "데이터 분석";

    private static final Map<String, Integer> DISTRIBUTION;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put(DATA_UNDERSTANDING, 10);
        m.put(ANALYSIS_PLANNING, 10);
        m.put(DATA_ANALYSIS, 30);
        DISTRIBUTION = m;
    }

    /** 데이터 이해 — 토픽별 출제 문항 수 (합계 10) */
    private static final Map<String, Integer> DATA_UNDERSTANDING_QUOTA;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put("DIKW 피라미드", 1);
        m.put("DB 정의와 특징", 1);
        m.put("DB 활용 - OLTP/OLAP/CRM/SCM", 1);
        m.put("빅데이터의 이해", 2);
        m.put("빅데이터 가치와 영향", 2);
        m.put("데이터 사이언티스트 역량", 2);
        m.put("가치 창조 데이터 사이언스", 1);
        DATA_UNDERSTANDING_QUOTA = m;
    }

    /** 데이터 분석 기획 — 토픽별 출제 문항 수 (합계 10) */
    private static final Map<String, Integer> ANALYSIS_PLANNING_QUOTA;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put("분석 방법론 - KDD/CRISP-DM/SEMMA", 2);
        m.put("분석과제 발굴 - Top-Down/Bottom-Up", 2);
        m.put("분석 프로젝트 관리", 1);
        m.put("분석 마스터플랜", 2);
        m.put("분석 거버넌스", 2);
        m.put("데이터 거버넌스·표준화", 1);
        ANALYSIS_PLANNING_QUOTA = m;
    }

    /** 데이터 분석 — 토픽별 출제 문항 수 (합계 30) */
    private static final Map<String, Integer> DATA_ANALYSIS_QUOTA;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put("R 프로그래밍 기초", 4);
        m.put("데이터 전처리", 3);
        m.put("통계 분석 기초", 4);
        m.put("회귀분석", 4);
        m.put("분류분석", 4);
        m.put("앙상블·인공신경망", 3);
        m.put("군집분석", 2);
        m.put("연관분석", 2);
        m.put("시계열 분석", 2);
        m.put("PCA·MDS", 1);
        m.put("모형 평가", 1);
        DATA_ANALYSIS_QUOTA = m;
    }

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final AiProvider aiProvider;
    private final DiscordNotifier discordNotifier;
    private final Random random = new Random();

    public AdspMockExamCreator(MockExamRepository mockExamRepository,
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
    public MockExamEntity create() {
        return create(MockExamDifficulty.NORMAL);
    }

    @Transactional
    public MockExamEntity create(MockExamDifficulty mockExamDifficulty) {
        MockExamDifficulty difficulty = mockExamDifficulty != null ? mockExamDifficulty : MockExamDifficulty.NORMAL;
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.ADSP).orElse(0) + 1;
        String name = "ADsP 모의고사 " + nextSeq + "회";

        int totalQuestions = DISTRIBUTION.values().stream().mapToInt(Integer::intValue).sum();
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("ADsP 모의고사 생성 시작 - sequence={}, 분포={}, 평균난이도={}", nextSeq, DISTRIBUTION, difficulty);

        SubjectEntity root = subjectRepository.findByNameAndParentIsNull(ROOT_SUBJECT_NAME)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + ROOT_SUBJECT_NAME + "' 루트 과목을 찾을 수 없습니다. V38 마이그레이션 미적용?"));

        Map<String, SubjectEntity> categorySubjects = new HashMap<>();
        for (String category : DISTRIBUTION.keySet()) {
            SubjectEntity leaf = subjectRepository.findByNameAndParentId(category, root.getId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                            "카테고리 '" + category + "'를 찾을 수 없습니다."));
            categorySubjects.put(category, leaf);
        }

        List<QuestionEntity> picked = new ArrayList<>();
        Set<String> exhibitedHashes = new HashSet<>();
        int slotCursor = 0;
        for (Map.Entry<String, Integer> entry : DISTRIBUTION.entrySet()) {
            String category = entry.getKey();
            int needed = entry.getValue();
            SubjectEntity subject = categorySubjects.get(category);

            Map<String, Integer> quota = switch (category) {
                case DATA_UNDERSTANDING -> DATA_UNDERSTANDING_QUOTA;
                case ANALYSIS_PLANNING -> ANALYSIS_PLANNING_QUOTA;
                case DATA_ANALYSIS -> DATA_ANALYSIS_QUOTA;
                default -> Map.of();
            };
            List<AdspExample> seeds = AdspTopicExamples.weightedRandomFor(category, quota, needed, random);
            if (seeds.isEmpty() || seeds.size() < needed) {
                throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        "카테고리 '" + category + "' 시드 풀이 부족합니다 (필요 " + needed + ", 보유 " + seeds.size() + ")");
            }

            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            Pageable lookback = PageRequest.of(0, RECENT_LOOKBACK);
            List<String> recentAnswers = questionRepository
                    .findRecentAnswersBySubjectId(subject.getId(), lookback);
            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

            AiGenerationRequest request = new AiGenerationRequest(
                    category, subject.getId(), seeds.get(0).topic(),
                    recentSummaries, needed, ExamType.ADSP);
            AiGenerationResponse response = aiProvider
                    .generateAdspQuestions(request, seeds, targetDifficulties, recentSummaries, recentAnswers);
            List<GeneratedQuestion> generated = response.questions();
            if (generated == null || generated.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' ADsP AI 생성 실패 (필요 %d, 생성 %d)",
                                category, needed, generated == null ? 0 : generated.size()));
            }

            if (hasHashConflict(generated, needed, exhibitedHashes)) {
                log.warn("ADsP 카테고리 '{}' 본문 hash 중복 감지 — 1회 재시도", category);
                response = aiProvider
                        .generateAdspQuestions(request, seeds, targetDifficulties, recentSummaries, recentAnswers);
                generated = response.questions();
                if (generated == null || generated.size() < needed) {
                    throw new SqldpassException(
                            ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                            String.format("'%s' ADsP 재시도 실패 (필요 %d, 생성 %d)",
                                    category, needed, generated == null ? 0 : generated.size()));
                }
            }

            for (int i = 0; i < needed; i++) {
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
                new MockExamEntity(name, ExamType.ADSP, nextSeq, com.sqldpass.persistent.mockexam.EngineerExamTemplate.LATEST));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("ADsP 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());

        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyMockExamGenerated("데이터분석 준전문가(ADsP)", saved, picked.size(), categoryDist);

        return saved;
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
            if (exhibited.contains(h) || questionRepository.existsByContentHash(h)) return true;
        }
        return false;
    }

    private QuestionEntity toQuestionEntity(SubjectEntity subject, GeneratedQuestion gq, int targetDifficulty) {
        int correctOption = gq.correctOption() != null ? gq.correctOption() : 1;
        return new QuestionEntity(subject, gq.content(), correctOption, gq.explanation(),
                gq.summary(), null, targetDifficulty);
    }
}
