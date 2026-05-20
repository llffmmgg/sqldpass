import SwiftUI

// MARK: - Public API

/// `AppBottomActionBar` 가 받는 액션 단위.
struct BottomAction {
    let title: String
    let action: () -> Void
    var isEnabled: Bool = true
    var isLoading: Bool = false
    var variant: AppButtonVariant = .primary
}

/// 화면 하단에 고정되는 CTA 바.
///
/// - 호출 측에서 `.safeAreaInset(edge: .bottom)` 안에 배치하거나
///   `ZStack` 최상단에 두는 것을 권장한다.
/// - 단일/이중 액션 모두 지원. 이중일 때는 primary 가 더 큰 layoutPriority 를 가진다.
struct AppBottomActionBar: View {
    let primary: BottomAction
    var secondary: BottomAction? = nil
    /// 버튼 크기 — 기본은 `.large`(56pt). 모의고사·실전문제 풀이 화면처럼 얇은 액션 바를 원하면 `.regular` 전달.
    var buttonSize: AppButtonSize = .large

    var body: some View {
        HStack(spacing: Spacing.sm) {
            if let secondary {
                AppButton(
                    title: secondary.title,
                    variant: secondary.variant,
                    size: buttonSize,
                    isEnabled: secondary.isEnabled,
                    isLoading: secondary.isLoading,
                    action: secondary.action
                )
                .frame(maxWidth: .infinity)
                .layoutPriority(1)

                AppButton(
                    title: primary.title,
                    variant: primary.variant,
                    size: buttonSize,
                    isEnabled: primary.isEnabled,
                    isLoading: primary.isLoading,
                    action: primary.action
                )
                .frame(maxWidth: .infinity)
                .layoutPriority(2)
            } else {
                AppButton(
                    title: primary.title,
                    variant: primary.variant,
                    size: buttonSize,
                    isEnabled: primary.isEnabled,
                    isLoading: primary.isLoading,
                    action: primary.action
                )
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, buttonSize == .large ? Spacing.sm : Spacing.xs)
        .frame(minHeight: buttonSize == .large ? 56 : 44)
        .frame(maxWidth: .infinity)
        .background(Color.appSurface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}

// MARK: - Preview

#Preview("AppBottomActionBar — variants") {
    VStack(spacing: Spacing.lg) {
        Spacer().frame(height: Spacing.base)

        Text("Single primary")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppBottomActionBar(
            primary: BottomAction(title: "다음 문제", action: {})
        )

        Text("Primary + Secondary")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppBottomActionBar(
            primary: BottomAction(title: "제출", action: {}),
            secondary: BottomAction(title: "임시 저장",
                                    action: {},
                                    variant: .secondary)
        )

        Text("Loading primary")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppBottomActionBar(
            primary: BottomAction(title: "제출 중", action: {}, isLoading: true)
        )

        Text("Disabled primary")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppBottomActionBar(
            primary: BottomAction(title: "옵션 선택 필요",
                                  action: {},
                                  isEnabled: false)
        )

        Spacer()
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    .background(Color.appPage)
}
