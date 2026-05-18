import SwiftUI

struct SolveActionBar: View {
    let canGoPrevious: Bool
    let canGoNext: Bool
    let isLastQuestion: Bool
    let isSubmitting: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        HStack(spacing: Spacing.md) {
            Button(action: onPrevious) {
                Label("이전", systemImage: "chevron.left")
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
            }
            .buttonStyle(.bordered)
            .disabled(!canGoPrevious)

            if isLastQuestion {
                Button(action: onSubmit) {
                    if isSubmitting {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    } else {
                        Text("제출하기")
                            .font(AppType.bodyEmph)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .disabled(isSubmitting)
            } else {
                Button(action: onNext) {
                    Label("다음", systemImage: "chevron.right")
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .disabled(!canGoNext)
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.sm)
        .background(Color.appSurface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}
