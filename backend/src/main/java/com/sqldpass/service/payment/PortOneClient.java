package com.sqldpass.service.payment;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.extern.slf4j.Slf4j;

/**
 * PortOne V2 REST API 클라이언트.
 *
 * 엔드포인트:
 *  - GET  /payments/{paymentId}                  : 결제 단건 조회 (검증용)
 *  - POST /payments/{paymentId}/cancel           : 결제 취소
 *
 * 인증:
 *  - Authorization: PortOne {API_V2_SECRET}
 *
 * 응답에서 사용하는 핵심 필드 (status, amount.total, paidAt, currency, customer 등) 는
 * paymentId 단건 조회 응답 스펙(JSON) 기준. 본 클라이언트는 검증·취소 둘만 노출.
 */
@Slf4j
@Component
public class PortOneClient {

    private final PaymentProperties properties;
    private final RestClient restClient;

    public PortOneClient(PaymentProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getPortone().getApiBaseUrl())
                .build();
    }

    /** PortOne 에서 paymentId 의 결제 정보를 조회. status / amount / paidAt 등을 검증에 사용. */
    public PortOnePaymentInfo getPayment(String paymentId) {
        ensureConfigured();
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "PortOne " + properties.getPortone().getApiSecret())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("PortOne getPayment 실패 status={} paymentId={}", res.getStatusCode(), paymentId);
                        throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                                "PortOne 결제 조회 실패: " + res.getStatusCode());
                    })
                    .body(Map.class);
            return PortOnePaymentInfo.fromResponse(body);
        } catch (SqldpassException e) {
            throw e;
        } catch (Exception e) {
            log.error("PortOne getPayment 호출 중 예외 paymentId={}", paymentId, e);
            throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "PortOne 결제 조회 중 오류: " + e.getMessage());
        }
    }

    /** 결제 취소(환불). 본 PR 범위에선 호출되지 않으나, 운영 시 환불 처리에 사용. */
    public void cancel(String paymentId, String reason) {
        ensureConfigured();
        try {
            restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "PortOne " + properties.getPortone().getApiSecret())
                    .body(Map.of("reason", reason == null ? "" : reason))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                                "PortOne 결제 취소 실패: " + res.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (SqldpassException e) {
            throw e;
        } catch (Exception e) {
            throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "PortOne 결제 취소 중 오류: " + e.getMessage());
        }
    }

    private void ensureConfigured() {
        if (properties.getPortone().getApiSecret().isBlank()) {
            throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "PortOne API 시크릿이 설정되지 않았습니다.");
        }
    }

    /** PortOne 단건 결제 응답에서 우리가 필요한 값만 추려낸 뷰. */
    public record PortOnePaymentInfo(
            String paymentId,
            String status,        // PAID / CANCELLED / FAILED / READY / VIRTUAL_ACCOUNT_ISSUED ...
            int amountTotal,
            String currency,
            OffsetDateTime paidAt,
            Map<String, Object> raw) {

        @SuppressWarnings("unchecked")
        public static PortOnePaymentInfo fromResponse(Map<String, Object> body) {
            if (body == null) {
                throw new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR, "PortOne 응답 본문이 비어있습니다.");
            }
            String paymentId = asString(body.get("id"));
            String status = asString(body.get("status"));
            String currency = asString(body.get("currency"));
            int amountTotal = 0;
            Object amount = body.get("amount");
            if (amount instanceof Map<?, ?> map) {
                Object total = map.get("total");
                if (total instanceof Number n) {
                    amountTotal = n.intValue();
                }
            }
            OffsetDateTime paidAt = null;
            Object paidAtRaw = body.get("paidAt");
            if (paidAtRaw instanceof String s && !s.isBlank()) {
                try {
                    paidAt = OffsetDateTime.parse(s);
                } catch (Exception ignore) {
                    // 형식 다양성 허용 — 검증 단계에서 paidAt 은 옵션 필드
                }
            }
            return new PortOnePaymentInfo(paymentId, status, amountTotal, currency, paidAt,
                    (Map<String, Object>) body);
        }

        private static String asString(Object o) {
            return o == null ? null : o.toString();
        }

        public boolean isPaid() {
            return "PAID".equalsIgnoreCase(status);
        }
    }
}
