package com.sqldpass.service.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 입력입니다."),
    MISSING_HEADER(HttpStatus.BAD_REQUEST, "MISSING_HEADER", "필수 헤더가 누락되었습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "요청 본문을 읽을 수 없습니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND", "과목을 찾을 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "문제를 찾을 수 없습니다."),
    SOLVE_NOT_FOUND(HttpStatus.NOT_FOUND, "SOLVE_NOT_FOUND", "풀이 기록을 찾을 수 없습니다."),
    MOCK_EXAM_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK_EXAM_NOT_FOUND", "모의고사를 찾을 수 없습니다."),
    MOCK_EXAM_LOCKED(HttpStatus.FORBIDDEN, "MOCK_EXAM_LOCKED", "프리미엄 모의고사입니다. 잠금을 해제해야 풀이할 수 있습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_NOT_FOUND", "피드백을 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),

    // 401
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "ADMIN_LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "OAUTH_LOGIN_FAILED", "소셜 로그인에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    // 409
    GENERATION_ALREADY_RUNNING(HttpStatus.CONFLICT, "GENERATION_ALREADY_RUNNING", "이미 문제 생성이 진행 중입니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "NICKNAME_DUPLICATE", "이미 사용 중인 닉네임입니다."),
    MOCK_EXAM_INSUFFICIENT_QUESTIONS(HttpStatus.CONFLICT, "MOCK_EXAM_INSUFFICIENT_QUESTIONS", "문제가 부족하여 모의고사를 생성할 수 없습니다."),

    // 429
    ANONYMOUS_SOLVE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "ANONYMOUS_SOLVE_LIMIT_EXCEEDED", "오늘의 무료 풀이 한도를 모두 사용했어요. 가입하면 무제한으로 풀 수 있어요."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_GENERATION_FAILED", "AI 문제 생성에 실패했습니다."),
    AI_VERIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_VERIFICATION_FAILED", "AI 문제 검증에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
