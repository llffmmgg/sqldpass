import Charts
import SwiftUI

struct InsightsView: View {
    @State private var viewModel = InsightsViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationTitle("인사이트")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.streak == nil {
                    await viewModel.load()
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.streak == nil {
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.streak == nil {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    overviewSection
                    if !viewModel.recentSolves.isEmpty {
                        scoreTrendSection
                    }
                    if !viewModel.subjectStats.isEmpty {
                        subjectWrongRateSection
                    }
                }
                .padding(Spacing.base)
            }
        }
    }

    private var overviewSection: some View {
        HStack(spacing: Spacing.md) {
            overviewCard(
                label: "연속 학습",
                value: viewModel.streak.map { "\($0.currentStreak)" } ?? "—",
                unit: "일",
                color: .semanticWarning,
                icon: "flame.fill"
            )
            overviewCard(
                label: "최장",
                value: viewModel.streak.map { "\($0.longestStreak)" } ?? "—",
                unit: "일",
                color: .brandPrimary,
                icon: "trophy.fill"
            )
            overviewCard(
                label: "전체 평균",
                value: viewModel.overallStats.map { String(format: "%.1f", $0.avgDailyCount) } ?? "—",
                unit: "건/일",
                color: .semanticInfo,
                icon: "chart.bar.fill"
            )
        }
    }

    private func overviewCard(label: String, value: String, unit: String, color: Color, icon: String) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(spacing: Spacing.xs) {
                Image(systemName: icon)
                    .font(.footnote)
                    .foregroundStyle(color)
                Text(label)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(value)
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(color)
                Text(unit)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
        .padding(Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var scoreTrendSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("최근 점수 추이")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Chart {
                ForEach(Array(viewModel.recentSolves.enumerated()), id: \.element.id) { idx, solve in
                    LineMark(
                        x: .value("회차", idx + 1),
                        y: .value("점수", solve.score)
                    )
                    .foregroundStyle(Color.brandPrimary)
                    .symbol(.circle)
                    .interpolationMethod(.catmullRom)
                }
            }
            .chartYScale(domain: 0...100)
            .chartYAxis {
                AxisMarks(values: [0, 50, 100])
            }
            .frame(height: 200)
            .padding(Spacing.md)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
    }

    private var subjectWrongRateSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("과목별 오답률")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Chart {
                ForEach(viewModel.subjectStats) { stat in
                    BarMark(
                        x: .value("오답률", stat.wrongRate),
                        y: .value("과목", stat.subjectName)
                    )
                    .foregroundStyle(barColor(for: stat.wrongRate))
                    .annotation(position: .trailing) {
                        Text("\(stat.wrongRate)%")
                            .font(AppType.caption)
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
            }
            .chartXScale(domain: 0...100)
            .chartXAxis {
                AxisMarks(values: [0, 25, 50, 75, 100]) { value in
                    AxisGridLine()
                    AxisValueLabel {
                        if let v = value.as(Int.self) {
                            Text("\(v)%")
                        }
                    }
                }
            }
            .frame(height: CGFloat(max(120, viewModel.subjectStats.count * 36)))
            .padding(Spacing.md)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
    }

    private func barColor(for rate: Int) -> Color {
        if rate >= 50 { return .semanticDanger }
        if rate >= 25 { return .semanticWarning }
        return .brandPrimary
    }
}

#Preview {
    NavigationStack {
        InsightsView()
    }
}
