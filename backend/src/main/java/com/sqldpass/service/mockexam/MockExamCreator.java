package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
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
import com.sqldpass.service.generation.QuestionContentHasher;
import com.sqldpass.service.generation.TopicExamples;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

/**
 * SQLD 모의고사 생성기 — 정처기/컴활과 동일 패턴(시드 + AI 변형 + 사용자 지정 난이도).
 *
 * 시드 소스: TopicExamples (HTML 기출 기반 토픽별 [기본/심화/고난도] JSON 시드).
 *
 * 표준 SQLD 시험 50문항 분포 (실제 출제 비중 반영):
 * - 1과목: 데이터 모델링의 이해 (총 10문항)
 *   · 데이터 모델링의 이해 (id=3): 5문항
 *   · 데이터 모델과 SQL    (id=4): 5문항
 * - 2과목: SQL 기본 및 활용 (총 40문항)
 *   · SQL 기본 (id=5): 20문항
 *   · SQL 활용 (id=6): 15문항
 *   · 관리 구문 (id=7): 5문항  ← 실제 SQLD에서 관리 구문은 5~6문항 비중
 *
 * 흐름: 카테고리 → TopicExamples.randomFor(needed) → AI 변형 → 사용자 지정 난이도로 강제 저장 → 편성.
 */
@Slf4j
@Component
public class MockExamCreator {

    /** 카테고리(과목) ID → needed 문항 수. 실제 SQLD 출제 비중 반영. */
    private static final Map<Long, Integer> DISTRIBUTION;
    /** 카테고리 ID → 표시 이름 (TopicExamples.SQLD_SUBJECT_TOPICS 키와 일치) */
    private static final Map<Long, String> CATEGORY_NAMES;
    static {
        LinkedHashMap<Long, Integer> dist = new LinkedHashMap<>();
        dist.put(3L, 5);   // 1과목: 데이터 모델링의 이해 (시드 15)
        dist.put(4L, 5);   // 1과목: 데이터 모델과 SQL    (시드 6)
        dist.put(5L, 20);  // 2과목: SQL 기본              (시드 27)
        dist.put(6L, 15);  // 2과목: SQL 활용              (시드 24)
        dist.put(7L, 5);   // 2과목: 관리 구문             (시드 6)
        DISTRIBUTION = dist;  // 합 50

        LinkedHashMap<Long, String> names = new LinkedHashMap<>();
        names.put(3L, "데이터 모델링의 이해");
        names.put(4L, "데이터 모델과 SQL");
        names.put(5L, "SQL 기본");
        names.put(6L, "SQL 활용");
        names.put(7L, "관리 구문");
        CATEGORY_NAMES = names;
    }

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final AiProvider sqldAiProvider;
    private final DiscordNotifier discordNotifier;
    private final Random random = new Random();

