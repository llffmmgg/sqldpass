package com.sqldpass.service.payment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Play Developer API 클라이언트 — 안드로이드 앱 인앱 결제 영수증을 검증하고 acknowledge.
 *
 * <p>Play Console 에서 일회성 상품(In-app product)으로 등록한 SKU 를 클라이언트가 구매하면 받는
 * purchaseToken 을 검증한다. purchaseState=0(PURCHASED) + acknowledgementState 확인 후, 3일 내
 * acknowledge 하지 않으면 자동 환불되므로 검증 직후 ack 호출 필수.
 */
@Slf4j
@Component
@Configuration
@EnableConfigurationProperties(PlayBillingProperties.class)
public class PlayBillingClient {

    private static final String APPLICATION_NAME = "sqldpass-backend";
    private static final List<String> SCOPES =
            Collections.singletonList(AndroidPublisherScopes.ANDROIDPUBLISHER);

    private final PlayBillingProperties properties;
    private volatile AndroidPublisher cached;

    public PlayBillingClient(PlayBillingProperties properties,
                             @Value("${sqldpass.play-billing.service-account-json-path:}") String saPath) {
        this.properties = properties;
        // saPath 는 PlayBillingProperties.serviceAccountJsonPath 로도 들어와 있지만, 명시적 주입을
        // 한 번 더 받아 부팅 시점 누락을 빠르게 검출 (선택적 — 미설정이어도 호출 시점에만 fail).
        if (saPath != null && !saPath.isBlank()
                && (properties.getServiceAccountJsonPath() == null || properties.getServiceAccountJsonPath().isBlank())) {
            properties.setServiceAccountJsonPath(saPath);
        }
    }

    /**
     * 인앱 일회성 상품 구매 영수증 검증.
     * 호출 후 결과의 acknowledgementState 가 0 이면 acknowledge() 를 곧바로 호출해야 한다.
     */
    public PlayPurchaseInfo verifyProduct(String productId, String purchaseToken) {
        if (productId == null || productId.isBlank() || purchaseToken == null || purchaseToken.isBlank()) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "productId/purchaseToken 가 비어 있습니다.");
        }
        try {
            ProductPurchase purchase = publisher().purchases().products()
                    .get(properties.getPackageName(), productId, purchaseToken)
                    .execute();
            return PlayPurchaseInfo.from(purchase);
        } catch (IOException e) {
            log.error("Play Billing 검증 실패 productId={} token={}", productId, mask(purchaseToken), e);
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    /**
     * acknowledge — 구매 후 3일 내 미호출 시 Google 이 자동 환불한다.
     */
    public void acknowledge(String productId, String purchaseToken) {
        try {
            publisher().purchases().products()
                    .acknowledge(properties.getPackageName(), productId, purchaseToken, null)
                    .execute();
        } catch (IOException e) {
            log.error("Play Billing acknowledge 실패 productId={} token={}",
                    productId, mask(purchaseToken), e);
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    private AndroidPublisher publisher() {
        AndroidPublisher local = this.cached;
        if (local != null) return local;
        synchronized (this) {
            if (this.cached != null) return this.cached;
            String saPath = properties.getServiceAccountJsonPath();
            if (saPath == null || saPath.isBlank()) {
                throw new IllegalStateException(
                        "sqldpass.play-billing.service-account-json-path 가 설정되지 않았습니다. " +
                        "Play Console 에서 발급한 서비스 계정 JSON 경로를 application.yaml 또는 환경변수로 주입해주세요.");
            }
            try (InputStream in = new FileInputStream(saPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(SCOPES);
                HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
                this.cached = new AndroidPublisher.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        requestInitializer)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                return this.cached;
            } catch (Exception e) {
                throw new IllegalStateException("Play Billing API 클라이언트 초기화 실패", e);
            }
        }
    }

    private static String mask(String token) {
        if (token == null) return "null";
        return token.length() <= 8 ? "***" : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * 검증 결과 요약 — purchaseState/acknowledgementState/구매 시각 등 후속 처리에 필요한 필드만 추출.
     * Google purchaseState: 0=PURCHASED, 1=CANCELED, 2=PENDING
     * acknowledgementState: 0=NOT_ACKNOWLEDGED, 1=ACKNOWLEDGED
     */
    public record PlayPurchaseInfo(
            int purchaseState,
            int acknowledgementState,
            Long purchaseTimeMillis,
            String orderId,
            String regionCode) {

        public static PlayPurchaseInfo from(ProductPurchase p) {
            return new PlayPurchaseInfo(
                    p.getPurchaseState() != null ? p.getPurchaseState() : -1,
                    p.getAcknowledgementState() != null ? p.getAcknowledgementState() : -1,
                    p.getPurchaseTimeMillis(),
                    p.getOrderId(),
                    p.getRegionCode());
        }

        public boolean isPurchased() {
            return purchaseState == 0;
        }

        public boolean needsAcknowledge() {
            return acknowledgementState == 0;
        }

        public Instant purchasedAtInstant() {
            return purchaseTimeMillis != null ? Instant.ofEpochMilli(purchaseTimeMillis) : Instant.now();
        }
    }
}
