# Step 5 — 인사이트 화면 (InsightsView + Swift Charts)

## Background

학습 통계 시각화 화면. Swift Charts (iOS 16+) 시스템 framework 활용.

데이터 소스:
- `GET /api/streak/me` — 연속 학습 정보
- `GET /api/solves/stats/overall-avg` — 전체 14일 평균 풀이 수
- `GET /api/wrong-answers/stats` — 과목별 오답률 (Step 1 추가)
- `GET /api/solves` — 최근 N건으로 점수 추이 (이미 SolveService.myHistory)

차트:
- **BarMark**: 과목별 오답률 가로 막대
- **LineMark**: 최근 풀이 점수 추이

## Workdir

```bash
ios/
```

## Dependencies

- Step 1: WrongAnswerService.stats
- 기존: StreakService.me, SolveService.overallStats, SolveService.myHistory

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Insights/InsightsView.swift` | 신규 |
| `ios/Sqldpass/Features/Insights/InsightsViewModel.swift` | 신규 |

## Implementation

### `InsightsViewModel.swift`

```swift
import Foundation
import Observation

@Observable
final class InsightsViewModel {
    private(set) var streak: StreakInfo?
    private(set) var overallStats: OverallStats?
    private(set) var subjectStats: [WrongAnswerStats] = []
    private(set) var recentSolves: [Solve] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let streakTask = StreakService.me()
            async let statsTask = SolveService.overallStats()
            async let subjectsTask = WrongAnswerService.stats()
            async let solvesTask = SolveService.myHistory()
            let (s, o, sub, sv) = try await (streakTask, statsTask, subjectsTask, solvesTask)
            streak = s
            overallStats = o
            subjectStats = sub
            // 최근 10건만, 오래된 순으로 그래프 표시
            recentSolves = sv
                .sorted { $0.solvedAt < $1.solvedAt }
                .suffix(10)
                .map { $0 }
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
```

### `InsightsView.swift`

```swift
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
```

## Validation

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

Swift Charts 는 iOS 16+ 시스템 framework — `import Charts` 외 의존성/SPM 추가 불필요.

## 금지사항

- 외부 차트 라이브러리(`SwiftUICharts`, `Charts-iOS` 등) SPM 추가 금지. 이유: Swift Charts 가 시스템 표준 + 디자인 토큰 친화. 외부 의존성은 빌드 시간/유지보수 비용.
- 데이터 없을 때 빈 차트 영역 노출 금지. 이유: 의미 없는 빈 박스는 정보 밀도 떨어뜨림. `if !viewModel.recentSolves.isEmpty` 가드.
- `Chart` 안에서 임의의 색(`Color.red` 등) 사용 금지. 이유: 디자인 토큰만 사용. 차트 색상은 `barColor(for:)` 같은 토큰 기반 헬퍼.
- `recentSolves` 를 10건 이상 표시 금지. 이유: 좁은 모바일 화면에서 추이 가독성. 10건이 적절한 최근 트렌드 윈도우.
