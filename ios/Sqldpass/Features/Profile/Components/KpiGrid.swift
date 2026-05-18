import SwiftUI

/// 내정보 상단 KPI 2x2 그리드.
///
/// 항목 순서(좌→우, 위→아래):
/// 1. 총 풀이 (문제)
/// 2. 평균 정답률 (%)
/// 3. 최장 연속 (일)
/// 4. 합격 확률 (%)
///
/// `docs/MOBILE_UX_SPEC.md` § 2.5 / § 6 의 단일 진실 원천을 따른다.
/// Android 미러: `mobile/app/src/main/java/com/sqldpass/app/ui/profile/KpiGrid.kt`.
///
/// 값이 `nil` 이면 "—" 를 표시한다 (placeholder).
/// 본 phase 에서는 `longestStreak` 만 실데이터, 나머지는 `nil`.
struct KpiGrid: View {
    let kpi: ProfileKpi

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.md),
        GridItem(.flexible(), spacing: Spacing.md)
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: Spacing.md) {
            KpiTile(
                icon: "checklist",
                label: "총 풀이",
                value: kpi.totalSolved.map(String.init) ?? "—",
                unit: "문제"
            )
            KpiTile(
                icon: "percent",
                label: "평균 정답률",
                value: kpi.avgCorrectRate.map { "\($0)%" } ?? "—",
                unit: nil
            )
            KpiTile(
                icon: "flame.fill",
                label: "최장 연속",
                value: kpi.longestStreak.map(String.init) ?? "—",
                unit: "일"
            )
            KpiTile(
                icon: "chart.line.uptrend.xyaxis",
                label: "합격 확률",
                value: kpi.passProbability.map { "\($0)%" } ?? "—",
                unit: nil
            )
        }
    }
}

#Preview("Placeholder (전부 nil)") {
    KpiGrid(kpi: .empty)
        .padding()
        .background(Color.appPage)
}

#Preview("Streak 만 실데이터") {
    KpiGrid(
        kpi: ProfileKpi(
            totalSolved: nil,
            avgCorrectRate: nil,
            longestStreak: 12,
            passProbability: nil
        )
    )
    .padding()
    .background(Color.appPage)
}
