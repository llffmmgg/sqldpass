package com.sqldpass.service.payment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Apple App Store Server Notifications V2 JWS 검증기.
 *
 * <p>Apple 공식 라이브러리 {@code com.apple.itunes:app-store-server-library} 의
 * {@link SignedDataVerifier} 를 래핑한다. 검증 항목:
 * <ul>
 *   <li>JWS 서명 (ES256) — x5c 체인을 Apple Root CA(G3) 까지 추적</li>
 *   <li>bundleId — 우리 앱 Bundle ID 와 일치 여부</li>
 *   <li>environment — SANDBOX/PRODUCTION 일치 여부 (XCODE/LOCAL_TESTING 은 서명 검증 스킵 — 운영 금지)</li>
 *   <li>appAppleId — PRODUCTION 한정. SANDBOX 에서는 검사 안 함</li>
 * </ul>
 *
 * <p>실패 시 {@link AppStorePayloadVerificationException} 으로 감싸서 throw — webhook 컨트롤러가
 * 401 로 변환한다.
 *
 * <p>Apple Root CA 는 {@code src/main/resources/apple-roots/} 에 DER 형식으로 동봉
 * (Apple PKI 공식 배포본). 라이브러리는 자체로 root CA 를 번들하지 않으므로 직접 제공해야 한다.
 */
@Slf4j
@Service
public class AppStorePayloadVerifier {

    /** Apple PKI 공식 배포 G3 root cert (DER). ASSN v2 JWS 체인의 종착점. */
    private static final String ROOT_CA_RESOURCE = "apple-roots/AppleRootCA-G3.cer";

    private final SignedDataVerifier verifier;
    private final Environment environment;
    private final String bundleId;
    private final Long appAppleId;

    public AppStorePayloadVerifier(
            @Value("${app-store.bundle-id:com.sqldpass.app}") String bundleId,
            @Value("${app-store.app-apple-id:0}") long appAppleId,
            @Value("${app-store.environment:SANDBOX}") String environmentName) {
        this.bundleId = bundleId;
        this.appAppleId = appAppleId;
        this.environment = parseEnvironment(environmentName);

        Set<InputStream> rootCertificates = loadRootCertificates();
        // PRODUCTION 환경 + appAppleId 미설정 시 라이브러리가 IllegalArgumentException 을 던진다.
        // 그래도 운영자가 알아차리도록 명시적으로 로그.
        Long appAppleIdParam = appAppleId == 0L ? null : appAppleId;
        if (this.environment == Environment.PRODUCTION && appAppleIdParam == null) {
            throw new IllegalStateException(
                    "app-store.app-apple-id 는 PRODUCTION 환경에서 필수입니다. " +
                            "App Store Connect 의 numeric app id 를 환경변수 APP_STORE_APP_APPLE_ID 로 주입하세요.");
        }
        this.verifier = new SignedDataVerifier(
                rootCertificates,
                bundleId,
                appAppleIdParam,
                this.environment,
                /* enableOnlineChecks = */ false);
    }

    @PostConstruct
    void logConfig() {
        log.info("AppStorePayloadVerifier 초기화 — bundleId={} environment={} appAppleId={}",
                bundleId, environment, appAppleId == 0L ? "미설정(WARN: PRODUCTION 에서는 필수)" : appAppleId);
        if (appAppleId == 0L) {
            log.warn("app-store.app-apple-id 가 0(미설정) — SANDBOX/XCODE 가 아닌 환경에서는 검증이 실패합니다.");
        }
    }

    /**
     * ASSN v2 signedPayload(JWS) 의 서명 + 페이로드를 검증·디코딩.
     *
     * @throws AppStorePayloadVerificationException 서명·체인·bundleId·환경 검증 실패 시
     */
    public ResponseBodyV2DecodedPayload verifyAndDecodeNotification(String signedPayload) {
        if (signedPayload == null || signedPayload.isBlank()) {
            throw new AppStorePayloadVerificationException("signedPayload 가 비어있습니다.");
        }
        try {
            return verifier.verifyAndDecodeNotification(signedPayload);
        } catch (VerificationException e) {
            throw new AppStorePayloadVerificationException(
                    "ASSN v2 notification 검증 실패: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new AppStorePayloadVerificationException(
                    "ASSN v2 notification 디코딩 중 예외", e);
        }
    }

    /**
     * data.signedTransactionInfo (JWS) 의 서명 + 페이로드를 검증·디코딩.
     *
     * @throws AppStorePayloadVerificationException 서명·체인·bundleId 검증 실패 시
     */
    public JWSTransactionDecodedPayload verifyAndDecodeTransaction(String signedTransactionInfo) {
        if (signedTransactionInfo == null || signedTransactionInfo.isBlank()) {
            throw new AppStorePayloadVerificationException("signedTransactionInfo 가 비어있습니다.");
        }
        try {
            return verifier.verifyAndDecodeTransaction(signedTransactionInfo);
        } catch (VerificationException e) {
            throw new AppStorePayloadVerificationException(
                    "ASSN v2 transaction 검증 실패: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new AppStorePayloadVerificationException(
                    "ASSN v2 transaction 디코딩 중 예외", e);
        }
    }

    private Environment parseEnvironment(String value) {
        if (value == null || value.isBlank()) {
            return Environment.SANDBOX;
        }
        try {
            return Environment.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("app-store.environment 값이 유효하지 않음 — SANDBOX 로 fallback: {}", value);
            return Environment.SANDBOX;
        }
    }

    private Set<InputStream> loadRootCertificates() {
        ClassPathResource resource = new ClassPathResource(ROOT_CA_RESOURCE);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Apple Root CA 리소스를 찾지 못했습니다: " + ROOT_CA_RESOURCE +
                            ". src/main/resources/apple-roots/AppleRootCA-G3.cer 동봉 여부 확인.");
        }
        try (InputStream in = resource.getInputStream()) {
            // SignedDataVerifier 생성자는 InputStream 들을 즉시 읽어 X.509 객체로 파싱하므로
            // 클래스패스 stream 을 메모리 buffer 로 복제할 필요가 없다 — 그러나 안전을 위해 복제.
            byte[] bytes = in.readAllBytes();
            return Set.of(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IllegalStateException("Apple Root CA 로딩 실패: " + ROOT_CA_RESOURCE, e);
        }
    }
}
