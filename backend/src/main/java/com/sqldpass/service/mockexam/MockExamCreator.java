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
import com.sqldpass.service.generation.SqldMcqGenerationValidator;
import com.sqldpass.service.generation.TopicExamples;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;
import com.sqldpass.service.notification.DiscordNotifier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockExamCreator {

    private static final int MAX_REGENERATION = 2;

    private static final Map<Long, Integer> DISTRIBUTION;
    private static final Map<Long, String> CATEGORY_NAMES;

    static {
        LinkedHashMap<Long, Integer> dist = new LinkedHashMap<>();
        dist.put(3L, 5);
        dist.put(4L, 5);
        dist.put(5L, 20);
        dist.put(6L, 15);
        dist.put(7L, 5);
        DISTRIBUTION = dist;

        LinkedHashMap<Long, String> names = new LinkedHashMap<>();
        names.put(3L, "\uB370\uC774\uD130 \uBAA8\uB378\uB9C1\uC758 \uC774\uD574");
        names.put(4L, "\uB370\uC774\uD130 \uBAA8\uB378\uACFC SQL");
        names.put(5L, "SQL \uAE30\uBCF8");
        names.put(6L, "SQL \uD65C\uC6A9");
        names.put(7L, "\uAD00\uB9AC \uAD6C\uBB38");
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
        String name = "SQLD \uBAA8\uC758\uACE0\uC0AC " + nextSeq + "\uD68C";

        int totalQuestions = DISTRIBUTION.values().stream().mapToInt(Integer::intValue).sum();
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("SQLD mock exam generation started - sequence={}, difficulty={}, totalQuestions={}",
                nextSeq, difficulty, totalQuestions);

        List<QuestionEntity> picked = new ArrayList<>();
        Set<String> exhibitedHashes = new HashSet<>();
        int slotCursor = 0;

        for (Map.Entry<Long, Integer> entry : DISTRIBUTION.entrySet()) {
            Long subjectId = entry.getKey();
            int needed = entry.getValue();
            String categoryName = CATEGORY_NAMES.get(subjectId);

            List<String> seedJsons = TopicExamples.randomFor(categoryName, needed, random);
            if (seedJsons.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("SQLD category '%s' does not have enough seeds (needed=%d, available=%d)",
                                categoryName, needed, seedJsons.size()));
            }

            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            List<String> recentSummaries = questionRepository.findSummariesBySubjectId(subjectId);
            Set<String> knownSummaryKeys = recentSummaries.stream()
                    .map(SqldMcqGenerationValidator::normalizeSummary)
                    .filter(key -> key != null && !key.isBlank())
                    .collect(Collectors.toCollection(HashSet::new));

            SubjectEntity subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));

            AiGenerationRequest request = new AiGenerationRequest(
                    categoryName, subjectId, categoryName,
                    recentSummaries, needed, ExamType.SQLD);

            List<GeneratedQuestion> generated = validateAndRetry(
                    request, seedJsons, targetDifficulties, recentSummaries,
                    knownSummaryKeys, exhibitedHashes, needed, categoryName);

            for (int i = 0; i < needed; i++) {
                GeneratedQuestion question = generated.get(i);
                int targetDifficulty = targetDifficulties.get(i);
                String hash = QuestionContentHasher.hashOf(question.content());

                QuestionEntity entity = toQuestionEntity(subject, question, targetDifficulty);
                entity.assignContentHash(hash);
                picked.add(questionRepository.save(entity));
                exhibitedHashes.add(hash);

                String summaryKey = SqldMcqGenerationValidator.normalizeSummary(question.summary());
                if (summaryKey != null) {
                    knownSummaryKeys.add(summaryKey);
                }
            }
        }

        MockExamEntity saved = mockExamRepository.save(new MockExamEntity(name, com.sqldpass.persistent.mockexam.ExamType.SQLD, nextSeq, com.sqldpass.persistent.mockexam.EngineerExamTemplate.LATEST));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }

        log.info("SQLD mock exam generation completed - id={}, questionCount={}", saved.getId(), picked.size());

        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyMockExamGenerated("SQLD", saved, picked.size(), categoryDist);

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

    private List<GeneratedQuestion> validateAndRetry(AiGenerationRequest request,
                                                     List<String> seedJsons,
                                                     List<Integer> targetDifficulties,
                                                     List<String> recentSummaries,
                                                     Set<String> knownSummaryKeys,
                                                     Set<String> exhibitedHashes,
                                                     int needed,
                                                     String categoryName) {
        List<String> summaryHints = new ArrayList<>(recentSummaries);
        List<GeneratedQuestion> current = callAi(request, seedJsons, targetDifficulties, summaryHints, needed);

        for (int attempt = 0; attempt <= MAX_REGENERATION; attempt++) {
            List<String> issues = findIssues(current, needed, knownSummaryKeys, exhibitedHashes);
            if (issues.isEmpty()) {
                return current;
            }

            log.warn("SQLD category '{}' generated invalid payload (attempt={}): {}",
                    categoryName, attempt, issues);

            if (attempt == MAX_REGENERATION) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' SQLD generation payload is invalid: %s",
                                categoryName, String.join(" | ", issues)));
            }

            current.stream()
                    .map(GeneratedQuestion::summary)
                    .filter(summary -> summary != null && !summary.isBlank())
                    .forEach(summaryHints::add);
            current = callAi(request, seedJsons, targetDifficulties, summaryHints, needed);
        }

        throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                "'" + categoryName + "' SQLD regeneration loop ended unexpectedly");
    }

    private List<GeneratedQuestion> callAi(AiGenerationRequest request,
                                           List<String> seedJsons,
                                           List<Integer> targetDifficulties,
                                           List<String> recentSummaries,
                                           int needed) {
        AiGenerationResponse response = sqldAiProvider
                .generateSqldFromSeeds(request, seedJsons, targetDifficulties, recentSummaries);
        List<GeneratedQuestion> generated = response.questions();
        if (generated == null || generated.size() < needed) {
            throw new SqldpassException(
                    ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                    String.format("'%s' SQLD AI generation failed (needed=%d, generated=%d)",
                            request.subjectName(), needed, generated == null ? 0 : generated.size()));
        }
        return generated;
    }

    private List<String> findIssues(List<GeneratedQuestion> generated,
                                    int needed,
                                    Set<String> knownSummaryKeys,
                                    Set<String> exhibitedHashes) {
        List<String> issues = new ArrayList<>();
        Set<String> batchSummaryKeys = new HashSet<>(knownSummaryKeys);
        Set<String> batchHashes = new HashSet<>(exhibitedHashes);

        for (int i = 0; i < needed && i < generated.size(); i++) {
            GeneratedQuestion question = generated.get(i);
            List<String> basicIssues = SqldMcqGenerationValidator.basicIssues(question);
            if (!basicIssues.isEmpty()) {
                issues.add("Question " + (i + 1) + ": " + String.join(", ", basicIssues));
                continue;
            }

            String summaryKey = SqldMcqGenerationValidator.normalizeSummary(question.summary());
            if (summaryKey != null && !batchSummaryKeys.add(summaryKey)) {
                issues.add("Question " + (i + 1) + ": summary duplicates an existing question");
            }

            String hash = QuestionContentHasher.hashOf(question.content());
            if (!batchHashes.add(hash) || questionRepository.existsByContentHash(hash)) {
                issues.add("Question " + (i + 1) + ": content duplicates an existing question");
            }
        }

        return issues;
    }

    private QuestionEntity toQuestionEntity(SubjectEntity subject,
                                            GeneratedQuestion question,
                                            int targetDifficulty) {
        return new QuestionEntity(
                subject,
                question.content(),
                question.correctOption(),
                question.explanation(),
                question.summary(),
                null,
                targetDifficulty
        );
    }
}
