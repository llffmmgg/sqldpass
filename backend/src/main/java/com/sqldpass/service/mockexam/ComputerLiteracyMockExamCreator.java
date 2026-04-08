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
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.ComputerLiteracyTopicExamples;
import com.sqldpass.service.generation.ComputerLiteracyTopicExamples.CL1Example;
import com.sqldpass.service.generation.QuestionContentHasher;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

/**
 * 컴퓨터활용능력 1급 필기 모의고사 즉석 생성기.
 *
 * - 카테고리 3개(컴퓨터 일반 / 스프레드시트 일반 / 데이터베이스 일반)
 * - 각 카테고리당 5문항 = 총 15문항 mini 모의고사 (시드 풀 확장 후 60문항으로 확장 예정)
 * - 사용자 지정 평균 난이도(EASY/NORMAL/HARD/VERY_HARD)에 따라 분포 슬롯 결정
 * - 각 시드를 AI에게 변형 1개씩 생성 요청 → DB 저장 → 모의고사 편성
 *
 * 정처기(EngineerMockExamCreator)와 동일 패턴, 객관식(MCQ) 버전.
 */
@Slf4j
@Component
public class ComputerLiteracyMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "컴퓨터활용능력 1급 필기";
    private static final int RECENT_LOOKBACK = 30;

    // 카테고리 이름 상수 (ComputerLiteracyTopicExamples 키와 일치)
    private static final String COMPUTER = "컴퓨터 일반";
    private static final String SPREADSHEET = "스프레드시트 일반";
    private static final String DATABASE = "데이터베이스 일반";

    /**
     * 카테고리별 needed 문항 수 — 컴활 1급 필기 표준 60문항(과목당 20).
     * AiProvider의 chunk 분할 로직(MAX_QUESTIONS_PER_CALL=8)이 큰 needed를 자동 분할.
     */
    private static final Map<String, Integer> DISTRIBUTION;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put(COMPUTER, 20);
        m.put(SPREADSHEET, 20);
        m.put(DATABASE, 20);
        DISTRIBUTION = m;
    }

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final AiProvider computerLiteracyAiProvider;
    private final DiscordNotifier discordNotifier;
    private final Random random = new Random();

    public ComputerLiteracyMockExamCreator(MockExamRepository mockExamRepository,
                                           QuestionRepository questionRepository,
                                           SubjectRepository subjectRepository,
                                           @Qualifier("generator") AiProvider computerLiteracyAiProvider,
                                           DiscordNotifier discordNotifier) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.computerLiteracyAiProvider = computerLiteracyAiProvider;
        this.discordNotifier = discordNotifier;
    }

    @Transactional
    public MockExamEntity create() {
        return create(MockExamDifficulty.NORMAL);
    }

    @Transactional
    public MockExamEntity create(MockExamDifficulty mockExamDifficulty) {
        MockExamDifficulty difficulty = mockExamDifficulty != null ? mockExamDifficulty : MockExamDifficulty.NORMAL;
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.COMPUTER_LITERACY_1).orElse(0) + 1;
        String name = "컴활 1급 필기 모의고사 " + nextSeq + "회";

        int totalQuestions = DISTRIBUTION.values().stream().mapToInt(Integer::intValue).sum();
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("컴활 모의고사 생성 시작 - sequence={}, 분포={}, 평균난이도={}, 슬롯={}",
                nextSeq, DISTRIBUTION, difficulty, difficultySlots);

        SubjectEntity root = subjectRepository.findByNameAndParentIsNull(ROOT_SUBJECT_NAME)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + ROOT_SUBJECT_NAME + "' 루트 과목을 찾을 수 없습니다. V21 마이그레이션 미적용?"));

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

            // 1) 시드 풀에서 needed개 무작위 추출
            List<CL1Example> seeds = ComputerLiteracyTopicExamples.randomFor(category, needed, random);
            if (seeds.isEmpty() || seeds.size() < needed) {
                throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        "카테고리 '" + category + "' 시드 풀이 부족합니다 (필요 " + needed + ", 보유 " + seeds.size() + ")");
            }

            // 2) 카테고리에 할당된 목표 난이도 슬롯 needed개 슬라이스
            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            // 3) 회피 신호 — 최근 출제 정답/요약
            Pageable lookback = PageRequest.of(0, RECENT_LOOKBACK);
            List<String> recentAnswers = questionRepository
                    .findRecentAnswersBySubjectId(subject.getId(), lookback);
            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

            // 4) AI 호출
            AiGenerationRequest request = new AiGenerationRequest(
                    category, subject.getId(), seeds.get(0).topic(),
                    recentSummaries, needed, ExamType.COMPUTER_LITERACY_1);
            AiGenerationResponse response = computerLiteracyAiProvider
                    .generateComputerLiteracyQuestions(request, seeds, targetDifficulties, recentSummaries, recentAnswers);
            List<GeneratedQuestion> generated = response.questions();
            if (generated == null || generated.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' 컴활 AI 생성 실패 (필요 %d, 생성 %d)",
                                category, needed, generated == null ? 0 : generated.size()));
            }

            // 5) 본문 hash 중복 검증 — 회차 내 + DB 기존과 충돌 시 1회 재시도
            if (hasHashConflict(generated, needed, exhibitedHashes)) {
                log.warn("컴활 카테고리 '{}' 본문 hash 중복 감지 — 1회 재시도", category);
                response = computerLiteracyAiProvider
                        .generateComputerLiteracyQuestions(request, seeds, targetDifficulties, recentSummaries, recentAnswers);
                generated = response.questions();
                if (generated == null || generated.size() < needed) {
                    throw new SqldpassException(
                            ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                            String.format("'%s' 컴활 재시도 실패 (필요 %d, 생성 %d)",
                                    category, needed, generated == null ? 0 : generated.size()));
                }
            }

            // 6) 저장 + 사용자 지정 난이도로 difficulty 강제 + content_hash 등록
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
                new MockExamEntity(name, ExamType.COMPUTER_LITERACY_1, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("컴활 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());

        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyMockExamGenerated("컴퓨터활용능력 1급 필기", saved, picked.size(), categoryDist);

        return saved;
    }

    /** 사용자 지정 평균 난이도 → 분포 슬롯 (정처기 4단계 동일) */
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
                null,            // topic — 컴활은 카테고리 단위라 별도 topic 미사용
                targetDifficulty
        );
    }
}
