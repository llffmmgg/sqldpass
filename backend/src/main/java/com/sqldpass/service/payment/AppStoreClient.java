package com.sqldpass.service.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Apple StoreKit 2 signedTransaction(JWS) 처리.
 *
 * <p>1차 구현(본 phase): JWS payload 만 base64URL 디코딩 → JSON 파싱 → productId/transactionId 추출.
 * 정식 출시 직전 별도 phase 에서 Apple Root CA 체인으로 RSA 서명 검증 + App Store Server API
 * GET /inApps/v1/transactions/{id} 교차 확인 추가 예정.</p>
 *
 * <p>현 단계 보호:
 * 1) productId 화이트리스트 ({@code PaymentService} 에서 app-store-product-id-mapping 확인)
 * 2) PaymentService.verifyAppStore 의 idempotent 체크 (같은 transactionId 재요청 시 기존 결제 반환)
 * </p>
 */
@Slf4j
@Component
public class AppStoreClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWS signedTransaction 의 payload 만 추출. 서명 검증 없음(1차).
     */
    public TransactionInfo parsePayload(String signedTransaction) {
        if (signedTransaction == null || signedTransaction.isBlank()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "signedTransaction 은 필수입니다.");
        }
        String[] parts = signedTransaction.split("\\.");
        if (parts.length != 3) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "JWS 형식이 아닙니다.");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            return new TransactionInfo(
                    payload.path("transactionId").asText(),
                    payload.path("originalTransactionId").asText(),
                    payload.path("productId").asText(),
                    payload.path("bundleId").asText(),
                    payload.path("purchaseDate").asLong(0),
                    payload.path("expiresDate").asLong(0));
        } catch (Exception e) {
            log.warn("App Store JWS payload 파싱 실패", e);
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "App Store JWS 파싱 실패");
        }
    }

    public record TransactionInfo(
            String transactionId,
            String originalTransactionId,
            String productId,
            String bundleId,
            long purchaseDate,
            long expiresDate
    ) {}
}
