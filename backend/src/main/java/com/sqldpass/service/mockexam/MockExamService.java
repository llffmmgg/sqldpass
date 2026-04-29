package com.sqldpass.service.mockexam;

import java.time.LocalDateTime;
import java.util.List;

import java.time.LocalDate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.sqldpass.config.CacheConfig;
import com.sqldpass.controller.admin.dto.ManualMockExamRequest;
import com.sqldpass.controller.admin.dto.ManualMockExamRequest.ManualQuestion;
import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamMapper;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.QuestionContentHasher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockExamService {

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final MockExamCreator mockExamCreator;
    private final EngineerMockExamCreator engineerMockExamCreator;
    private final ComputerLiteracyMockExamCreator computerLiteracyMockExamCreator;
    private final ComputerLiteracy2MockExamCreator computerLiteracy2MockExamCreator;
    private final EngineerWrittenMockExamCreator engineerWrittenMockExamCreator;
    private final AdspMockExamCreator adspMockExamCreator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 어드민용 — DRAFT 포함 전체 회차 */
    public List<MockExam> getAll() {
        return mapRows(mockExamRepository.findAllWithQuestionCounts());
    }

    /** 사용자용 — DRAFT 제외 (PUBLISHED + PREMIUM만) */
    @Cacheable(CacheConfig.CACHE_MOCK_EXAM_LIST)
    public List<MockExam> getAllForUser() {
        return mapRows(mockExamRepository.findUserVisibleWithQuestionCounts());
    }

    private List<MockExam> mapRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    MockExamEntity exam = (MockExamEntity) row[0];
                    int count = ((Long) row[1]).intValue();
                    Double avg = row[2] != null ? ((Number) row[2]).doubleValue() : null;
                    Integer min = row[3] != null ? ((Number) row[3]).intValue() : null;
                    Integer max = row[4] != null ? ((Number) row[4]).intValue() : null;
                    return MockExamMapper.toSummary(exam, count, avg, min, max);
                })
                .toList();
    }

    /** 어드민 — visibility 변경 */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public MockExam changeVisibility(Long id, MockExamVisibility visibility) {
        MockExamEntity entity = mockExamRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        entity.changeVisibility(visibility);
        return MockExamMapper.toSummary(entity, entity.getQuestions().size(), null, null, null);
    }

    public MockExam get(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(entity);
    }

    /**
     * 사용자용 상세 조회 — DRAFT는 NOT_FOUND, PREMIUM은 잠금(예외).
     * 결제 시스템 도입 후 권한 있는 사용자만 PREMIUM 통과시키도록 확장.
     */
    /** 모의고사의 모든 문제를 수동 검수 완료 처리 */
    @Transactional
    public int markAllQuestionsVerified(Long mockExamId) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(mockExamId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        List<Long> questionIds = entity.getQuestions().stream()
                .map(q -> q.getId())
                .toList();
        if (questionIds.isEmpty()) return 0;
        return questionRepository.markVerifiedInBatch(questionIds, LocalDateTime.now());
    }

    /** 전문가 검증 완료 토글 */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public boolean toggleExpertVerified(Long mockExamId) {
        MockExamEntity entity = mockExamRepository.findById(mockExamId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        entity.toggleExpertVerified();
        return entity.isExpertVerified();
    }

    /**
     * 어드민 — 모의고사를 기출 복원(PAST_EXAM) 으로 승격하거나 AI 로 되돌림.
     * promote=true 면 kind=PAST_EXAM + 연도/회차/시험일 세팅, false 면 AI 로 초기화.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public MockExam setPastExamMeta(Long id, boolean promote,
                                    Integer examYear, Integer examRound, LocalDate examDate) {
        MockExamEntity entity = mockExamRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        if (promote) {
            entity.promoteToPastExam(examYear, examRound, examDate);
        } else {
            entity.demoteToAi();
        }
        return MockExamMapper.toSummary(entity, entity.getQuestions().size(), null, null, null);
    }

    /** 어드민용 — DRAFT/PREMIUM 제한 없이 조회 */
    public MockExam getById(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(entity);
    }

    public MockExam getForUser(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        if (entity.getVisibility() == MockExamVisibility.DRAFT) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        if (!entity.isExpertVerified()) {
            // 전문가 검수 미완료 모의고사는 사용자에게 노출하지 않음
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        if (entity.getVisibility() == MockExamVisibility.PREMIUM) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_LOCKED);
        }
        return MockExamMapper.toDomain(entity);
    }

    @Transactional
    public MockExam create(ExamType examType) {
        return create(examType, null, null);
    }

    @Transactional
    public MockExam create(ExamType examType, MockExamDifficulty difficulty) {
        return create(examType, difficulty, null);
    }

    /**
     * Create a new mock exam with an optional difficulty preset and engineer template.
     * - difficulty is forwarded to all 3 creators (null → NORMAL).
     * - engineerTemplate is only used for ENGINEER_PRACTICAL (null → 랜덤 선택).
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public MockExam create(ExamType examType, MockExamDifficulty difficulty,
                           com.sqldpass.persistent.mockexam.EngineerExamTemplate engineerTemplate) {
        ExamType type = examType != null ? examType : ExamType.SQLD;
        MockExamEntity created = switch (type) {
            case SQLD -> mockExamCreator.create(difficulty);
            case ENGINEER_PRACTICAL -> engineerMockExamCreator.create(difficulty, engineerTemplate);
            case COMPUTER_LITERACY_1 -> computerLiteracyMockExamCreator.create(difficulty);
            case COMPUTER_LITERACY_2 -> computerLiteracy2MockExamCreator.create(difficulty);
            case ENGINEER_WRITTEN -> engineerWrittenMockExamCreator.create(difficulty);
            case ADSP -> adspMockExamCreator.create(difficulty);
        };

        MockExamEntity loaded = mockExamRepository.findByIdWithQuestions(created.getId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(loaded);
    }

    /**
     * 어드민 수동 모의고사 등록 — JSON 한 통으로 모의고사 메타 + 문제 N개를 동시에 적재한다.
     * AI 자동 생성과 별도 경로. PAST_EXAM 승격 / 전문가 검수 플래그도 같은 트랜잭션에서 처리.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public MockExam createManual(ManualMockExamRequest request) {
        if (request == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "요청 본문이 비어 있습니다.");
        }
        if (request.examType() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "examType 은 필수입니다.");
        }
        List<ManualQuestion> questions = request.questions();
        if (questions == null || questions.isEmpty()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "questions 는 1개 이상이어야 합니다.");
        }

        boolean promote = Boolean.TRUE.equals(request.pastExam());
        if (promote && (request.examYear() == null || request.examRound() == null)) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "pastExam=true 이면 examYear/examRound 는 필수입니다.");
        }

        ExamType examType = request.examType();
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(examType).orElse(0) + 1;

        String name;
        if (request.name() != null && !request.name().isBlank()) {
            name = request.name().trim();
        } else {
            name = autoMockExamName(examType, nextSeq);
        }

        // AI 모의고사들과 동일하게 LATEST template ("최신 기출 분포 반영") 자동 적용
        MockExamEntity entity = new MockExamEntity(name, examType, nextSeq, EngineerExamTemplate.LATEST);
        mockExamRepository.save(entity);

        if (promote) {
            entity.promoteToPastExam(request.examYear(), request.examRound(), request.examDate());
        }
        if (Boolean.TRUE.equals(request.expertVerified())) {
            entity.toggleExpertVerified();
        }

        for (int i = 0; i < questions.size(); i++) {
            ManualQuestion q = questions.get(i);
            QuestionEntity saved = buildQuestion(q, i + 1);
            questionRepository.save(saved);
            entity.linkQuestion(saved, i + 1);
        }

        MockExamEntity loaded = mockExamRepository.findByIdWithQuestions(entity.getId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(loaded);
    }

    /**
     * "{자격증} 모의고사 {sequence}회" 형태의 자동 이름 — AI 모의고사들과 동일 형식.
     * 사용자 측 manual 등록 시 이름을 비워두면 백엔드가 sequence 기반으로 채워준다.
     */
    private static String autoMockExamName(ExamType type, int sequence) {
        String typeLabel = switch (type) {
            case SQLD -> "SQLD";
            case ENGINEER_PRACTICAL -> "정처기 실기";
            case ENGINEER_WRITTEN -> "정처기 필기";
            case COMPUTER_LITERACY_1 -> "컴활 1급";
            case COMPUTER_LITERACY_2 -> "컴활 2급";
            case ADSP -> "ADsP";
        };
        return String.format("%s 모의고사 %d회", typeLabel, sequence);
    }

    private QuestionEntity buildQuestion(ManualQuestion q, int index) {
        if (q == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "questions[" + (index - 1) + "] 가 null 입니다.");
        }
        if (q.subjectId() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "questions[" + (index - 1) + "].subjectId 는 필수입니다.");
        }
        if (q.content() == null || q.content().isBlank()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "questions[" + (index - 1) + "].content 는 필수입니다.");
        }
        SubjectEntity subject = subjectRepository.findById(q.subjectId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "questions[" + (index - 1) + "].subjectId=" + q.subjectId() + " 가 존재하지 않습니다."));

        QuestionType qt = q.questionType() != null ? q.questionType() : QuestionType.MCQ;
        QuestionEntity entity;
        if (qt == QuestionType.MCQ) {
            if (q.correctOption() == null || q.correctOption() < 1 || q.correctOption() > 4) {
                throw new SqldpassException(ErrorCode.INVALID_INPUT,
                        "questions[" + (index - 1) + "].correctOption 은 1~4 사이여야 합니다 (MCQ).");
            }
            entity = new QuestionEntity(
                    subject,
                    q.content(),
                    q.correctOption(),
                    q.explanation() != null ? q.explanation() : "",
                    q.summary(),
                    q.topic(),
                    q.difficulty());
        } else {
            if (q.answer() == null || q.answer().isBlank()) {
                throw new SqldpassException(ErrorCode.INVALID_INPUT,
                        "questions[" + (index - 1) + "].answer 는 필수입니다 (SHORT_ANSWER/DESCRIPTIVE).");
            }
            String keywordsJson;
            try {
                List<String> kws = q.keywords() != null ? q.keywords() : List.of();
                keywordsJson = objectMapper.writeValueAsString(kws);
            } catch (Exception e) {
                keywordsJson = "[]";
            }
            entity = new QuestionEntity(
                    subject,
                    q.content(),
                    qt,
                    q.answer(),
                    keywordsJson,
                    q.explanation() != null ? q.explanation() : "",
                    q.summary(),
                    q.topic(),
                    q.difficulty());
        }
        entity.assignContentHash(QuestionContentHasher.hashOf(q.content()));
        return entity;
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MOCK_EXAM_LIST, allEntries = true)
    public void delete(Long id) {
        if (!mockExamRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        questionRepository.releaseFromMockExam(id);
        mockExamRepository.deleteById(id);
    }
}
