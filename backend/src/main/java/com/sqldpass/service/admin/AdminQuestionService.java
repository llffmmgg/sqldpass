package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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
        this.readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.writeTx = new TransactionTemplate(transactionManager);
        // verifyAll 외부에 클래스 레벨 @Transactional(readOnly=true)가 걸려 있어
        // 기본 PROPAGATION_REQUIRED로 join하면 write가 read-only 커넥션에서 실행됨.
        // REQUIRES_NEW로 outer tx를 suspend하고 독립 write tx 생성.
        this.writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
    private static final int BATCH_SIZE_MCQ = 10;
    private static final int BATCH_SIZE_SHORT = 5;

    public QuestionVerifyRunResponse verifyAll(ExamType examType, Long subjectId, int limit, boolean forceRecheck) {
        int requestedLimit = limit > 0 ? limit : 100;

        // ── Phase 1: 짧은 read tx — 대상 스냅샷 추출 (트리아지 정렬) ────────────
        VerificationFetchResult fetched = readOnlyTx.execute(status -> {
            SubjectEntity subject = subjectId != null
                    ? subjectRepository.findById(subjectId)
                            .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND))
                    : null;

            List<Long> ids = fetchTriageIdsForVerification(examType, subjectId, requestedLimit, !forceRecheck);
            if (ids.isEmpty()) {
                return new VerificationFetchResult(subject, List.of());
            }
            List<QuestionEntity> entities = questionRepository.findByIdInWithSubjectAndParent(ids);
            List<QuestionSnapshot> snapshots = ids.stream()
                    .map(id -> entities.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .map(this::toSnapshot)
                    .toList();
            return new VerificationFetchResult(subject, snapshots);
        });

        SubjectEntity subject = fetched.subject();
        List<QuestionSnapshot> snapshots = fetched.snapshots();

        // ── Phase 2: 트랜잭션 밖 — 배치 LLM 검증 + 자동 fix ─────────────────────
        List<QuestionVerifyResultResponse> suspicious = new ArrayList<>();
        List<Long> verifiedIds = new ArrayList<>();          // APPROVED 또는 fix 성공
        Map<Long, FixedQuestionPayload> fixedPayloads = new java.util.LinkedHashMap<>();
        int suspiciousCount = 0;
        int fixedCount = 0;
        int unfixableCount = 0;
        int errorCount = 0;
        int processed = 0;

        // 시험·과목·유형이 같은 것끼리 묶어 배치 검증
        Map<String, List<QuestionSnapshot>> grouped = groupForBatch(snapshots);
        for (Map.Entry<String, List<QuestionSnapshot>> entry : grouped.entrySet()) {
            List<QuestionSnapshot> bucket = entry.getValue();
            int batchSize = isShortAnswerBucket(bucket) ? BATCH_SIZE_SHORT : BATCH_SIZE_MCQ;

            for (int start = 0; start < bucket.size(); start += batchSize) {
                int end = Math.min(start + batchSize, bucket.size());
                List<QuestionSnapshot> chunk = bucket.subList(start, end);

                List<AiVerificationRequest> requests = chunk.stream()
                        .map(this::toVerificationRequest)
                        .toList();

                List<AiVerificationResponse> outcomes;
                try {
                    outcomes = verifier.verifyQuestionsBatch(requests);
                } catch (Exception e) {
                    log.warn("Batch verify call threw — marking all UNKNOWN: {}", e.getMessage());
                    outcomes = chunk.stream()
                            .map(s -> AiVerificationResponse.ofUnknown("호출 예외: " + e.getMessage()))
                            .toList();
                }

                // 결과 처리
                for (int i = 0; i < chunk.size(); i++) {
                    QuestionSnapshot snap = chunk.get(i);
                    AiVerificationResponse outcome = i < outcomes.size()
                            ? outcomes.get(i)
                            : AiVerificationResponse.ofUnknown("응답 누락");

                    processed++;

                    switch (outcome.outcome()) {
                        case APPROVED -> verifiedIds.add(snap.id());
                        case REJECTED -> {
                            suspiciousCount++;
                            String reason = outcome.reason();
                            String suggestedFix = null;
                            boolean fixedOk = false;

                            // 자동 fix 시도 (회차당 1회)
                            if (Boolean.TRUE.equals(outcome.fixable()) || outcome.fixable() == null) {
                                FixedQuestionPayload fix = tryAutoFix(snap, reason);
                                if (fix != null) {
                                    fixedPayloads.put(snap.id(), fix);
                                    fixedOk = true;
                                    fixedCount++;
                                    suggestedFix = "자동 수정 적용됨";
                                }
                            }
                            if (!fixedOk) {
                                unfixableCount++;
                            }
                            suspicious.add(new QuestionVerifyResultResponse(
                                    snap.id(), snap.subjectName(), snap.summary(),
                                    (fixedOk ? "[자동수정] " : "") + reason));
                        }
                        case UNKNOWN -> {
                            errorCount++;
                            suspicious.add(new QuestionVerifyResultResponse(
                                    snap.id(), snap.subjectName(), snap.summary(),
                                    "판단 불가: " + outcome.reason()));
                        }
                    }
                }
                log.info("Batch verify [{}] {}~{}/{}", entry.getKey(), start, end, bucket.size());
            }
        }

        LocalDateTime completedAt = LocalDateTime.now();
        int processedFinal = processed;
        int suspiciousFinal = suspiciousCount;
        int fixedFinal = fixedCount;
        int unfixableFinal = unfixableCount;
        int errorFinal = errorCount;

        // ── Phase 3: 짧은 write tx — fix 적용 + verifiedAt 마킹 + run 저장 ─────
        QuestionVerificationRunEntity run = writeTx.execute(status -> {
            // 1) 자동 fix 적용 — fix된 문제는 update*() 안에서 verifiedAt = null로 자동 리셋됨
            if (!fixedPayloads.isEmpty()) {
                List<Long> fixIds = new ArrayList<>(fixedPayloads.keySet());
                List<QuestionEntity> targets = questionRepository.findByIdInWithSubjectAndParent(fixIds);
                Map<Long, QuestionEntity> byId = targets.stream()
                        .collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, q -> q));
                for (Map.Entry<Long, FixedQuestionPayload> e : fixedPayloads.entrySet()) {
                    QuestionEntity entity = byId.get(e.getKey());
                    if (entity == null) continue;
                    applyFix(entity, e.getValue());
                }
            }
            // 2) APPROVED 일괄 마킹
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
                            suspiciousFinal,
                            fixedFinal,
                            unfixableFinal,
                            errorFinal,
                            completedAt));
        });

        log.info("Batch verification complete - processed={}, suspicious={}, fixed={}, unfixable={}, error={}",
                processedFinal, suspiciousFinal, fixedFinal, unfixableFinal, errorFinal);

        return new QuestionVerifyRunResponse(
                run.getExamType(),
                run.getSubject() != null ? run.getSubject().getId() : null,
                run.getSubjectName(),
                run.getLimitRequested(),
                run.isForceRecheck(),
                run.getProcessedCount(),
                run.getSuspiciousCount(),
                run.getFixedCount(),
                run.getUnfixableCount(),
                run.getErrorCount(),
                run.getCompletedAt(),
                suspicious,
                getVerifyHistory(5));
    }

    /** 트리아지 정렬로 검증 대상 ID 추출 — 피드백/저정답률/미검증 우선 */
    private List<Long> fetchTriageIdsForVerification(ExamType examType, Long subjectId,
                                                     int limit, boolean onlyUnverified) {
        String rootName = null;
        List<String> excludedRoots = null;
        if (examType != null) {
            switch (examType) {
                case SQLD -> excludedRoots = SQLD_EXCLUDED_ROOTS;
                case ENGINEER_PRACTICAL -> rootName = ENGINEER_ROOT_NAME;
                case COMPUTER_LITERACY_1 -> rootName = COMPUTER_LITERACY_ROOT_NAME;
            }
        }
        return questionRepository.findTriageIdsForVerification(
                rootName, excludedRoots, subjectId, onlyUnverified, limit);
    }

    /** 같은 시험·과목·유형끼리 묶어서 배치 검증할 그룹 키 생성 */
    private Map<String, List<QuestionSnapshot>> groupForBatch(List<QuestionSnapshot> snapshots) {
        Map<String, List<QuestionSnapshot>> map = new java.util.LinkedHashMap<>();
        for (QuestionSnapshot s : snapshots) {
            String key = s.examType() + "|" + s.subjectName() + "|"
                    + (s.questionType() != null ? s.questionType().name() : "MCQ");
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return map;
    }

    private boolean isShortAnswerBucket(List<QuestionSnapshot> bucket) {
        if (bucket.isEmpty()) return false;
        QuestionType qt = bucket.get(0).questionType();
        return qt == QuestionType.SHORT_ANSWER || qt == QuestionType.DESCRIPTIVE;
    }

    private AiVerificationRequest toVerificationRequest(QuestionSnapshot snap) {
        GeneratedQuestion gq = new GeneratedQuestion(
                snap.content(), snap.correctOption(), snap.explanation(), snap.summary(),
                snap.topic(), snap.difficulty(),
                snap.questionType() != null ? snap.questionType().name() : null,
                snap.answer(), snap.keywords());
        return new AiVerificationRequest(snap.examType(), snap.subjectName(), gq);
    }

    /** 거절된 문제에 대해 LLM에게 fix 요청 — 성공 시 적용 payload 반환 */
    private FixedQuestionPayload tryAutoFix(QuestionSnapshot snap, String reason) {
        try {
            GeneratedQuestion original = new GeneratedQuestion(
                    snap.content(), snap.correctOption(), snap.explanation(), snap.summary(),
                    snap.topic(), snap.difficulty(),
                    snap.questionType() != null ? snap.questionType().name() : null,
                    snap.answer(), snap.keywords());
            QuestionType qt = snap.questionType() != null ? snap.questionType() : QuestionType.MCQ;
            GeneratedQuestion fixed = verifier.fixQuestion(original, reason, snap.examType(), qt);
            if (fixed == null) return null;
            return new FixedQuestionPayload(qt, fixed);
        } catch (Exception e) {
            log.warn("Auto-fix failed for #{}: {}", snap.id(), e.getMessage());
            return null;
        }
    }

    /** 자동 fix payload를 영속 엔티티에 적용. update*() 가 verifiedAt=null 자동 리셋. */
    private void applyFix(QuestionEntity entity, FixedQuestionPayload payload) {
        GeneratedQuestion gq = payload.fixed();
        if (payload.questionType() == QuestionType.MCQ) {
            int co = gq.correctOption() != null ? gq.correctOption() : 1;
            entity.updateMcq(gq.content(), co,
                    gq.explanation(),
                    gq.summary());
        } else {
            String keywordsJson = "[]";
            if (gq.keywords() != null && !gq.keywords().isEmpty()) {
                try {
                    keywordsJson = objectMapper.writeValueAsString(gq.keywords());
                } catch (Exception e) {
                    log.warn("Failed to serialize fix keywords: {}", e.getMessage());
                }
            }
            entity.updateShortAnswer(
                    gq.content(),
                    payload.questionType(),
                    gq.answerText() != null ? gq.answerText() : "",
                    keywordsJson,
                    gq.explanation(),
                    gq.summary());
        }
    }

    /** Phase 2 → Phase 3 전달용 fix payload */
    private record FixedQuestionPayload(QuestionType questionType, GeneratedQuestion fixed) {}

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
