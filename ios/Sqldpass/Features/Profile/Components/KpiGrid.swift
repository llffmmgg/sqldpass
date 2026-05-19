import SwiftUI

/// 내정보 상단 KPI 2x2 그리드 (4 타일).
///
/// 항목 순서(좌→우, 위→아래):
/// 1. 총 풀이 (문제)        — checkmark.circle / semanticSuccess
/// 2. 평균 정답률 (%)       — target / brandPrimary
/// 3. 합격 확률 (%)          — chart.line.uptrend.xyaxis / semanticInfo
/// 4. 오답 (개)              — note.text / semanticWarning
///
/// 871lJPyM 핸드오프 `screens.jsx` MyScreen (line 414~) StatCard 4 종에 대응.
/// 백엔드가 아직 4 종 모두를 제공하지 않으므로 본 phase 에서는 전 타일 `—`
/// placeholder + trend 캡션 "데이터가 쌓이면 표시돼요" 로 통일한다.
///
/// 최장 연속 KPI 는 Hero 카드의 streak strip 으로 이동했으므로 더 이상
/// 본 그리드에 포함되지 않는다.
struct KpiGrid: View {
    /// 미사용 — 데이터가 채워지는 별 phase 에서 동일 props 시그니처를 유지하기 위해 남겨둔다.
    let kpi: ProfileKpi

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.md),
        GridItem(.flexible(), spacing: Spacing.md)
    ]

    private let trendCaption = "데이터가 쌓이면 표시돼요"

    var body: some View {
        LazyVGrid(columns: columns, spacing: Spacing.md) {
            KpiTile(
                icon: "checkmark.circle",
                label: "총 풀이",
                value: "—",
                unit: "문제",
                iconColor: .semanticSuccess,
                trend: trendCaption
            )
            KpiTile(
                icon: "target",
                label: "평균 정답률",
                value: "—",
                unit: "%",
                iconColor: .brandPrimary,
                trend: trendCaption
            )
            KpiTile(
                icon: "chart.line.uptrend.xyaxis",
                label: "합격 확률",
                value: "—",
                unit: "%",
                iconColor: .semanticInfo,
                trend: trendCaption
            )
            KpiTile(
                icon: "note.text",
                label: "오답",
                value: "—",
                unit: "개",
                iconColor: .semanticWarning,
                trend: trendCaption
            )
        }
    }
}

#Preview("KpiGrid — placeholder (4 타일)") {
    KpiGrid(kpi: .empty)
        .padding()
        .background(Color.appPage)
}
