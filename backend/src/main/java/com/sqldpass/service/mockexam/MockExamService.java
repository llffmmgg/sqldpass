package com.sqldpass.service.mockexam;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamMapper;
import com.sqldpass.persistent.mockexam.MockExamRepository;
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

    public List<MockExam> getAll() {
        return mockExamRepository.findAllWithQuestionCounts().stream()
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

    public MockExam get(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(entity);
    }

    @Transactional
    public MockExam create(ExamType examType) {
        return create(examType, null);
    }

    /**
     * Create a new mock exam with an optional difficulty preset.
     * The preset is forwarded to SQLD, engineer practical, and computer literacy creators.
     * When null, each creator falls back to NORMAL.
     */
    @Transactional
    public MockExam create(ExamType examType, MockExamDifficulty difficulty) {
        ExamType type = examType != null ? examType : ExamType.SQLD;
        MockExamEntity created = switch (type) {
            case SQLD -> mockExamCreator.create(difficulty);
            case ENGINEER_PRACTICAL -> engineerMockExamCreator.create(difficulty);
            case COMPUTER_LITERACY_1 -> computerLiteracyMockExamCreator.create(difficulty);
        };

        MockExamEntity loaded = mockExamRepository.findByIdWithQuestions(created.getId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(loaded);
    }

    @Transactional
    public void delete(Long id) {
        if (!mockExamRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        questionRepository.releaseFromMockExam(id);
        mockExamRepository.deleteById(id);
    }
}
