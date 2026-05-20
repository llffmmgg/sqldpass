import SwiftUI

/// 모의고사 풀이 하단 액션 바.
///
/// - 일반 문항: `이전` (secondary, 3) + `다음` (primary, 7) — 가로 3:7 비율.
/// - 마지막 문항: `이전` (secondary, 3) + `제출하기` (primary, 7).
///
/// 비율은 `containerRelativeFrame(.horizontal, count: 10, span: N)` 로 강제.
struct SolveActionBar: View {
    let canGoPrevious: Bool
    let canGoNext: Bool
    let isLastQuestion: Bool
    let isSubmitting: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        HStack(spacing: Spacing.sm) {
            AppButton(
                title: "이전",
                variant: .secondary,
                size: .regular,
                isEnabled: canGoPrevious,
                action: onPrevious
            )
            .containerRelativeFrame(
                .horizontal,
                count: 10,
                span: 3,
                spacing: Spacing.sm
            )

            primaryButton
                .containerRelativeFrame(
                    .horizontal,
                    count: 10,
                    span: 7,
                    spacing: Spacing.sm
                )
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.xs)
        .frame(minHeight: 44)
        .frame(maxWidth: .infinity)
        .background(Color.appSurface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }

    @ViewBuilder
    private var primaryButton: some View {
        if isLastQuestion {
            AppButton(
                title: "제출하기",
                variant: .primary,
                size: .regular,
                isEnabled: !isSubmitting,
                isLoading: isSubmitting,
                action: onSubmit
            )
        } else {
            AppButton(
                title: "다음",
                variant: .primary,
                size: .regular,
                isEnabled: canGoNext,
                action: onNext
            )
        }
    }
}
