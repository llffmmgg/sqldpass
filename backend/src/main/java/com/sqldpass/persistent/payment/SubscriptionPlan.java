package com.sqldpass.persistent.payment;

/**
 * 구독 플랜.
 *
 * - days = "결제일 이후 추가로 받는 일수". 실제 사용 가능 일수는 결제일 포함 (days + 1).
 *   예: THREE_DAY(3) → 결제일 + 3일 = 4일치 사용. 만료는 결제일 + (days+1)일의 00:00 KR.
 * - days = null 이면 평생 (UNLIMITED).
 * - removesAds / allowsPdf / hasLibraryAccess / allowsPremium 은 plan 별 차등 (thunder-focus-paywall 매트릭스).
 * - allowsPremium 은 PASS+(난이도 ≥ 0.5 또는 visibility=PREMIUM) 회차 풀이 허용 여부.
 *   Focus 만 false — paywall 정책상 일상 학습 권한만 제공한다.
 *
 * enum 키 (THREE_DAY 등) 는 DB `subscription.plan` 컬럼에 enum name 으로 저장되므로
 * 절대 변경 금지. 사용자 노출 라벨은 프론트에서 매핑 (THREE_DAY → "Thunder").
 */
public enum SubscriptionPlan {
    THREE_DAY(3,    true, false, true, /* allowsPremium */ true,  1),
    FOCUS    (30,   true, false, true, /* allowsPremium */ false, 2),
    ONE_MONTH(30,   true, false, true, /* allowsPremium */ true,  3),
    UNLIMITED(null, true, true,  true, /* allowsPremium */ true,  4);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;
    /** 오답노트 사용 + 즐겨찾기 무제한 — 무료 외 모든 plan 에서 true. */
    private final boolean hasLibraryAccess;
    /** PASS+ 회차 풀이 허용. Focus 만 false (paywall 정책). */
    private final boolean allowsPremium;
    /** 업그레이드 비교용 강도. 큰 값일수록 강한 plan. */
    private final int tier;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf,
                     boolean hasLibraryAccess, boolean allowsPremium, int tier) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
        this.hasLibraryAccess = hasLibraryAccess;
        this.allowsPremium = allowsPremium;
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

    public boolean isAllowsPremium() {
        return allowsPremium;
    }

    /** UNLIMITED 면 true. */
    public boolean isLifetime() {
        return days == null;
    }
}
