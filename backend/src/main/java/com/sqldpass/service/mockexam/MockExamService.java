package com.sqldpass.service.mockexam;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.mockexam.MockExam;
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

    /** 모의고사 목록 조회 (최신 sequence 순) */
    public List<MockExam> getAll() {
        return mockExamRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getSequence(), a.getSequence()))
                .map(e -> MockExamMapper.toSummary(e, e.getQuestions().size()))
                .toList();
    }

    /** 모의고사 상세 (50문항 포함, 정답 미포함) */
    public MockExam get(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return MockExamMapper.toDomain(entity);
    }

    /** 신규 모의고사 생성 (관리자 호출) */
    @Transactional
    public MockExam create() {
        MockExamEntity created = mockExamCreator.create();
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
