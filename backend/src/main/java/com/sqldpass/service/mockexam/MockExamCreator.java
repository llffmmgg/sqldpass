package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

/**
 * SQLD 모의고사 생성기 — 정처기/컴활과 동일 패턴(시드 + AI 변형 + 사용자 지정 난이도).
 *
 * 시드 소스: 운영 DB에 이미 존재하는 SQLD 문제를 매번 카테고리별 needed개 무작위 추출.
 * (정처기/컴활은 코드 시드 풀, SQLD는 운영 누적 데이터를 시드로 활용 — 사용자 결정)
 *
 * 표준 SQLD 시험 50문항 분포:
 * - 1과목: 데이터 모델링의 이해 (총 10문항)
 *   · 데이터 모델링의 이해 (id=3): 5문항
 *   · 데이터 모델과 SQL    (id=4): 5문항
 * - 2과목: SQL 기본 및 활용 (총 40문항)
 *   · SQL 기본 (id=5): 14문항
 *   · SQL 활용 (id=6): 13문항
 *   · 관리 구문 (id=7): 13문항
 *
 * AI 호출당 시드 = 변형 = needed개. 시드별 1개씩 변형 → 사용자 지정 난이도로 강제 → DB 저장 → 모의고사 편성.
 */
@Slf4j
@Component
public class MockExamCreator {

    private static final int RECENT_LOOKBACK = 30;

    /** 카테고리(과목) ID → needed 문항 수. 표준 SQLD 50문항 분포. */
    private static final Map<Long, Integer> DISTRIBUTION;
    /** 카테고리 ID → 표시 이름 (로깅·프롬프트용) */
    private static final Map<Long, String> CATEGORY_NAMES;
    static {
        LinkedHashMap<Long, Integer> dist = new LinkedHashMap<>();
        dist.put(3L, 5);   // 1과목: 데이터 모델링의 이해
        dist.put(4L, 5);   // 1과목: 데이터 모델과 SQL
        dist.put(5L, 14);  // 2과목: SQL 기본
        dist.put(6L, 13);  // 2과목: SQL 활용
        dist.put(7L, 13);  // 2과목: 관리 구문
        DISTRIBUTION = dist;

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
    private final Random random = new Random();

    public MockExamCreator(MockExamRepository mockExamRepository,
                           QuestionRepository questionRepository,
                           SubjectRepository subjectRepository,
                           @Qualifier("generator") AiProvider sqldAiProvider) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.sqldAiProvider = sqldAiProvider;
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
        int slotCursor = 0;
        for (Map.Entry<Long, Integer> entry : DISTRIBUTION.entrySet()) {
            Long subjectId = entry.getKey();
            int needed = entry.getValue();
            String categoryName = CATEGORY_NAMES.get(subjectId);

            // 1) DB에서 시드로 사용할 SQLD 문제 needed개 무작위 추출
            List<QuestionEntity> seeds = questionRepository.findRandomBySubjectId(subjectId, needed);
            if (seeds.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("SQLD 카테고리 '%s'(id=%d) 시드 풀이 부족합니다 (필요 %d, 보유 %d). " +
                                        "어드민에서 AI 문제 생성으로 풀을 더 채워주세요.",
                                categoryName, subjectId, needed, seeds.size()));
            }

            // 2) 카테고리에 할당된 목표 난이도 슬롯 needed개 슬라이스
            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            // 3) 회피 신호 — 최근 출제 요약
            Pageable lookback = PageRequest.of(0, RECENT_LOOKBACK);
            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subjectId);

            // 4) Subject 조회 (저장용)
            SubjectEntity subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));

            // 5) AI 호출 — 시드 변형 + 목표 난이도
            AiGenerationRequest request = new AiGenerationRequest(
                    categoryName, subjectId, categoryName,
                    recentSummaries, needed, ExamType.SQLD);
            AiGenerationResponse response = sqldAiProvider
                    .generateSqldFromSeeds(request, seeds, targetDifficulties, recentSummaries);
            List<GeneratedQuestion> generated = response.questions();
            if (generated == null || generated.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' SQLD AI 생성 실패 (필요 %d, 생성 %d)",
                                categoryName, needed, generated == null ? 0 : generated.size()));
            }

            // 6) 저장 — 사용자 지정 난이도로 강제
            for (int i = 0; i < needed; i++) {
                GeneratedQuestion gq = generated.get(i);
                int targetDifficulty = targetDifficulties.get(i);
                QuestionEntity entity = toQuestionEntity(subject, gq, targetDifficulty);
                picked.add(questionRepository.save(entity));
            }
        }

        MockExamEntity saved = mockExamRepository.save(new MockExamEntity(name, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("SQLD 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());
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
