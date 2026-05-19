import SwiftUI

/// 원형 진척도 표시.
///
/// `value/total` 비율을 한 줄 stroke 로 채우고, 가운데에 caller 가 지정한 짧은 라벨을
/// 배치한다. 모의고사 카드의 best score, 기출 카드의 진행률, 홈의 오늘 목표 ring 등에
/// 공통으로 사용한다.
///
/// 색·폰트는 디자인 토큰을 그대로 사용:
/// - 배경 트랙: `Color.appElevated`
/// - 진행 stroke: caller 가 주입 (`Color.brandPrimary` 또는 자격증 액센트)
/// - 가운데 라벨: `AppType.monoNumeric.weight(.semibold)` + 호출자 색
struct AppProgressRing: View {
    let value: Int
    let total: Int
    var color: Color = .brandPrimary
    var size: CGFloat = 52
    var stroke: CGFloat = 4
    /// 가운데에 표시할 라벨. nil 이면 퍼센트 자동 계산.
    var label: String? = nil

    private var safeTotal: Int { max(total, 1) }
    private var clampedValue: Int { max(0, min(value, safeTotal)) }
    private var fraction: CGFloat {
        CGFloat(clampedValue) / CGFloat(safeTotal)
    }
    private var displayLabel: String {
        if let label { return label }
        return "\(Int((fraction * 100).rounded()))%"
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.appElevated, lineWidth: stroke)
            Circle()
                .trim(from: 0, to: fraction)
                .stroke(color, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.45), value: fraction)
            Text(displayLabel)
                .font(AppType.monoNumeric.weight(.semibold))
                .foregroundStyle(color)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
        }
        .frame(width: size, height: size)
    }
}

#Preview("AppProgressRing — variants") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            HStack(spacing: Spacing.base) {
                AppProgressRing(value: 7, total: 10, color: .brandPrimary)
                AppProgressRing(value: 50, total: 50, color: .semanticSuccess, label: "✓")
                AppProgressRing(value: 12, total: 50, color: .certSQLD)
                AppProgressRing(value: 0, total: 20, color: .semanticDanger)
            }

            HStack(spacing: Spacing.base) {
                AppProgressRing(value: 8, total: 20, color: .certEngineerPractical, size: 56, stroke: 4.5)
                AppProgressRing(value: 17, total: 20, color: .certADSP, size: 42, stroke: 4, label: "17")
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
