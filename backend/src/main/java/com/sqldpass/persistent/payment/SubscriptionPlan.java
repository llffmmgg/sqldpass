package com.sqldpass.persistent.payment;

/**
 * 구독 플랜.
 *
 * - 모든 plan 은 PREMIUM 풀이를 허용한다 (allowsPremium = true 고정).
 * - days = null 이면 평생 (UNLIMITED).
 * - removesAds, allowsPdf 는 plan 별 차등.
 */
public enum SubscriptionPlan {
    THREE_DAY(3, false, false),
    ONE_MONTH(30, true, false),
    UNLIMITED(null, true, true);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
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
