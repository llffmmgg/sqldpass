package com.sqldpass.service.payment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * sqldpass.payment.* 설정 바인딩.
 *
 *   sqldpass.payment.portone.store-id
 *   sqldpass.payment.portone.api-secret
 *   sqldpass.payment.reviewer-nicknames     (콤마 구분 닉네임)
 *   sqldpass.payment.default-amount         (원, ≥ 1)
 *   sqldpass.payment.default-product-name   (TEST 금지, 비어있지 않을 것)
 */
@Configuration
@ConfigurationProperties(prefix = "sqldpass.payment")
public class PaymentProperties {

    private PortOne portone = new PortOne();
    private String reviewerNicknames = "";
    private int defaultAmount = 3000;
    private String defaultProductName = "문어CBT 프리미엄 모의고사 1회차 잠금 해제";

    public PortOne getPortone() {
        return portone;
    }

    public void setPortone(PortOne portone) {
        this.portone = portone;
    }

    public String getReviewerNicknames() {
        return reviewerNicknames;
    }

    public void setReviewerNicknames(String reviewerNicknames) {
        this.reviewerNicknames = reviewerNicknames == null ? "" : reviewerNicknames;
    }

    public int getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(int defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public String getDefaultProductName() {
        return defaultProductName;
    }

    public void setDefaultProductName(String defaultProductName) {
        this.defaultProductName = defaultProductName;
    }

    /** 화이트리스트 닉네임 Set. 빈 문자열·공백은 제거. 대소문자 구분 유지. */
    public Set<String> reviewerNicknameSet() {
        if (reviewerNicknames == null || reviewerNicknames.isBlank()) {
            return Set.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String s : reviewerNicknames.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    public static class PortOne {
        private String storeId = "";
        private String apiSecret = "";
        private String apiBaseUrl = "https://api.portone.io";

        public String getStoreId() {
            return storeId;
        }

        public void setStoreId(String storeId) {
            this.storeId = storeId == null ? "" : storeId;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret == null ? "" : apiSecret;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }
}
