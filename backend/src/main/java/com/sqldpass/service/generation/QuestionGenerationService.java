package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;

@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final AiProvider generator;
    private final AiProvider verifier;
    private final int questionsPerSubject;
    private final boolean verificationEnabled;
    private final int maxCallsPerRun;

    public QuestionGenerationService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            @Qualifier("generator") AiProvider generator,
            @Qualifier("verifier") AiProvider verifier,
            @Value("${sqldpass.ai.generation.questions-per-subject:3}") int questionsPerSubject,
            @Value("${sqldpass.ai.generation.verification-enabled:true}") boolean verificationEnabled,
            @Value("${sqldpass.ai.generation.max-calls-per-run:20}") int maxCallsPerRun) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.generator = generator;
        this.verifier = verifier;
        this.questionsPerSubject = questionsPerSubject;
        this.verificationEnabled = verificationEnabled;
        this.maxCallsPerRun = maxCallsPerRun;
    }

    public GenerationResult generateAll() {
        List<SubjectEntity> leafSubjects = subjectRepository.findByChildrenIsEmpty();
        int totalGenerated = 0;
        int totalVerified = 0;
        int totalSaved = 0;
        List<String> errors = new ArrayList<>();
        int callCount = 0;

        for (SubjectEntity subject : leafSubjects) {
            if (callCount >= maxCallsPerRun) {
                log.warn("Max API calls reached ({}), stopping generation", maxCallsPerRun);
                errors.add("호출 상한 도달로 중단됨");
                break;
            }

            try {
                List<String> existingSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

                AiGenerationRequest request = new AiGenerationRequest(
                        subject.getName(), subject.getId(), existingSummaries, questionsPerSubject);

                AiGenerationResponse response = generator.generateQuestions(request);
                callCount++;
                totalGenerated += response.questions().size();

                for (GeneratedQuestion question : response.questions()) {
                    if (callCount >= maxCallsPerRun) {
                        errors.add("호출 상한 도달로 검증 중단됨");
                        break;
                    }

                    if (verificationEnabled) {
                        try {
                            AiVerificationRequest verifyRequest = new AiVerificationRequest(subject.getName(), question);
                            AiVerificationResponse verifyResponse = verifier.verifyQuestion(verifyRequest);
                            callCount++;

                            if (!verifyResponse.approved()) {
                                log.info("Question rejected for subject '{}': {}", subject.getName(), verifyResponse.reason());
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

                    // 중복 체크 (summary 기반)
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
            } catch (Exception e) {
                log.error("Generation failed for subject '{}'", subject.getName(), e);
                errors.add("생성 실패 [" + subject.getName() + "]: " + e.getMessage());
            }
        }

        log.info("Generation complete: generated={}, verified={}, saved={}, errors={}",
                totalGenerated, totalVerified, totalSaved, errors.size());
        return new GenerationResult(totalGenerated, totalVerified, totalSaved, errors);
    }
}
