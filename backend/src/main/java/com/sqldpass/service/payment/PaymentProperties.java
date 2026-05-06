package com.sqldpass.service.payment;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sqldpass.persistent.payment.SubscriptionPlan;

/**
 * sqldpass.payment.* 설정 바인딩.
 *
 *   sqldpass.payment.portone.store-id
 *   sqldpass.payment.portone.api-secret
 *   sqldpass.payment.reviewer-nicknames        (콤마 구분 닉네임)
 *   sqldpass.payment.three-day.amount/product-name
 *   sqldpass.payment.one-month.amount/product-name
 *   sqldpass.payment.unlimited.amount/product-name
 */
@Configuration
@ConfigurationProperties(prefix = "sqldpass.payment")
public class PaymentProperties {

    private PortOne portone = new PortOne();
    private String reviewerNicknames = "";
    private PlanConfig threeDay = new PlanConfig(3900, "문어CBT 3일 이용권");
    private PlanConfig oneMonth = new PlanConfig(9900, "문어CBT 한달 이용권");
    private PlanConfig unlimited = new PlanConfig(29900, "문어CBT 평생 무제한 이용권");

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

    public PlanConfig getThreeDay() {
        return threeDay;
    }

    public void setThreeDay(PlanConfig threeDay) {
        this.threeDay = threeDay;
    }

    public PlanConfig getOneMonth() {
        return oneMonth;
    }

    public void setOneMonth(PlanConfig oneMonth) {
        this.oneMonth = oneMonth;
    }

    public PlanConfig getUnlimited() {
        return unlimited;
    }

    public void setUnlimited(PlanConfig unlimited) {
        this.unlimited = unlimited;
    }

    /** 주어진 plan 의 가격·상품명 설정 반환. */
    public PlanConfig configFor(SubscriptionPlan plan) {
        return switch (plan) {
            case THREE_DAY -> threeDay;
            case ONE_MONTH -> oneMonth;
            case UNLIMITED -> unlimited;
        };
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

    public static class PlanConfig {
        private int amount;
        private String productName;

        public PlanConfig() {}

        public PlanConfig(int amount, String productName) {
            this.amount = amount;
            this.productName = productName;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
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
