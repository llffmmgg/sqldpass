package com.sqldpass.service.mockexam;

import java.time.LocalDateTime;
import java.util.List;

import java.time.LocalDate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.config.CacheConfig;
import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamMapper;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockExamService {

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final MockExamCreator mockExamCreator;
    private final EngineerMockExamCreator engineerMockExamCreator;
    private final ComputerLiteracyMockExamCreator computerLiteracyMockExamCreator;
    private final ComputerLiteracy2MockExamCreator computerLiteracy2MockExamCreator;
    private final EngineerWrittenMockExamCreator engineerWrittenMockExamCreator;
    private final AdspMockExamCreator adspMockExamCreator;

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
