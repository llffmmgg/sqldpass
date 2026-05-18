package com.sqldpass.persistent.payment;

/**
 * 구독 플랜.
 *
 * - days = "결제일 이후 추가로 받는 일수". 실제 사용 가능 일수는 결제일 포함 (days + 1).
 *   예: THREE_DAY(3) → 결제일 + 3일 = 4일치 사용. 만료는 결제일 + (days+1)일의 00:00 KR.
 * - UNLIMITED 는 180 일 (= 6개월). 사용자 노출 라벨은 "All Pass" 유지.
 *   현재 모든 plan 이 days 를 가지므로 isLifetime() 은 항상 false 반환.
 *   단, 기존 subscription 테이블에 expires_at=NULL 로 저장된 평생권은 활성 판정
 *   (expires_at IS NULL OR > now) 에서 자동으로 평생 유지 — 정책 결정.
 * - removesAds / allowsPdf / hasLibraryAccess / allowsPremium 은 plan 별 차등 (thunder-focus-paywall 매트릭스).
 * - allowsPremium 은 PASS+(난이도 ≥ 0.5 또는 visibility=PREMIUM) 회차 풀이 허용 여부.
 *   Focus 만 false — paywall 정책상 일상 학습 권한만 제공한다.
 *
 * enum 키 (THREE_DAY 등) 는 DB `subscription.plan` 컬럼에 enum name 으로 저장되므로
 * 절대 변경 금지. 사용자 노출 라벨은 프론트에서 매핑 (THREE_DAY → "Thunder").
 */
public enum SubscriptionPlan {
    THREE_DAY(3,    true, false, true, /* allowsPremium */ true),
    FOCUS    (30,   true, false, true, /* allowsPremium */ false),
    ONE_MONTH(30,   true, false, true, /* allowsPremium */ true),
    UNLIMITED(180,  true, true,  true, /* allowsPremium */ true);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;
    /** 오답노트 사용 + 즐겨찾기 무제한 — 무료 외 모든 plan 에서 true. */
    private final boolean hasLibraryAccess;
    /** PASS+ 회차 풀이 허용. Focus 만 false (paywall 정책). */
    private final boolean allowsPremium;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf,
                     boolean hasLibraryAccess, boolean allowsPremium) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
        this.hasLibraryAccess = hasLibraryAccess;
        this.allowsPremium = allowsPremium;
    }

    /**
     * 본 plan 이 currentPlan 의 업그레이드인지 — 명시적 매트릭스.
     *
     * 단순 tier 비교로는 THREE_DAY(3일 PASS+ 포함) → FOCUS(30일 PASS+ 없음) 같은
     * "기간/기능 교차" 경로가 업그레이드로 인식되는 문제가 있어 매트릭스로 고정.
     *
     * 허용:
     *   THREE_DAY → ONE_MONTH, UNLIMITED   (FOCUS 는 PASS+ 잃는 비대칭 경로라 차단)
     *   FOCUS     → ONE_MONTH, UNLIMITED
     *   ONE_MONTH → UNLIMITED
     *   UNLIMITED → (없음)
     */
    public boolean isUpgradeFrom(SubscriptionPlan currentPlan) {
        if (currentPlan == null || this == currentPlan) {
            return false;
        }
        return switch (currentPlan) {
            case THREE_DAY -> this == ONE_MONTH || this == UNLIMITED;
            case FOCUS     -> this == ONE_MONTH || this == UNLIMITED;
            case ONE_MONTH -> this == UNLIMITED;
            case UNLIMITED -> false;
        };
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

    /**
     * days 가 null 이면 true. 현재 모든 plan 이 days 를 가지므로 항상 false.
     * 호출부(만료 계산) 는 "expires_at 을 null 로 저장할지 vs 날짜 계산할지" 분기용으로 유지.
     */
    public boolean isLifetime() {
        return days == null;
    }
}
