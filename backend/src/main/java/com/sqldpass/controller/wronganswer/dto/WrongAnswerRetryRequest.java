package com.sqldpass.controller.wronganswer.dto;

/**
 * 오답 다시 풀기 요청.
 * - MCQ: selectedOption (1~4)
 * - SHORT_ANSWER/DESCRIPTIVE: answerText
 * 둘 중 questionType에 맞는 것 하나만 채워서 보내면 됨.
 */
public record WrongAnswerRetryRequest(
        Integer selectedOption,
        String answerText
) {
}
