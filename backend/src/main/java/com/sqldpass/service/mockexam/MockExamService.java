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

    /** 모의고사 목록 조회 (최신 sequence 순) — 단일 GROUP BY 쿼리로 N+1 방지 */
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

    /** 모의고사 상세 (50문항 포함, 정답 미포함) */
    public MockExam get(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(entity);
    }

    /** 신규 모의고사 생성 (관리자 호출). examType null 이면 SQLD 기본값. */
    @Transactional
    public MockExam create(ExamType examType) {
        return create(examType, null);
    }

    /**
     * 신규 모의고사 생성 (관리자 호출, 난이도 지정 가능).
     * - difficulty는 정처기에만 적용. SQLD는 무시.
     * - difficulty null이면 NORMAL 기본값(EngineerMockExamCreator 내부 처리).
     */
    @Transactional
    public MockExam create(ExamType examType, MockExamDifficulty difficulty) {
        ExamType type = examType != null ? examType : ExamType.SQLD;
        MockExamEntity created = switch (type) {
            case SQLD -> mockExamCreator.create();
            case ENGINEER_PRACTICAL -> engineerMockExamCreator.create(difficulty);
            case COMPUTER_LITERACY_1 -> computerLiteracyMockExamCreator.create(difficulty);
        };
        // parent 과목까지 fetch한 상태로 리로드 (매퍼에서 N+1 방지)
        MockExamEntity loaded = mockExamRepository.findByIdWithQuestions(created.getId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(loaded);
    }

    /** 모의고사 삭제 (관리자) */
    @Transactional
    public void delete(Long id) {
        if (!mockExamRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        // 편성된 문제들을 풀로 복귀(FK 해제) 후 모의고사 삭제
        questionRepository.releaseFromMockExam(id);
        mockExamRepository.deleteById(id);
    }
}
