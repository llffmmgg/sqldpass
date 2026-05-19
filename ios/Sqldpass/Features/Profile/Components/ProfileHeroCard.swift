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
        VStack(alignment: .leading, spacing: Spacing.base) {
            topRow
            streakStrip
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.brandPrimary.opacity(0.10))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.xl, style: .continuous)
                .stroke(Color.brandPrimary.opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
    }

    // MARK: - Top row

    @ViewBuilder
    private var topRow: some View {
        HStack(spacing: Spacing.md) {
            mascotBox

            VStack(alignment: .leading, spacing: Spacing.xxs) {
                HStack(spacing: Spacing.xs) {
                    Text(displayNickname)
                        .font(AppType.heading.weight(.bold))
                        .foregroundStyle(Color.appTextPrimary)
                        .lineLimit(1)
                    subscriptionBadge
                }
                if let provider, !provider.isEmpty {
                    Text("\(provider.lowercased()) 로그인")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                        .lineLimit(1)
                } else if let errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(AppType.caption)
                        .foregroundStyle(Color.semanticDanger)
                        .lineLimit(2)
                }
            }

            Spacer(minLength: 0)

            if !isActive {
                Button(action: onUpgradeTap) {
                    Text("PRO")
                        .font(AppType.caption.weight(.bold))
                        .foregroundStyle(Color.brandPrimaryFG)
                        .padding(.horizontal, Spacing.md)
                        .padding(.vertical, Spacing.xs)
                        .background(Color.brandPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("PRO 업그레이드")
            }
        }
    }

    private var mascotBox: some View {
        ZStack {
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .fill(Color.brandPrimary.opacity(0.12))
            AppMascot(pose: .greeting, sizeDp: 44, animateOnAppear: false)
        }
        .frame(width: 56, height: 56)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .stroke(Color.brandPrimary.opacity(0.18), lineWidth: 1)
        )
    }

    private var subscriptionBadge: some View {
        Text(badgeLabel)
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(isActive ? Color.brandPrimary : Color.appTextMuted)
            .padding(.horizontal, Spacing.xs)
            .padding(.vertical, 2)
            .background(isActive ? Color.brandPrimary.opacity(0.12) : Color.appElevated)
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
    }

    // MARK: - Streak strip

    private var streakStrip: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(alignment: .center, spacing: Spacing.md) {
                Text("🔥")
                    .font(AppType.title)
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 2) {
                    Text("최장 연속")
                        .font(AppType.caption.weight(.bold))
                        .foregroundStyle(Color.semanticWarning)
                    HStack(alignment: .firstTextBaseline, spacing: Spacing.xxs) {
                        Text(streak.map { String($0.longestStreak) } ?? "—")
                            .font(AppType.monoNumericLarge.weight(.bold))
                            .foregroundStyle(Color.appTextPrimary)
                            .lineLimit(1)
                        Text("일")
                            .font(AppType.footnote.weight(.semibold))
                            .foregroundStyle(Color.appTextMuted)
                    }
                }

                Spacer(minLength: 0)

                weekDotsRow
            }

            Text("일주일 기록은 곧 표시돼요")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
        }
        .padding(Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
    }

    private var weekDotsRow: some View {
        let labels = ["월", "화", "수", "목", "금", "토", "일"]
        let todayIdx = todayMondayBasedIndex
        let solvedToday = streak?.solvedToday ?? false

        return HStack(spacing: 4) {
            ForEach(0..<7, id: \.self) { i in
                VStack(spacing: 2) {
                    dayCell(isToday: i == todayIdx, solved: i == todayIdx && solvedToday)
                    Text(labels[i])
                        .font(AppType.caption)
                        .foregroundStyle(i == todayIdx ? Color.semanticWarning : Color.appTextSubtle)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("이번 주 학습 기록 — 오늘 \(labels[todayIdx])")
    }

    @ViewBuilder
    private func dayCell(isToday: Bool, solved: Bool) -> some View {
        let shape = RoundedRectangle(cornerRadius: Radius.sm, style: .continuous)
        ZStack {
            if solved {
                shape.fill(Color.semanticWarning)
            } else {
                shape.fill(Color.appSurface)
            }
            shape.stroke(
                isToday ? Color.semanticWarning : Color.appBorder,
                lineWidth: 1
            )
        }
        .frame(width: 18, height: 18)
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
