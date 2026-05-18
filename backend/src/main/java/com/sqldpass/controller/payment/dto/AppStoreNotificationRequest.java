package com.sqldpass.controller.payment.dto;

/**
 * App Store Server Notifications V2 페이로드.
 *
 * Apple → 우리 webhook → signedPayload 단일 필드 (JWS).
 * JWS payload 디코딩 후 notificationType / data.signedTransactionInfo 로 분기.
 */
public record AppStoreNotificationRequest(
        String signedPayload
) {}
