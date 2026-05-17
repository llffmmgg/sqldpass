import SwiftUI

struct SolveHeader: View {
    let progress: Double
    let currentIndex: Int
    let totalCount: Int
    let answeredCount: Int
    let elapsedSeconds: Int

    var body: some View {
        VStack(spacing: Spacing.xs) {
            HStack {
                Text("\(currentIndex + 1) / \(totalCount)")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer()
                Text(formattedTime)
                    .font(AppType.monoNumeric.weight(.semibold))
                    .foregroundStyle(Color.brandPrimary)
            }
            ProgressView(value: progress)
                .tint(Color.brandPrimary)
            HStack {
                Text("답안 \(answeredCount) / \(totalCount)")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
            }
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
