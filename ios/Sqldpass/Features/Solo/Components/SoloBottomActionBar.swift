import SwiftUI

/// 단일 채점 풀이 하단 액션 바.
///
/// 미답 상태: [정답 확인]
/// 정답 공개 상태: [채점 완료(disabled)] [다음 문제 / 결과 보기]
///
/// safeAreaInset(.bottom) 또는 상위에서 padding 처리.
struct SoloBottomActionBar: View {
    let revealed: Bool
    let hasAnswer: Bool
    let submitting: Bool
    let isLastBeforeComplete: Bool
    let onSubmit: () -> Void
    let onNext: () -> Void

    var body: some View {
        HStack(spacing: Spacing.md) {
            if !revealed {
                Button(action: onSubmit) {
                    Text(submitting ? "확인중…" : "정답 확인")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.brandPrimaryFG)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .background(
                    RoundedRectangle(cornerRadius: Radius.sm)
                        .fill(hasAnswer && !submitting ? Color.brandPrimary : Color.appBorderStrong)
                )
                .disabled(!hasAnswer || submitting)
            } else {
                Button(action: {}) {
                    Text("채점 완료")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextSubtle)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .background(
                    RoundedRectangle(cornerRadius: Radius.sm)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .disabled(true)

                Button(action: onNext) {
                    Text(isLastBeforeComplete ? "결과 보기" : "다음 문제")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.brandPrimaryFG)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .background(
                    RoundedRectangle(cornerRadius: Radius.sm)
                        .fill(submitting ? Color.appBorderStrong : Color.brandPrimary)
                )
                .disabled(submitting)
            }
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.base)
        .background(Color.appSurface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}
