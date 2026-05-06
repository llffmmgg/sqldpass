package com.sqldpass.persistent.payment;

/**
 * 구독 플랜.
 *
 * - 모든 plan 은 PREMIUM 풀이를 허용한다 (allowsPremium = true 고정).
 * - days = null 이면 평생 (UNLIMITED).
 * - removesAds, allowsPdf 는 plan 별 차등.
 */
public enum SubscriptionPlan {
    THREE_DAY(3, false, false, 1),
    ONE_MONTH(30, true, false, 2),
    UNLIMITED(null, true, true, 3);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;
    /** 업그레이드 비교용 강도. 큰 값일수록 강한 plan. */
    private final int tier;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf, int tier) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    /** 본 plan 이 currentPlan 의 업그레이드인지. 같은 plan 또는 약한 plan 은 false. */
    public boolean isUpgradeFrom(SubscriptionPlan currentPlan) {
        return currentPlan != null && this.tier > currentPlan.tier;
    }

    public Integer getDays() {
        return days;
    }

    public boolean isRemovesAds() {
        return removesAds;
    }

    public boolean isAllowsPdf() {
        return allowsPdf;
    }

    /** UNLIMITED 면 true. */
    public boolean isLifetime() {
        return days == null;
    }
}
