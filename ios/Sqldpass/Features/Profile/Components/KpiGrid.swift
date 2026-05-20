import SwiftUI

/// 사용자 KPI — 가로 2 타일 (총 풀이 / 평균 정답률).
/// 합격 확률·오답은 별 phase 에서 백엔드 신설로 채우기 전까지 본 그리드에서 제외한다.
struct KpiGrid: View {
    let kpi: ProfileKpi

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.md),
        GridItem(.flexible(), spacing: Spacing.md)
    ]

    private let placeholder = "데이터가 쌓이면 표시돼요"

    var body: some View {
        LazyVGrid(columns: columns, spacing: Spacing.md) {
            KpiTile(
                icon: "checkmark.circle",
                label: "총 풀이",
                value: kpi.totalSolved.map(String.init) ?? "—",
                unit: "문제",
                iconColor: .semanticSuccess,
                trend: kpi.totalSolved == nil ? placeholder : nil
            )
            KpiTile(
                icon: "target",
                label: "평균 정답률",
                value: kpi.avgCorrectRate.map(String.init) ?? "—",
                unit: "%",
                iconColor: .brandPrimary,
                trend: kpi.avgCorrectRate == nil ? placeholder : nil
            )
        }
    }
}

#Preview("KpiGrid — placeholder") {
    KpiGrid(kpi: .empty)
        .padding()
        .background(Color.appPage)
}

#Preview("KpiGrid — populated") {
    KpiGrid(
        kpi: ProfileKpi(
            totalSolved: 312,
            avgCorrectRate: 74,
            longestStreak: 9,
            passProbability: nil
        )
    )
    .padding()
    .background(Color.appPage)
}
