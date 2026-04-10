package com.sqldpass.service.solve;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final GradingService gradingService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SolveService(SolveRepository solveRepository, QuestionRepository questionRepository,
                        MemberRepository memberRepository, SubjectRepository subjectRepository,
                        MockExamRepository mockExamRepository, GradingService gradingService) {
        this.solveRepository = solveRepository;
        this.questionRepository = questionRepository;
        this.memberRepository = memberRepository;
        this.subjectRepository = subjectRepository;
        this.mockExamRepository = mockExamRepository;
        this.gradingService = gradingService;
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

        // 모든 문제 존재 여부 확인
        for (SolveAnswerRequest answerReq : request.answers()) {
            if (!questionMap.containsKey(answerReq.questionId())) {
                throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND, "문제를 찾을 수 없습니다. id=" + answerReq.questionId());
            }
        }

        // 채점 — GradingService가 MCQ/SHORT_ANSWER/DESCRIPTIVE 분기
        List<GradingService.GradingResult> results = request.answers().stream()
                .map(req -> gradingService.grade(
                        questionMap.get(req.questionId()),
                        req.selectedOption(),
                        req.answerText()))
                .toList();

        // 점수 집계 — 부분점수 합산 (서술형 고려)
        double totalScore = results.stream().mapToDouble(GradingService.GradingResult::score).sum();
        int correctCount = (int) results.stream().filter(GradingService.GradingResult::correct).count();
        int score = totalCount > 0 ? (int) Math.round(totalScore / totalCount * 100) : 0;

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

        // 답안별 엔티티 생성
        for (int i = 0; i < request.answers().size(); i++) {
            SolveAnswerRequest req = request.answers().get(i);
            QuestionEntity question = questionMap.get(req.questionId());
            GradingService.GradingResult result = results.get(i);

            SolveAnswerEntity answerEntity;
            if (question.getQuestionType() == null || question.getQuestionType().name().equals("MCQ")) {
                answerEntity = new SolveAnswerEntity(
                        solveEntity, question,
                        req.selectedOption() != null ? req.selectedOption() : 0,
                        question.getCorrectOption() != null ? question.getCorrectOption() : 0,
                        result.correct());
            } else {
                String matchedJson;
                try {
                    matchedJson = OBJECT_MAPPER.writeValueAsString(result.matchedKeywords());
                } catch (Exception e) {
                    matchedJson = "[]";
                }
                answerEntity = new SolveAnswerEntity(
                        solveEntity, question,
                        req.answerText(),
                        matchedJson,
                        result.score(),
                        result.correct());
            }
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

    /** 어드민용 — 멤버 제한 없이 풀이 엔티티 조회 (문제 내용 포함) */
    public SolveEntity getSolveEntityForAdmin(Long id) {
        return solveRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SOLVE_NOT_FOUND));
    }
}
