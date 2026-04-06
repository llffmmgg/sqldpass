package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import tools.jackson.databind.ObjectMapper;
import com.sqldpass.service.generation.dto.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.notification.DiscordNotifier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QuestionGenerationService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final GenerationLockService lockService;
    private final AiProvider generator;
    private final AiProvider verifier;
    private final DiscordNotifier discordNotifier;
    private final int questionsPerSubject;
    private final boolean verificationEnabled;
    private final int maxCallsPerRun;

    public QuestionGenerationService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            GenerationLockService lockService,
            @Qualifier("generator") AiProvider generator,
            @Qualifier("verifier") AiProvider verifier,
            DiscordNotifier discordNotifier,
            @Value("${sqldpass.ai.generation.questions-per-subject:3}") int questionsPerSubject,
            @Value("${sqldpass.ai.generation.verification-enabled:true}") boolean verificationEnabled,
            @Value("${sqldpass.ai.generation.max-calls-per-run:50}") int maxCallsPerRun) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.lockService = lockService;
        this.generator = generator;
        this.verifier = verifier;
        this.discordNotifier = discordNotifier;
        this.questionsPerSubject = questionsPerSubject;
        this.verificationEnabled = verificationEnabled;
        this.maxCallsPerRun = maxCallsPerRun;
    }

    public GenerationResult generateAll() {
        return generateAll(questionsPerSubject, event -> {});
    }

    public GenerationResult generateAll(int count, Consumer<GenerationEvent> eventListener) {
        lockService.acquire();
        List<GeneratedQuestion> savedQuestions = new ArrayList<>();
        try {
            GenerationResult result = doGenerate(count, eventListener, savedQuestions);
            try {
                String resultJson = new ObjectMapper().writeValueAsString(
                        new com.sqldpass.controller.admin.dto.GenerationResultResponse(
                                result.totalGenerated(), result.totalVerified(), result.totalSaved(), result.errors()));
                lockService.complete(resultJson);
            } catch (Exception e) {
                log.error("Failed to save generation result", e);
                lockService.fail("결과 저장 실패: " + e.getMessage());
            }
            // Discord 알림 — 생성 완료 시점
            discordNotifier.notifyGenerationComplete(result, savedQuestions);
            return result;
        } catch (Exception e) {
            lockService.fail(e.getMessage());
            throw e;
        }
    }

    private GenerationResult doGenerate(int count, Consumer<GenerationEvent> eventListener,
                                         List<GeneratedQuestion> savedQuestionsOut) {
        List<SubjectEntity> leafSubjects = subjectRepository.findByChildrenIsEmpty();
        int totalGenerated = 0;
        int totalVerified = 0;
        int totalSaved = 0;
        List<String> errors = new ArrayList<>();
        int callCount = 0;

        eventListener.accept(GenerationEvent.progress("문제 생성을 시작합니다. 총 " + leafSubjects.size() + "개 과목"));

        for (SubjectEntity subject : leafSubjects) {
            List<String> topics = PromptBuilder.getTopicsForSubject(subject.getName());
            if (topics.isEmpty()) {
                log.warn("No topics defined for subject '{}'", subject.getName());
                continue;
            }

            // 문제가 가장 적은 토픽 N개 선택
            List<String> selectedTopics = topics.stream()
                    .sorted(Comparator.comparingLong(t -> questionRepository.countBySubjectIdAndTopic(subject.getId(), t)))
                    .limit(count)
                    .toList();

            eventListener.accept(GenerationEvent.progress(
                    "'" + subject.getName() + "' — 토픽: " + String.join(", ", selectedTopics)));

            for (String topic : selectedTopics) {
                if (callCount >= maxCallsPerRun) {
                    errors.add("호출 상한 도달로 중단됨");
                    eventListener.accept(GenerationEvent.error("호출 상한 도달"));
                    return new GenerationResult(totalGenerated, totalVerified, totalSaved, errors);
                }

                try {
                    List<String> existingSummaries = questionRepository.findSummariesBySubjectIdAndTopic(
                            subject.getId(), topic);

                    AiGenerationRequest request = new AiGenerationRequest(
                            subject.getName(), subject.getId(), topic, existingSummaries, 3);

                    eventListener.accept(GenerationEvent.progress("'" + topic + "' 기본/심화/고난도 3문제 생성 중..."));

                    AiGenerationResponse response = generator.generateQuestions(request);
                    callCount++;

                    if (response.questions().isEmpty()) {
                        errors.add("생성 실패 [" + topic + "]: 빈 응답");
                        continue;
                    }

                    totalGenerated += response.questions().size();

                    for (GeneratedQuestion question : response.questions()) {
                        if (callCount >= maxCallsPerRun) {
                            errors.add("호출 상한 도달로 검증 중단됨");
                            break;
                        }

                        question = new GeneratedQuestion(
                                question.content(), question.correctOption(), question.explanation(),
                                question.summary(), topic, question.difficulty());
                        // 현재 사용하지 않음
                        if (verificationEnabled && callCount < maxCallsPerRun) {
                            try {
                                Thread.sleep(10000);
                                AiVerificationRequest verifyRequest = new AiVerificationRequest(subject.getName(), question);
                                AiVerificationResponse verifyResponse = verifier.verifyQuestion(verifyRequest);
                                callCount++;

                                if (!verifyResponse.approved()) {
                                    eventListener.accept(GenerationEvent.progress(
                                            "'" + topic + "' 검증 실패: " + verifyResponse.reason() + " → 수정 요청"));

                                    if (callCount < maxCallsPerRun) {
                                        GeneratedQuestion fixed = generator.fixQuestion(question, verifyResponse.reason());
                                        callCount++;
                                        if (fixed != null) {
                                            question = new GeneratedQuestion(
                                                    fixed.content(), fixed.correctOption(), fixed.explanation(),
                                                    fixed.summary(), topic, fixed.difficulty());
                                            eventListener.accept(GenerationEvent.progress("'" + topic + "' 수정 완료"));
                                        } else {
                                            eventListener.accept(GenerationEvent.progress("'" + topic + "' 수정 불가, 건너뜀"));
                                            continue;
                                        }
                                    }
                                }
                                totalVerified++;
                            } catch (Exception e) {
                                log.error("Verification failed for topic '{}', saving anyway", topic, e);
                                errors.add("검증 실패 [" + topic + "]: " + e.getMessage());
                            }
                        } else {
                            totalVerified++;
                        }

                        if (question.summary() != null && existingSummaries.contains(question.summary())) {
                            log.info("Duplicate summary detected, skipping: {}", question.summary());
                            continue;
                        }

                        QuestionEntity entity = new QuestionEntity(
                                subject, question.content(), question.correctOption(),
                                question.explanation(), question.summary(), question.topic(), question.difficulty());
                        questionRepository.save(entity);
                        totalSaved++;
                        savedQuestionsOut.add(question);

                        if (question.summary() != null) {
                            existingSummaries.add(question.summary());
                        }
                    }

                    eventListener.accept(GenerationEvent.progress("'" + topic + "' 저장 완료"));

                } catch (Exception e) {
                    log.error("Generation failed for topic '{}'", topic, e);
                    errors.add("생성 실패 [" + topic + "]: " + e.getMessage());
                    eventListener.accept(GenerationEvent.error("'" + topic + "' 실패: " + e.getMessage()));
                }
            }
        }

        log.info("Generation complete: generated={}, verified={}, saved={}, errors={}",
                totalGenerated, totalVerified, totalSaved, errors.size());
        return new GenerationResult(totalGenerated, totalVerified, totalSaved, errors);
    }
}
