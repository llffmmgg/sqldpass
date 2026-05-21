package com.sqldpass.controller.common;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MultipartException;

import com.sqldpass.controller.usage.dto.QuotaExceededResponse;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.usage.QuotaExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final DiscordNotifier discordNotifier;

    /**
     * 무료 일일 한도 초과 — HTTP 402 Payment Required.
     * 응답 body 의 code(DAILY_QUESTION_LIMIT|DAILY_MOCK_LIMIT) 가 클라이언트 모달 분기 키.
     * resetAt 은 KST naive (project_kst_naive_serialization 메모리).
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<QuotaExceededResponse> handleQuotaExceeded(QuotaExceededException e) {
        QuotaExceededResponse body = new QuotaExceededResponse(
                e.getCode(), e.getUsed(), e.getLimit(), e.getResetAt());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    @ExceptionHandler(SqldpassException.class)
    public ResponseEntity<ErrorResponse> handleSqldpassException(SqldpassException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        // 5xx 서버 에러는 Discord 알림 대상 (4xx 클라이언트 에러는 제외)
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("SqldpassException (5xx) at {}", request.getRequestURI(), e);
            discordNotifier.notifyException(e, request.getRequestURI());
        }
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), e.getMessage(), errorCode.getCode());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), e.getMessage(), errorCode.getCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        ErrorCode errorCode = ErrorCode.MISSING_HEADER;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), e.getMessage(), errorCode.getCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException e) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncDisconnect(AsyncRequestNotUsableException e) {
        log.debug("클라이언트 연결 종료됨 (SSE disconnect)");
    }

    // 스캐너 봇이 보내는 잘못된 multipart 요청 — 500이 아니라 400으로 정정
    // Discord 알림은 DiscordNotifier에서 필터링됨
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException e) {
        log.debug("Malformed multipart request: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        // Discord 알림 — NoResourceFoundException 등 노이즈는 notifier 내부에서 필터링
        discordNotifier.notifyException(e, request.getRequestURI());
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
