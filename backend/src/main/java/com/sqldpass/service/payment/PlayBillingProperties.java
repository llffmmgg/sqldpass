package com.sqldpass.service.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sqldpass.persistent.payment.SubscriptionPlan;

import lombok.Getter;
import lombok.Setter;

/**
 * Google Play Billing 설정.
 * - {@code packageName}: 앱 ID (Capacitor 의 com.sqldpass.app 과 동일)
 * - {@code serviceAccountJsonPath}: Play Console 에서 발급한 서비스 계정 JSON 키 경로.
 *   미설정이면 PlayBillingClient 가 fail-fast (검증 호출 시 IllegalStateException).
 * - {@code productIdMapping}: Play Console 에 등록한 상품 ID(SKU) ↔ 우리 SubscriptionPlan 매핑.
 *   클라이언트가 보낸 productId 가 plan 과 합치하는지 검증할 때 사용한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sqldpass.play-billing")
public class PlayBillingProperties {

    private String packageName = "com.sqldpass.app";
    private String serviceAccountJsonPath = "";
    private Map<SubscriptionPlan, String> productIdMapping = new HashMap<>();

    /** SubscriptionPlan 으로 productId 조회 — 매핑 누락 시 null 반환. */
    public String productIdFor(SubscriptionPlan plan) {
        return productIdMapping.get(plan);
    }

    /** productId 로 역매핑된 plan 조회 — 매핑 없거나 일치 없으면 null. */
    public SubscriptionPlan planFor(String productId) {
        if (productId == null) return null;
        for (Map.Entry<SubscriptionPlan, String> entry : productIdMapping.entrySet()) {
            if (productId.equals(entry.getValue())) return entry.getKey();
        }
        return null;
    }
}
