import SwiftUI

// MARK: - Public API

/// `AppStateView` 가 사용하는 보조 액션.
struct AppViewAction {
    let label: String
    let onTap: () -> Void
}

/// 화면 전체를 차지하는 상태 뷰의 케이스.
enum AppViewState {
    case loading
    case empty(title: String,
               message: String? = nil,
               mascot: AppMascotPose = .guide,
               action: AppViewAction? = nil)
    case errorState(title: String,
                    message: String? = nil,
                    onRetry: (() -> Void)? = nil)
}

/// 빈 상태 / 로딩 / 에러를 한 곳에서 표현하는 풀-블리드 뷰.
struct AppStateView: View {
    let state: AppViewState

    var body: some View {
        Group {
            switch state {
            case .loading:
                loadingBody
            case let .empty(title, message, mascot, action):
                emptyBody(title: title,
                          message: message,
                          mascot: mascot,
                          action: action)
            case let .errorState(title, message, onRetry):
                errorBody(title: title,
                          message: message,
                          onRetry: onRetry)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(Spacing.lg)
        .background(Color.appPage)
    }

    // MARK: - Sub-views

    private var loadingBody: some View {
        VStack(spacing: Spacing.sm) {
            ProgressView()
                .tint(Color.brandPrimary)
            Text("불러오는 중…")
                .font(AppType.callout)
                .foregroundStyle(Color.appTextMuted)
        }
    }

    private func emptyBody(title: String,
                           message: String?,
                           mascot: AppMascotPose,
                           action: AppViewAction?) -> some View {
        VStack(spacing: Spacing.md) {
            AppMascot(pose: mascot, sizeDp: 96)
            Text(title)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .multilineTextAlignment(.center)
            if let message {
                Text(message)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .multilineTextAlignment(.center)
            }
            if let action {
                AppButton(title: action.label,
                          variant: .secondary,
                          action: action.onTap)
                    .frame(maxWidth: 240)
                    .padding(.top, Spacing.xs)
            }
        }
    }

    private func errorBody(title: String,
                           message: String?,
                           onRetry: (() -> Void)?) -> some View {
        VStack(spacing: Spacing.md) {
            // 침착함 유지를 위해 색을 빨갛게 칠하지 않는다.
            AppMascot(pose: .review, sizeDp: 96)
            Text(title)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .multilineTextAlignment(.center)
            if let message {
                Text(message)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .multilineTextAlignment(.center)
            }
            if let onRetry {
                AppButton(title: "다시 시도",
                          variant: .secondary,
                          action: onRetry)
                    .frame(maxWidth: 240)
                    .padding(.top, Spacing.xs)
            }
        }
    }
}

// MARK: - Preview

#Preview("AppStateView — loading / empty / error") {
    TabView {
        AppStateView(state: .loading)
            .tabItem { Label("Loading", systemImage: "hourglass") }

        AppStateView(state: .empty(
            title: "아직 풀이 기록이 없어요",
            message: "오늘 첫 문제를 풀어보면 여기에 통계가 쌓입니다.",
            mascot: .guide,
            action: AppViewAction(label: "문제 풀기", onTap: {})
        ))
        .tabItem { Label("Empty", systemImage: "tray") }

        AppStateView(state: .errorState(
            title: "데이터를 불러오지 못했어요",
            message: "네트워크 상태를 확인하고 다시 시도해주세요.",
            onRetry: {}
        ))
        .tabItem { Label("Error", systemImage: "exclamationmark.triangle") }
    }
}
