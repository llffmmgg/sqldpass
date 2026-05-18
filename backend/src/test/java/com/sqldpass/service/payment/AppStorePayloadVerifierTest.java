package com.sqldpass.service.payment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AppStorePayloadVerifier} smoke test.
 *
 * <p>실제 Apple 서명을 받은 JWS 페이로드는 운영 KMS·StoreKit2 sandbox 흐름이 필요해서 단위테스트
 * 환경에서는 재현 불가. 라이브러리 동작은 Apple 측 책임이고, 본 서비스가 라이브러리를 올바르게
 * 래핑하는지는 {@code PaymentWebhookControllerTest} 의 mock 시나리오로 검증한다.
 *
 * <p>여기서는 인스턴스 생성 가능성과 입력 sanitization (빈 문자열 reject) 만 sanity check 한다.
 * Apple Root CA G3 가 resources/apple-roots/ 에 동봉돼 있어 SignedDataVerifier 생성이 성공해야 한다.
 */
@Disabled("실제 Apple-signed JWS 가 필요해 CI 에서 의미 있는 검증 불가. 인스턴스화는 PaymentWebhookControllerTest 에서 mock 으로 대체.")
class AppStorePayloadVerifierTest {

    @Test
    void instantiate_sandbox_then_reject_blank_input() {
        AppStorePayloadVerifier verifier = new AppStorePayloadVerifier(
                "com.sqldpass.app", 0L, "SANDBOX");
        assertNotNull(verifier);

        assertThrows(AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeNotification(""));
        assertThrows(AppStorePayloadVerificationException.class,
                () -> verifier.verifyAndDecodeTransaction(null));
    }
}
