import SwiftUI

// MARK: - Public API

/// 비동기 로딩 자리표시 셰이프.
///
/// - 두 가지 톤(`appElevated` 와 `appElevated.opacity(0.55)`) 을
///   1.1s easeInOut 으로 페이드 왕복한다.
/// - 무한 반복은 일반적으로 금지지만, 스켈레톤은 데이터 도착 전까지
///   사용자가 "로딩 중" 임을 알아채는 신호이므로 명시적 예외.
/// - `accessibilityReduceMotion` 가 true 면 중간 톤으로 스냅하고 정지.
struct AppSkeleton: View {
    var cornerRadius: CGFloat = Radius.md

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var phase: Bool = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(currentFill)
            .onAppear { startAnimating() }
    }

    private var currentFill: Color {
        if reduceMotion {
            // 동작 줄이기: 중간 톤으로 고정
            return Color.appElevated.opacity(0.78)
        }
        return phase
            ? Color.appElevated.opacity(0.55)
            : Color.appElevated
    }

    private func startAnimating() {
        guard !reduceMotion else { return }
        // 정당화: 스켈레톤은 비동기 데이터가 도착할 때까지 살아있어야 한다.
        withAnimation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true)) {
            phase = true
        }
    }
}

/// 카드 모양 스켈레톤. 카드형 리스트의 자리표시로 사용.
struct AppSkeletonCard: View {
    var height: CGFloat = 120
    var cornerRadius: CGFloat = Radius.lg

    var body: some View {
        AppSkeleton(cornerRadius: cornerRadius)
            .frame(height: height)
            .frame(maxWidth: .infinity)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
    }
}

// MARK: - Preview

#Preview("AppSkeleton — rectangle + card") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("Rectangle (Radius.md)")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppSkeleton()
                .frame(height: 20)
            AppSkeleton()
                .frame(height: 20)
                .frame(maxWidth: 220)
            AppSkeleton()
                .frame(height: 12)
                .frame(maxWidth: 160)

            Text("Card (height 120)")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppSkeletonCard()
            AppSkeletonCard(height: 80)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
