import SwiftUI

/// 내정보(마이) 탭 상단 hero 카드.
///
/// 구성(위→아래):
/// 1. 상단 행 — 56pt 마스코트 박스 + 닉네임/구독 배지 + provider 캡션
///    + (비활성 구독일 때만) 우측 `PRO` 업그레이드 칩
/// 2. streak strip — 🔥 + "최장 연속" + 큰 일수 + 7개 요일 미니 셀
///
/// 디자인 출처: 871lJPyM 핸드오프 `screens.jsx` 의 `MyScreen` (line 348~).
/// 핸드오프의 violet/orange 액센트는 디자인 토큰
/// (`Color.brandPrimary`, `Color.semanticWarning`)으로 대치한다.
///
/// 디자인 토큰만 사용 — hex/font-size 신규 정의 금지.
struct ProfileHeroCard: View {
    let nickname: String?
    let provider: String?
    let subscription: SubscriptionInfo?
    let streak: StreakInfo?
    let errorMessage: String?
    var onUpgradeTap: () -> Void = {}

    private var displayNickname: String { nickname ?? "학습자" }
    private var badgeLabel: String { subscription?.displayBadgeLabel ?? "FREE" }
    private var isActive: Bool { subscription?.active ?? false }

    /// 오늘의 요일(월=0..일=6).
    /// `Calendar.current.component(.weekday, ...)`: 일=1, 월=2, …, 토=7.
    private var todayMondayBasedIndex: Int {
        let weekday = Calendar.current.component(.weekday, from: Date())
        // 일=1 → 6, 월=2 → 0, 화=3 → 1, …, 토=7 → 5
        return (weekday + 5) % 7
    }

    var body: some View {
        topRow
            .padding(Spacing.base)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.brandPrimary)
            .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
    }

    // MARK: - Top row

    @ViewBuilder
    private var topRow: some View {
        HStack(spacing: Spacing.md) {
            initialAvatar

            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(displayNickname)
                    .font(AppType.heading.weight(.bold))
                    .foregroundStyle(Color.brandPrimaryFG)
                    .lineLimit(1)
                if let provider, !provider.isEmpty {
                    Text("\(provider.lowercased()) 로그인")
                        .font(AppType.caption)
                        .foregroundStyle(Color.brandPrimaryFG.opacity(0.85))
                        .lineLimit(1)
                } else if let errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(AppType.caption)
                        .foregroundStyle(Color.brandPrimaryFG)
                        .lineLimit(2)
                }
            }

            Spacer(minLength: 0)
        }
    }

    /// 닉네임 첫 글자 이니셜 아바타 — 진한 초록 배경 위 흰 톤 박스.
    private var initialAvatar: some View {
        ZStack {
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .fill(Color.brandPrimaryFG.opacity(0.20))
            Text(String(displayNickname.prefix(1)))
                .font(AppType.heading.weight(.bold))
                .foregroundStyle(Color.brandPrimaryFG)
        }
        .frame(width: 56, height: 56)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .stroke(Color.brandPrimaryFG.opacity(0.3), lineWidth: 1)
        )
    }

}

// MARK: - Preview

#Preview("ProfileHeroCard — variants") {
    ScrollView {
        VStack(spacing: Spacing.lg) {
            ProfileHeroCard(
                nickname: "경북대화이팅",
                provider: "GOOGLE",
                subscription: SubscriptionInfo(
                    active: false,
                    plan: nil,
                    expiresAt: nil,
                    removesAds: nil,
                    allowsPdf: nil,
                    hasLibraryAccess: nil,
                    allowsPremium: nil
                ),
                streak: StreakInfo(
                    currentStreak: 3,
                    longestStreak: 12,
                    lastSolveDate: nil,
                    solvedToday: true
                ),
                errorMessage: nil
            )

            ProfileHeroCard(
                nickname: nil,
                provider: nil,
                subscription: SubscriptionInfo(
                    active: true,
                    plan: "PRO",
                    expiresAt: nil,
                    removesAds: true,
                    allowsPdf: true,
                    hasLibraryAccess: true,
                    allowsPremium: true
                ),
                streak: nil,
                errorMessage: "네트워크 오류"
            )
        }
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}
