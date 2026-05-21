package com.sqldpass.controller.usage.dto;

import java.time.LocalDateTime;

/**
 * 무료 일일 한도 초과 응답 body (HTTP 402 Payment Required).
 *
 * resetAt 은 KST naive ISO 형식으로 직렬화 — 프론트가 +09:00 부착 (메모리 project_kst_naive_serialization).
 */
public record QuotaExceededResponse(
        String error,
        int used,
        int limit,
        LocalDateTime resetAt
) {}
