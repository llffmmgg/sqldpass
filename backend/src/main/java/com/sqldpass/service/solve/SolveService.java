package com.sqldpass.service.solve;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.solve.dto.SolveAnswerRequest;
import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveAnswerEntity;
import com.sqldpass.persistent.solve.SolveEntity;
import com.sqldpass.persistent.solve.SolveMapper;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

@Service
@Transactional(readOnly = true)
public class SolveService {

    private final SolveRepository solveRepository;
    private final QuestionRepository questionRepository;
    private final MemberRepository memberRepository;
    private final SubjectRepository subjectRepository;
    private final MockExamRepository mockExamRepository;

    public SolveService(SolveRepository solveRepository, QuestionRepository questionRepository,
                        MemberRepository memberRepository, SubjectRepository subjectRepository,
                        MockExamRepository mockExamRepository) {
        this.solveRepository = solveRepository;
        this.questionRepository = questionRepository;
        this.memberRepository = memberRepository;
        this.subjectRepository = subjectRepository;
        this.mockExamRepository = mockExamRepository;
    }

    @Transactional
    public Solve solve(Long memberId, SolveRequest request) {
        // subjectId 또는 mockExamId 중 정확히 하나는 있어야 함
        if ((request.subjectId() == null) == (request.mockExamId() == null)) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "subjectId 또는 mockExamId 중 하나만 지정해야 합니다.");
        }

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> questionIds = request.answers().stream()
                .map(SolveAnswerRequest::questionId)
                .toList();

        Map<Long, QuestionEntity> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

        int totalCount = request.answers().size();

        // 정답 수 계산
        int correctCount = 0;
        for (SolveAnswerRequest answerReq : request.answers()) {
            QuestionEntity question = questionMap.get(answerReq.questionId());
            if (question == null) {
                throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND, "문제를 찾을 수 없습니다. id=" + answerReq.questionId());
            }
            if (answerReq.selectedOption() == question.getCorrectOption()) {
                correctCount++;
            }
        }

        int score = totalCount > 0 ? (int) Math.round((double) correctCount / totalCount * 100) : 0;

        // 풀이 종류에 따라 SolveEntity 생성
        SolveEntity solveEntity;
        if (request.subjectId() != null) {
            SubjectEntity subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));
            solveEntity = new SolveEntity(member, subject, totalCount, correctCount, score);
        } else {
            MockExamEntity mockExam = mockExamRepository.findById(request.mockExamId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
            solveEntity = new SolveEntity(member, mockExam, totalCount, correctCount, score);
        }

        for (SolveAnswerRequest answerReq : request.answers()) {
            QuestionEntity question = questionMap.get(answerReq.questionId());
            boolean isCorrect = answerReq.selectedOption() == question.getCorrectOption();
            SolveAnswerEntity answerEntity = new SolveAnswerEntity(
                    solveEntity, question, answerReq.selectedOption(), question.getCorrectOption(), isCorrect
            );
            solveEntity.addAnswer(answerEntity);
        }

        solveRepository.save(solveEntity);
        return SolveMapper.toDomain(solveEntity);
    }

    public List<Solve> getMySolves(Long memberId) {
        return solveRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SolveMapper::toDomain)
                .toList();
    }

    public Solve getSolve(Long id, Long memberId) {
        SolveEntity entity = solveRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SOLVE_NOT_FOUND));
        if (!entity.getMember().getId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        return SolveMapper.toDomain(entity);
    }
}
