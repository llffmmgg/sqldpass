import SwiftUI

/// 모의고사 진행 상단 헤더 (Inked OMR 디자인 시스템).
///
/// 구성: 진행 알약 (현재 문항 / 전체) + 우측에 경과시간 알약.
/// 좌측 닫기 / 우측 북마크-플래그 아이콘은 호출 측(SolveView) 의 책임.
struct SolveHeader: View {
    let progress: Double
    let currentIndex: Int
    let totalCount: Int
    let answeredCount: Int
    let elapsedSeconds: Int

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(spacing: Spacing.sm) {
                AppProgressPill(
                    current: currentIndex + 1,
                    total: totalCount,
                    label: "문항"
                )

                Spacer(minLength: Spacing.sm)

                HStack(spacing: Spacing.xs) {
                    Image(systemName: "clock")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                    Text(formattedTime)
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                }
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, Spacing.sm - 2)
                .frame(minHeight: 32)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.full, style: .continuous)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
            }

            Text("답안 \(answeredCount) / \(totalCount)")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("진행 \(currentIndex + 1)번째 문제 중 \(totalCount)번째, 경과 \(elapsedSeconds)초, 답안 \(answeredCount)개 작성")
    }

    private var formattedTime: String {
        let m = elapsedSeconds / 60
        let s = elapsedSeconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}
