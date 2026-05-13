package com.sqldpass.service.wronganswer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.solve.dto.SolveAnswerRequest;
import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerPreviewResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerRetryResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveAnswerRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.solve.SolveService;

@Service
@Transactional(readOnly = true)
public class WrongAnswerService {

    private final SolveAnswerRepository solveAnswerRepository;
    private final QuestionRepository questionRepository;
    private final SolveService solveService;
    private final SubscriptionService subscriptionService;

    public WrongAnswerService(SolveAnswerRepository solveAnswerRepository,
                              QuestionRepository questionRepository,
                              SolveService solveService,
                              SubscriptionService subscriptionService) {
        this.solveAnswerRepository = solveAnswerRepository;
        this.questionRepository = questionRepository;
        this.solveService = solveService;
        this.subscriptionService = subscriptionService;
    }

    public List<WrongAnswerResponse> getWrongAnswers(Long memberId, Long subjectId) {
        requireLibraryAccess(memberId);
        return solveAnswerRepository.findWrongAnswers(memberId, subjectId).stream()
                .map(WrongAnswerResponse::from)
                .toList();
    }

    public List<WrongAnswerStatsResponse> getStats(Long memberId) {
        requireLibraryAccess(memberId);
        return solveAnswerRepository.findWrongAnswerStats(memberId).stream()
                .map(WrongAnswerStatsResponse::from)
                .toList();
    }

    /**
     * 오답노트 잠금 화면 미리보기 — 권한 가드 없이 본인 오답 상위 limit 개를 제목·과목만 반환.
     * 정답/해설은 응답에서 제외. memberId == null (비로그인) 이면 빈 리스트.
     */
    public List<WrongAnswerPreviewResponse> getPreview(Long memberId, int limit) {
        if (memberId == null) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return solveAnswerRepository.findWrongAnswers(memberId, null).stream()
                .limit(safeLimit)
                .map(WrongAnswerPreviewResponse::from)
                .toList();
    }

    /**
     * 오답 다시 풀기 — 단일 문제 답안 제출.
     *
     * 내부 동작:
     * 1. 문제의 subjectId 조회
     * 2. 1문제짜리 SolveRequest 빌드
     * 3. SolveService.solve() 위임 → SolveAnswerEntity 1건 저장
     * 4. 결과(정답 여부 + 정답/해설) 반환
     *
     * 정답이면 다음 오답노트 조회 시 자동으로 목록에서 사라짐 (마스터 완료).
     */
    @Transactional
    public WrongAnswerRetryResponse retry(Long memberId, Long questionId, Integer selectedOption, String answerText) {
        requireLibraryAccess(memberId);
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));

        SolveAnswerRequest answerReq = new SolveAnswerRequest(questionId, selectedOption, answerText);
        SolveRequest solveReq = new SolveRequest(question.getSubject().getId(), null, List.of(answerReq));
        Solve solve = solveService.solve(memberId, solveReq).solve();

        boolean correct = solve.getCorrectCount() > 0;
        return new WrongAnswerRetryResponse(
                correct,
                question.getCorrectOption(),
                question.getAnswer(),
                question.getExplanation()
        );
    }

    private void requireLibraryAccess(Long memberId) {
        if (!subscriptionService.hasLibraryAccess(memberId)) {
            throw new SqldpassException(ErrorCode.WRONG_ANSWER_REQUIRES_SUBSCRIPTION);
        }
    }
}
