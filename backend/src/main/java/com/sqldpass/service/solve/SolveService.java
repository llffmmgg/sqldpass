package com.sqldpass.service.solve;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.solve.SolveAnswerRequest;
import com.sqldpass.controller.solve.SolveRequest;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
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

    public SolveService(SolveRepository solveRepository, QuestionRepository questionRepository,
                        MemberRepository memberRepository, SubjectRepository subjectRepository) {
        this.solveRepository = solveRepository;
        this.questionRepository = questionRepository;
        this.memberRepository = memberRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public Solve solve(Long memberId, SolveRequest request) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        SubjectEntity subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));

        List<Long> questionIds = request.answers().stream()
                .map(SolveAnswerRequest::questionId)
                .toList();

        Map<Long, QuestionEntity> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

        int totalCount = request.answers().size();

        // 먼저 정답 수 계산
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
        SolveEntity solveEntity = new SolveEntity(member, subject, totalCount, correctCount, score);

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

    public Solve getSolve(Long id) {
        return solveRepository.findById(id)
                .map(SolveMapper::toDomain)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SOLVE_NOT_FOUND));
    }
}
