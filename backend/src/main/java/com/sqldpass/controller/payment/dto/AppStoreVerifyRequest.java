package com.sqldpass.controller.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * iOS StoreKit 2 영수증 검증 요청.
 *
 * @param signedTransaction JWS 형식의 signedTransactionInfo
 * @param productId         사용자가 구매한 상품 ID (클라이언트가 보내는 hint, 백엔드가 payload 와 비교)
 */
public record AppStoreVerifyRequest(
        @NotBlank String signedTransaction,
        @NotBlank String productId
) {}
