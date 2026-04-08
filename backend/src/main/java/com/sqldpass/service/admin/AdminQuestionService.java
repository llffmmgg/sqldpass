package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate readOnlyTx;
    private final TransactionTemplate writeTx;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminQuestionService(QuestionRepository questionRepository,
                                SubjectRepository subjectRepository,
                                QuestionVerificationRunRepository questionVerificationRunRepository,
                                @Qualifier("verifier") AiProvider verifier,
                                PlatformTransactionManager transactionManager) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.questionVerificationRunRepository = questionVerificationRunRepository;
        this.verifier = verifier;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    /**
     * 일괄 LLM 검증 — 트랜잭션을 3단계로 분리해 LLM 호출 중에는 DB 커넥션을 잡지 않는다.
     *
     *   1) Phase 1 (read-only tx): subject 조회 + 검증 대상 ID 페이징 → 엔티티 fetch → 스냅샷 DTO로 detach
     *   2) Phase 2 (no tx): 스냅샷 기반으로 verifier LLM 호출, 결과 누적
     *   3) Phase 3 (write tx): 성공한 ID에 verifiedAt 일괄 마킹 + 실행 이력 row 저장
     *
     * 결과적으로 LLM이 분 단위로 걸려도 HikariCP 커넥션을 그동안 점유하지 않음.
     */
    public QuestionVerifyRunResponse verifyAll(ExamType examType, Long subjectId, int limit, boolean forceRecheck) {
        int requestedLimit = limit > 0 ? limit : 100;

        // ── Phase 1: 짧은 read tx — 대상 스냅샷 추출 ────────────────────────────
        VerificationFetchResult fetched = readOnlyTx.execute(status -> {
            SubjectEntity subject = subjectId != null
                    ? subjectRepository.findById(subjectId)
                            .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND))
                    : null;

            List<Long> ids = fetchIdsForVerification(examType, subjectId, requestedLimit, !forceRecheck);
            if (ids.isEmpty()) {
                return new VerificationFetchResult(subject, List.of());
            }
            List<QuestionEntity> entities = questionRepository.findByIdInWithSubjectAndParent(ids);
            // ID 순서(=createdAt DESC) 보존
            List<QuestionSnapshot> snapshots = ids.stream()
                    .map(id -> entities.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .map(this::toSnapshot)
                    .toList();
            return new VerificationFetchResult(subject, snapshots);
        });

        SubjectEntity subject = fetched.subject();
        List<QuestionSnapshot> snapshots = fetched.snapshots();

        // ── Phase 2: 트랜잭션 밖 — LLM 호출 루프 ─────────────────────────────────
        List<QuestionVerifyResultResponse> suspicious = new ArrayList<>();
        List<Long> verifiedIds = new ArrayList<>();
        int processed = 0;

        for (QuestionSnapshot snap : snapshots) {
            try {
                GeneratedQuestion generatedQuestion = new GeneratedQuestion(
                        snap.content(), snap.correctOption(), snap.explanation(), snap.summary(),
                        snap.topic(), snap.difficulty(),
                        snap.questionType() != null ? snap.questionType().name() : null,
                        snap.answer(), snap.keywords());
                AiVerificationResponse response = verifier.verifyQuestion(
                        new AiVerificationRequest(snap.examType(), snap.subjectName(), generatedQuestion));
                if (!response.approved()) {
                    suspicious.add(new QuestionVerifyResultResponse(
                            snap.id(), snap.subjectName(), snap.summary(), response.reason()));
                }
                // 호출이 정상 끝났을 때만 검증 완료 마킹 — 실패한 문제는 다음 회차에 다시 잡히도록 유지
                verifiedIds.add(snap.id());
            } catch (Exception e) {
                log.warn("Question #{} verification failed: {}", snap.id(), e.getMessage());
                suspicious.add(new QuestionVerifyResultResponse(
                        snap.id(), snap.subjectName(), snap.summary(),
                        "검증 호출 실패: " + e.getMessage()));
            }
            processed++;
            if (processed % 20 == 0) {
                log.info("LLM direct verification progress {}/{}", processed, snapshots.size());
            }
        }

        LocalDateTime completedAt = LocalDateTime.now();
        int processedFinal = processed;
        int suspiciousCount = suspicious.size();

        // ── Phase 3: 짧은 write tx — 일괄 markVerified + run 저장 ────────────────
        QuestionVerificationRunEntity run = writeTx.execute(status -> {
            if (!verifiedIds.isEmpty()) {
                questionRepository.markVerifiedInBatch(verifiedIds, completedAt);
            }
            return questionVerificationRunRepository.save(
                    new QuestionVerificationRunEntity(
                            examType,
                            subject,
                            subject != null ? subject.getName() : null,
                            requestedLimit,
                            forceRecheck,
                            processedFinal,
                            suspiciousCount,
                            completedAt));
        });

        log.info("LLM direct verification complete - processed={}, suspicious={}, verified={}",
                processedFinal, suspiciousCount, verifiedIds.size());

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

    /** 검증 대상 ID 페이징 (시험·과목 필터 분기) */
    private List<Long> fetchIdsForVerification(ExamType examType, Long subjectId,
                                               int limit, boolean onlyUnverified) {
        PageRequest pageable = PageRequest.of(0, limit);
        if (examType == null) {
            return questionRepository.findIdsForVerification(subjectId, onlyUnverified, pageable);
        }
        return switch (examType) {
            case SQLD -> questionRepository.findSqldIdsForVerification(SQLD_EXCLUDED_ROOTS, subjectId, onlyUnverified, pageable);
            case ENGINEER_PRACTICAL -> questionRepository.findIdsByRootNameForVerification(
                    ENGINEER_ROOT_NAME, subjectId, onlyUnverified, pageable);
            case COMPUTER_LITERACY_1 -> questionRepository.findIdsByRootNameForVerification(
                    COMPUTER_LITERACY_ROOT_NAME, subjectId, onlyUnverified, pageable);
        };
    }

    /** 검증 페치 결과 — Phase 1 출력 */
    private record VerificationFetchResult(SubjectEntity subject, List<QuestionSnapshot> snapshots) {}

    /** 트랜잭션 밖에서 LLM 호출에 사용할 detached 스냅샷 */
    private record QuestionSnapshot(
            Long id,
            String subjectName,
            ExamType examType,
            String content,
            QuestionType questionType,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation,
            String summary,
            String topic,
            Integer difficulty
    ) {}

    private QuestionSnapshot toSnapshot(QuestionEntity q) {
        return new QuestionSnapshot(
                q.getId(),
                q.getSubject().getName(),
                resolveExamType(q),
                q.getContent(),
                q.getQuestionType(),
                q.getCorrectOption(),
                q.getAnswer(),
                parseKeywords(q.getKeywords()),
                q.getExplanation(),
                q.getSummary(),
                q.getTopic(),
                q.getDifficulty());
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
