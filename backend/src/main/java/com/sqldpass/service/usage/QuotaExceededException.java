package com.sqldpass.service.usage;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * 무료 회원 일일 한도 초과 시 throw.
 * Step 4 의 ControllerAdvice 에서 HTTP 402 Payment Required + 구조화 body 로 변환된다.
 *
 * {@code code} 는 {@code "DAILY_QUESTION_LIMIT"} 또는 {@code "DAILY_MOCK_LIMIT"} 둘 중 하나로
 * 클라이언트의 모달 분기 키로 쓰인다.
 */
@Getter
public class QuotaExceededException extends RuntimeException {

    private final String code;
    private final int used;
    private final int limit;
    private final LocalDateTime resetAt;

    public QuotaExceededException(String code, int used, int limit, LocalDateTime resetAt) {
        super(code);
        this.code = code;
        this.used = used;
        this.limit = limit;
        this.resetAt = resetAt;
    }
}
