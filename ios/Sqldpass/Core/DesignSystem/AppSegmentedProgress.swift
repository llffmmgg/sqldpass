import SwiftUI

/// 풀이 진행 segmented 바 — N 개 셀로 분할된 progress.
///
/// 디자인 출처: 871lJPyM `screens.jsx` QuizScreen 상단. 단일 fill 알약 대신 N 셀로
/// 나눠 현재까지 답한 문항 수가 한눈에 들어오도록 한다.
///
/// 셀 상태:
/// - `i < current`  → primary fill (`color`)
/// - `i == current` → outline (현재 문항)
/// - `i >  current` → 비어 있음 (`appElevated`)
///
/// 큰 세트 (50문제 이상) 에서는 셀이 너무 좁아질 수 있어 caller 가 `maxSegments` 로 셀 수를
/// clamp 하면 작은 알약 progress 처럼 보이게 fallback.
struct AppSegmentedProgress: View {
    let current: Int
    let total: Int
    var color: Color = .brandPrimary
    var height: CGFloat = 5
    var maxSegments: Int = 30

    private var safeTotal: Int { max(total, 1) }
    private var clampedCurrent: Int { max(0, min(current, safeTotal)) }

    /// 디자인 의도상 총 문항 수만큼 셀을 그리지만, 셀이 너무 좁아지면 가독성을 해친다.
    /// `maxSegments` 를 초과하면 단일 continuous bar 로 fallback.
    private var useSegments: Bool { safeTotal <= maxSegments }

    var body: some View {
        Group {
            if useSegments {
                HStack(spacing: 2) {
                    ForEach(0..<safeTotal, id: \.self) { i in
                        cell(for: i)
                    }
                }
            } else {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.appElevated)
                        Capsule()
                            .fill(color)
                            .frame(width: max(0, geo.size.width * CGFloat(clampedCurrent) / CGFloat(safeTotal)))
                            .animation(.easeOut(duration: 0.28), value: clampedCurrent)
                    }
                }
                .frame(height: height)
            }
        }
        .frame(height: height)
    }

    @ViewBuilder
    private func cell(for index: Int) -> some View {
        if index < clampedCurrent {
            Capsule().fill(color)
        } else if index == clampedCurrent {
            Capsule()
                .fill(Color.appElevated)
                .overlay(
                    Capsule().stroke(color.opacity(0.6), lineWidth: 1)
                )
        } else {
            Capsule().fill(Color.appElevated)
        }
    }
}

#Preview("AppSegmentedProgress") {
    VStack(alignment: .leading, spacing: Spacing.lg) {
        Text("10 문항 중 7 진행")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppSegmentedProgress(current: 7, total: 10)

        Text("20 문항 중 8 진행")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppSegmentedProgress(current: 8, total: 20)

        Text("50 문항 → maxSegments(30) 초과 → continuous bar")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
        AppSegmentedProgress(current: 23, total: 50)
    }
    .padding(Spacing.base)
    .background(Color.appPage)
}
