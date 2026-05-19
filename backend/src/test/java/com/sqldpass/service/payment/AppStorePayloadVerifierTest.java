package com.sqldpass.service.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AppStorePayloadVerifier} 단위 테스트.
 *
 * <p>실제 Apple-signed JWS 페이로드 검증은 운영 KMS·StoreKit2 sandbox 흐름이 필요해서 단위테스트
 * 환경에서는 재현 불가. 라이브러리 동작은 Apple 측 책임이고, 본 서비스가 라이브러리를 올바르게
 * 래핑하는지는 {@code PaymentWebhookControllerTest} 의 mock 시나리오로 검증한다.
 *
 * <p>여기서는 두 가지 sanity check 만 한다:
 * <ol>
 *   <li>SANDBOX + appAppleId=0 — 생성자 성공 + blank input reject</li>
 *   <li>PRODUCTION + appAppleId=0 — Bean 자체는 생성 성공(2026-05-19 다운타임 사고 근본 해결),
 *       단 verify 호출 시점에 {@link AppStorePayloadVerificationException} throw</li>
 * </ol>
 *
 * <p>Apple Root CA G3 가 resources/apple-roots/ 에 동봉돼 있어 SignedDataVerifier 생성이 성공해야 한다.
 */
class AppStorePayloadVerifierTest {

    @Test
    void sandbox_with_zero_apple_id_initializes_and_rejects_blank_input() {
        AppStorePayloadVerifier verifier = new AppStorePayloadVerifier(
                "com.sqldpass.app", 0L, "SANDBOX");
        assertNotNull(verifier);

        assertThrows(AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeNotification(""));
        assertThrows(AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeTransaction(null));
    }

    /**
     * 2026-05-19 운영 다운타임 사고의 근본 해결 검증:
     *
     * <p>이전 동작: 본 조건에서 생성자가 {@code IllegalStateException} throw → Bean 생성 실패
     * → Spring {@code ApplicationContext.refresh()} 실패 → backend 전체 startup 실패 → 다운타임.
     *
     * <p>현재 동작: Bean 정상 생성 + WARN 로그 + misconfigured 플래그. verify 호출 시점에만
     * {@link AppStorePayloadVerificationException} throw → {@code PaymentWebhookController} 가
     * 401 로 변환 → 결제 webhook 만 거부되고 backend 의 다른 기능은 정상 가동.
     */
    @Test
    void production_with_zero_apple_id_initializes_then_rejects_at_verify_time() {
        AppStorePayloadVerifier verifier = new AppStorePayloadVerifier(
                "com.sqldpass.app", 0L, "PRODUCTION");
        // Bean 자체는 정상 생성 — 이게 본 사고의 근본 해결 핵심
        assertNotNull(verifier);

        // verify 호출 시점에만 거부
        AppStorePayloadVerificationException ex1 = assertThrows(
                AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeNotification("not-a-real-jws-but-non-blank"));
        assertTrue(ex1.getMessage().contains("비활성화") || ex1.getMessage().contains("APP_STORE_APP_APPLE_ID"),
                "misconfigured 안내 메시지가 사용자/운영자에게 명확해야 함: " + ex1.getMessage());

        AppStorePayloadVerificationException ex2 = assertThrows(
                AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeTransaction("not-a-real-jws-but-non-blank"));
        assertNotNull(ex2);
    }
}
