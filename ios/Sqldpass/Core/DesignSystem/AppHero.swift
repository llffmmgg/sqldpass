import SwiftUI

// MARK: - Public API

/// 화면 최상단 hero. 그라데이션 없이 단단한 페이지 톤 + 하단 1pt hairline.
///
/// - 좌측: optional eyebrow → title → optional subtitle
/// - 우측: optional accessory (검색 버튼, 칩 등) + optional mascot
///
/// 상단 safe area 는 `NavigationStack` 또는 호출자가 처리한다고 가정한다.
struct AppHero<Accessory: View>: View {
    let title: String
    var eyebrow: String? = nil
    var subtitle: String? = nil
    var mascot: AppMascotPose? = nil
    @ViewBuilder var accessory: () -> Accessory

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            if let eyebrow {
                Text(eyebrow.uppercased())
                    .font(AppType.caption.weight(.semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color.brandPrimary)
            }

            HStack(alignment: .top, spacing: Spacing.md) {
                Text(title)
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: Spacing.sm)

                if let mascot {
                    AppMascot(pose: mascot, sizeDp: 56)
                }

                accessory()
            }

            if let subtitle {
                Text(subtitle)
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, Spacing.md)
        .padding(.bottom, Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appPage)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}

// MARK: - Convenience overload (no accessory)

extension AppHero where Accessory == EmptyView {
    init(title: String,
         eyebrow: String? = nil,
         subtitle: String? = nil,
         mascot: AppMascotPose? = nil) {
        self.title = title
        self.eyebrow = eyebrow
        self.subtitle = subtitle
        self.mascot = mascot
        self.accessory = { EmptyView() }
    }
}

// MARK: - Preview

#Preview("AppHero — variants") {
    ScrollView {
        VStack(spacing: Spacing.lg) {
            AppHero(title: "오늘의 풀이")

            AppHero(title: "오늘의 풀이",
                    eyebrow: "Dashboard")

            AppHero(title: "오늘의 풀이",
                    eyebrow: "Dashboard",
                    subtitle: "꾸준한 연습이 합격을 만듭니다.")

            AppHero(title: "안녕하세요, 희훈님",
                    eyebrow: "환영합니다",
                    subtitle: "이번 주는 12문제를 풀었어요.",
                    mascot: .greeting)

            AppHero(title: "모의고사",
                    eyebrow: "MOCK",
                    subtitle: "최근 응시 기록을 확인하세요.") {
                AppBadge(label: "NEW", tone: .accent, variant: .solid)
            }
        }
        .padding(.vertical, Spacing.lg)
    }
    .background(Color.appPage)
}
