package com.sqldpass.controller.wronganswer.dto;

/**
 * 오답 다시 풀기 응답.
 *
 * - correct: 정답 여부
 * - correctOption: MCQ 정답 번호 (단답형은 null)
 * - correctAnswer: 단답/약술형 모범답안 (MCQ는 null)
 * - explanation: 해설
 *
 * 정답이면 다음 오답노트 조회 시 자동으로 목록에서 사라짐 (마스터 완료).
 */
public record WrongAnswerRetryResponse(
        boolean correct,
        Integer correctOption,
        String correctAnswer,
        String explanation
) {
}