    public MockExamCreator(MockExamRepository mockExamRepository,
                           QuestionRepository questionRepository,
                           SubjectRepository subjectRepository,
                           @Qualifier("generator") AiProvider sqldAiProvider,
                           DiscordNotifier discordNotifier) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.sqldAiProvider = sqldAiProvider;
        this.discordNotifier = discordNotifier;
    }

    @Transactional
    public MockExamEntity create() {
        return create(MockExamDifficulty.NORMAL);
    }

    @Transactional
    public MockExamEntity create(MockExamDifficulty mockExamDifficulty) {
        MockExamDifficulty difficulty = mockExamDifficulty != null ? mockExamDifficulty : MockExamDifficulty.NORMAL;
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.SQLD).orElse(0) + 1;
        String name = "SQLD 모의고사 " + nextSeq + "회";

        int totalQuestions = DISTRIBUTION.values().stream().mapToInt(Integer::intValue).sum();
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("SQLD 모의고사 생성 시작 - sequence={}, 평균난이도={}, 총문항={}",
                nextSeq, difficulty, totalQuestions);

        List<QuestionEntity> picked = new ArrayList<>();
        Set<String> exhibitedHashes = new HashSet<>();
        int slotCursor = 0;
        for (Map.Entry<Long, Integer> entry : DISTRIBUTION.entrySet()) {
            Long subjectId = entry.getKey();
            int needed = entry.getValue();
            String categoryName = CATEGORY_NAMES.get(subjectId);

            // 1) TopicExamples에서 카테고리 시드 풀(토픽 × 3난이도) → needed개 무작위 추출
            List<String> seedJsons = TopicExamples.randomFor(categoryName, needed, random);
            if (seedJsons.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("SQLD 카테고리 '%s' TopicExamples 시드 풀이 부족합니다 (필요 %d, 보유 %d). " +
                                        "TopicExamples.SQLD_SUBJECT_TOPICS에 토픽을 추가하거나 needed를 줄이세요.",
                                categoryName, needed, seedJsons.size()));
            }

            // 2) 카테고리에 할당된 목표 난이도 슬롯 needed개 슬라이스
            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            // 3) 회피 신호 — 최근 출제 요약
            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subjectId);

            // 4) Subject 조회 (저장용)
            SubjectEntity subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));

            // 5) AI 호출 — 시드 변형 + 목표 난이도 (chunk 분할은 AiProvider 내부에서 자동)
            AiGenerationRequest request = new AiGenerationRequest(
                    categoryName, subjectId, categoryName,
                    recentSummaries, needed, ExamType.SQLD);
            AiGenerationResponse response = sqldAiProvider
                    .generateSqldFromSeeds(request, seedJsons, targetDifficulties, recentSummaries);
            List<GeneratedQuestion> generated = response.questions();
            if (generated == null || generated.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' SQLD AI 생성 실패 (필요 %d, 생성 %d)",
                                categoryName, needed, generated == null ? 0 : generated.size()));
            }

            // 6) 본문 hash 중복 검증 — 회차 내 + DB 기존과 충돌 시 1회 재시도
            if (hasHashConflict(generated, needed, exhibitedHashes)) {
                log.warn("SQLD 카테고리 '{}' 본문 hash 중복 감지 — 1회 재시도", categoryName);
                response = sqldAiProvider
                        .generateSqldFromSeeds(request, seedJsons, targetDifficulties, recentSummaries);
                generated = response.questions();
                if (generated == null || generated.size() < needed) {
                    throw new SqldpassException(
                            ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                            String.format("'%s' SQLD 재시도 실패 (필요 %d, 생성 %d)",
                                    categoryName, needed, generated == null ? 0 : generated.size()));
                }
            }

            // 7) 저장 — 사용자 지정 난이도로 강제 + content_hash 등록
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

        MockExamEntity saved = mockExamRepository.save(new MockExamEntity(name, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("SQLD 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());

        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyMockExamGenerated("SQLD", saved, picked.size(), categoryDist);

        return saved;
    }

    /** 사용자 지정 평균 난이도 → 분포 슬롯 (정처기/컴활과 동일 4단계) */
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
        if (l4 < 0) {
            l3 += l4;
            l4 = 0;
            if (l3 < 0) {
                l2 += l3;
                l3 = 0;
            }
        }
        List<Integer> slots = new ArrayList<>(totalQuestions);
        for (int i = 0; i < l1; i++) slots.add(1);
        for (int i = 0; i < l2; i++) slots.add(2);
        for (int i = 0; i < l3; i++) slots.add(3);
        for (int i = 0; i < l4; i++) slots.add(4);
        Collections.shuffle(slots, random);
        return slots;
    }

    /** 회차 내 hash 또는 DB 기존 hash와 충돌하는 본문이 하나라도 있는지 검사 */
    private boolean hasHashConflict(List<GeneratedQuestion> generated, int needed, Set<String> exhibited) {
        for (int i = 0; i < needed && i < generated.size(); i++) {
            String h = QuestionContentHasher.hashOf(generated.get(i).content());
            if (exhibited.contains(h) || questionRepository.existsByContentHash(h)) {
                return true;
            }
        }
        return false;
    }

    /** AI 응답을 4지선다 객관식 QuestionEntity로 변환 (사용자 지정 난이도 사용) */
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
