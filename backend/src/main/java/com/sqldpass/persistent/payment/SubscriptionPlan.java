package com.sqldpass.persistent.payment;

/**
 * 구독 플랜.
 *
 * - 모든 plan 은 PREMIUM 풀이를 허용한다 (allowsPremium = true 고정).
 * - days = null 이면 평생 (UNLIMITED).
 * - removesAds, allowsPdf, hasLibraryAccess 는 plan 별 차등.
 *
 * enum 키 (THREE_DAY 등) 는 DB `subscription.plan` 컬럼에 enum name 으로 저장되므로
 * 절대 변경 금지. 사용자 노출 라벨은 프론트에서 매핑 (THREE_DAY → "Thunder").
 */
public enum SubscriptionPlan {
    THREE_DAY(3, true, false, true, 1),
    FOCUS(30, true, false, true, 2),
    ONE_MONTH(30, true, false, true, 3),
    UNLIMITED(null, true, true, true, 4);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;
    /** 오답노트 사용 + 즐겨찾기 무제한 — 무료 외 모든 plan 에서 true. */
    private final boolean hasLibraryAccess;
    /** 업그레이드 비교용 강도. 큰 값일수록 강한 plan. */
    private final int tier;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf,
                     boolean hasLibraryAccess, int tier) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
        this.hasLibraryAccess = hasLibraryAccess;
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

    public boolean isHasLibraryAccess() {
        return hasLibraryAccess;
    }

    /** UNLIMITED 면 true. */
    public boolean isLifetime() {
        return days == null;
    }
}
