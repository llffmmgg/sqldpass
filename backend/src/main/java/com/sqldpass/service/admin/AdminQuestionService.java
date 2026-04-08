package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.AdminQuestionUpdateRequest;
import com.sqldpass.controller.admin.dto.QuestionVerifyHistoryResponse;
import com.sqldpass.controller.admin.dto.QuestionVerifyResultResponse;
import com.sqldpass.controller.admin.dto.QuestionVerifyRunResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.question.QuestionVerificationRunEntity;
import com.sqldpass.persistent.question.QuestionVerificationRunRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.dto.AiVerificationRequest;
import com.sqldpass.service.generation.dto.AiVerificationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminQuestionService {

    private static final String ENGINEER_ROOT_NAME = "\uC815\uBCF4\uCC98\uB9AC\uAE30\uC0AC \uC2E4\uAE30";
    private static final String COMPUTER_LITERACY_ROOT_NAME = "\uCEF4\uD4E8\uD130\uD65C\uC6A9\uB2A5\uB825 1\uAE09 \uC2E4\uAE30";
    private static final List<String> SQLD_EXCLUDED_ROOTS = List.of(
            ENGINEER_ROOT_NAME, COMPUTER_LITERACY_ROOT_NAME);

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionVerificationRunRepository questionVerificationRunRepository;
    private final AiProvider verifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminQuestionService(QuestionRepository questionRepository,
                                SubjectRepository subjectRepository,
                                QuestionVerificationRunRepository questionVerificationRunRepository,
                                @Qualifier("verifier") AiProvider verifier) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.questionVerificationRunRepository = questionVerificationRunRepository;
        this.verifier = verifier;
    }

    @Transactional
    public QuestionVerifyRunResponse verifyAll(ExamType examType, Long subjectId, int limit, boolean forceRecheck) {
        int requestedLimit = limit > 0 ? limit : 100;
        SubjectEntity subject = subjectId != null ? subjectRepository.findById(subjectId).orElse(null) : null;
        List<QuestionEntity> questions = fetchQuestionsForVerification(examType, subjectId, requestedLimit, !forceRecheck);

        List<QuestionVerifyResultResponse> suspicious = new ArrayList<>();
        int processed = 0;
        LocalDateTime completedAt = LocalDateTime.now();

        for (QuestionEntity question : questions) {
            try {
                GeneratedQuestion generatedQuestion = new GeneratedQuestion(
                        question.getContent(),
                        question.getCorrectOption(),
                        question.getExplanation(),
                        question.getSummary(),
                        question.getTopic(),
                        question.getDifficulty(),
                        question.getQuestionType() != null ? question.getQuestionType().name() : null,
                        question.getAnswer(),
                        parseKeywords(question.getKeywords()));
                AiVerificationResponse response = verifier.verifyQuestion(
                        new AiVerificationRequest(resolveExamType(question), question.getSubject().getName(), generatedQuestion));
                if (!response.approved()) {
                    suspicious.add(new QuestionVerifyResultResponse(
                            question.getId(), question.getSubject().getName(), question.getSummary(), response.reason()));
                }
            } catch (Exception e) {
                log.warn("Question #{} verification failed: {}", question.getId(), e.getMessage());
                suspicious.add(new QuestionVerifyResultResponse(
                        question.getId(),
                        question.getSubject().getName(),
                        question.getSummary(),
                        "검증 호출 실패: " + e.getMessage()));
            }

            question.markVerified(completedAt);
            processed++;
            if (processed % 20 == 0) {
                log.info("LLM direct verification progress {}/{}", processed, questions.size());
            }
        }

        QuestionVerificationRunEntity run = questionVerificationRunRepository.save(
                new QuestionVerificationRunEntity(
                        examType,
                        subject,
                        subject != null ? subject.getName() : null,
                        requestedLimit,
                        forceRecheck,
                        processed,
                        suspicious.size(),
                        completedAt));

        log.info("LLM direct verification complete - processed={}, suspicious={}", processed, suspicious.size());

        return new QuestionVerifyRunResponse(
                run.getExamType(),
                run.getSubject() != null ? run.getSubject().getId() : null,
                run.getSubjectName(),
                run.getLimitRequested(),
                run.isForceRecheck(),
                run.getProcessedCount(),
                run.getSuspiciousCount(),
                run.getCompletedAt(),
                suspicious,
                getVerifyHistory(5));
    }

    public List<QuestionVerifyHistoryResponse> getVerifyHistory(int limit) {
        int size = limit > 0 ? limit : 5;
        return questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, size)).stream()
                .map(QuestionVerifyHistoryResponse::from)
                .toList();
    }

    public Page<AdminQuestionResponse> getQuestions(Long subjectId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (subjectId != null) {
            return questionRepository.findBySubjectIdWithSubject(subjectId, pageable)
                    .map(AdminQuestionResponse::from);
        }
        return questionRepository.findAllWithSubject(pageable)
                .map(AdminQuestionResponse::from);
    }

    public AdminQuestionResponse getQuestion(Long id) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
        return AdminQuestionResponse.from(entity);
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(Long id, AdminQuestionUpdateRequest request) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));

        QuestionType questionType;
        try {
            questionType = QuestionType.valueOf(request.questionType());
        } catch (IllegalArgumentException e) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "유효하지 않은 questionType: " + request.questionType());
        }

        if (questionType == QuestionType.MCQ) {
            if (request.correctOption() == null) {
                throw new SqldpassException(ErrorCode.INVALID_INPUT, "MCQ 문제는 정답 옵션(1~4)이 필수입니다.");
            }
            entity.updateMcq(request.content(), request.correctOption(), request.explanation(), request.summary());
        } else {
            String keywordsJson = null;
            if (request.keywords() != null && !request.keywords().isEmpty()) {
                try {
                    keywordsJson = objectMapper.writeValueAsString(request.keywords());
                } catch (Exception e) {
                    throw new SqldpassException(ErrorCode.INVALID_INPUT, "keywords 직렬화 실패: " + e.getMessage());
                }
            }
            entity.updateShortAnswer(request.content(), questionType, request.answer(), keywordsJson,
                    request.explanation(), request.summary());
        }
        return AdminQuestionResponse.from(entity);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionRepository.deleteById(id);
    }

    public long countAll() {
        return questionRepository.count();
    }

    public long countToday() {
        return questionRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
    }

    private List<QuestionEntity> fetchQuestionsForVerification(ExamType examType, Long subjectId,
                                                               int limit, boolean onlyUnverified) {
        PageRequest pageable = PageRequest.of(0, limit);
        if (examType == null) {
            return questionRepository.findAllForVerification(subjectId, onlyUnverified, pageable);
        }
        return switch (examType) {
            case SQLD -> questionRepository.findSqldForVerification(SQLD_EXCLUDED_ROOTS, subjectId, onlyUnverified, pageable);
            case ENGINEER_PRACTICAL -> questionRepository.findByRootNameForVerification(
                    ENGINEER_ROOT_NAME, subjectId, onlyUnverified, pageable);
            case COMPUTER_LITERACY_1 -> questionRepository.findByRootNameForVerification(
                    COMPUTER_LITERACY_ROOT_NAME, subjectId, onlyUnverified, pageable);
        };
    }

    private ExamType resolveExamType(QuestionEntity question) {
        String rootName = question.getSubject().getParent() != null
                ? question.getSubject().getParent().getName()
                : question.getSubject().getName();
        if (ENGINEER_ROOT_NAME.equals(rootName)) {
            return ExamType.ENGINEER_PRACTICAL;
        }
        if (COMPUTER_LITERACY_ROOT_NAME.equals(rootName)) {
            return ExamType.COMPUTER_LITERACY_1;
        }
        return ExamType.SQLD;
    }

    private List<String> parseKeywords(String rawKeywords) {
        if (rawKeywords == null || rawKeywords.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawKeywords, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse keywords JSON for verification, using raw text");
            return List.of(rawKeywords);
        }
    }
}
