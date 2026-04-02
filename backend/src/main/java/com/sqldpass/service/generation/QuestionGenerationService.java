package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QuestionGenerationService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final GenerationLockService lockService;
    private final AiProvider generator;
    private final AiProvider verifier;
    private final int questionsPerSubject;
    private final boolean verificationEnabled;
    private final int maxCallsPerRun;

    public QuestionGenerationService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            GenerationLockService lockService,
            @Qualifier("generator") AiProvider generator,
            @Qualifier("verifier") AiProvider verifier,
            @Value("${sqldpass.ai.generation.questions-per-subject:3}") int questionsPerSubject,
            @Value("${sqldpass.ai.generation.verification-enabled:true}") boolean verificationEnabled,
            @Value("${sqldpass.ai.generation.max-calls-per-run:20}") int maxCallsPerRun) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.lockService = lockService;
        this.generator = generator;
        this.verifier = verifier;
        this.questionsPerSubject = questionsPerSubject;
        this.verificationEnabled = verificationEnabled;
        this.maxCallsPerRun = maxCallsPerRun;
    }

    public GenerationResult generateAll() {
        return generateAll(questionsPerSubject, event -> {});
    }

    public GenerationResult generateAll(int count, Consumer<GenerationEvent> eventListener) {
        lockService.acquire();
        try {
            return doGenerate(count, eventListener);
        } finally {
            lockService.release();
        }
    }

    private GenerationResult doGenerate(int count, Consumer<GenerationEvent> eventListener) {
        List<SubjectEntity> leafSubjects = subjectRepository.findByChildrenIsEmpty();
        int totalGenerated = 0;
        int totalVerified = 0;
        int totalSaved = 0;
        List<String> errors = new ArrayList<>();
        int callCount = 0;

        eventListener.accept(GenerationEvent.progress("문제 생성을 시작합니다. 총 " + leafSubjects.size() + "개 과목"));

        for (int i = 0; i < leafSubjects.size(); i++) {
            SubjectEntity subject = leafSubjects.get(i);

            if (callCount >= maxCallsPerRun) {
                log.warn("Max API calls reached ({}), stopping generation", maxCallsPerRun);
                errors.add("호출 상한 도달로 중단됨");
                break;
            }

            eventListener.accept(GenerationEvent.progress(
                    "[" + (i + 1) + "/" + leafSubjects.size() + "] '" + subject.getName() + "' 문제 생성 중..."));

            try {
                List<String> existingSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

                AiGenerationRequest request = new AiGenerationRequest(
                        subject.getName(), subject.getId(), existingSummaries, count);

                AiGenerationResponse response = generator.generateQuestions(request);
                callCount++;
                totalGenerated += response.questions().size();

                eventListener.accept(GenerationEvent.progress(
                        "'" + subject.getName() + "' " + response.questions().size() + "문제 생성 완료. 검증 시작..."));

                for (GeneratedQuestion question : response.questions()) {
                    if (callCount >= maxCallsPerRun) {
                        errors.add("호출 상한 도달로 검증 중단됨");
                        break;
                    }

                    if (verificationEnabled) {
                        try {
                            Thread.sleep(5000);
                            AiVerificationRequest verifyRequest = new AiVerificationRequest(subject.getName(), question);
                            AiVerificationResponse verifyResponse = verifier.verifyQuestion(verifyRequest);
                            callCount++;

                            if (!verifyResponse.approved()) {
                                log.info("Question rejected for subject '{}': {}", subject.getName(), verifyResponse.reason());
                                eventListener.accept(GenerationEvent.progress(
                                        "문제 검증 실패 (사유: " + verifyResponse.reason() + ")"));
                                continue;
                            }
                            totalVerified++;
                        } catch (Exception e) {
                            log.error("Verification failed for subject '{}', skipping question", subject.getName(), e);
                            errors.add("검증 실패 [" + subject.getName() + "]: " + e.getMessage());
                            continue;
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
                            question.explanation(), question.summary());
                    questionRepository.save(entity);
                    totalSaved++;

                    if (question.summary() != null) {
                        existingSummaries.add(question.summary());
                    }
                }

                eventListener.accept(GenerationEvent.progress(
                        "'" + subject.getName() + "' 완료 (저장: " + totalSaved + "개)"));

            } catch (Exception e) {
                log.error("Generation failed for subject '{}'", subject.getName(), e);
                errors.add("생성 실패 [" + subject.getName() + "]: " + e.getMessage());
                eventListener.accept(GenerationEvent.error(
                        "'" + subject.getName() + "' 생성 실패: " + e.getMessage()));
            }
        }

        log.info("Generation complete: generated={}, verified={}, saved={}, errors={}",
                totalGenerated, totalVerified, totalSaved, errors.size());
        return new GenerationResult(totalGenerated, totalVerified, totalSaved, errors);
    }
}
