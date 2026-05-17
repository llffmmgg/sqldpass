import SwiftUI

struct DashboardView: View {
    @State private var viewModel = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    welcomeHeader
                    streakCard
                    quickStatsRow

                    if let errorMessage = viewModel.errorMessage {
                        errorBanner(message: errorMessage)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
            .navigationTitle("홈")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.member == nil {
                    await viewModel.load()
                }
            }
            .overlay {
                if viewModel.isLoading && viewModel.member == nil {
                    ProgressView()
                        .controlSize(.large)
                }
            }
        }
    }

    // MARK: - Sections

    private var welcomeHeader: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("안녕하세요 👋")
                .font(AppType.callout)
                .foregroundStyle(Color.appTextMuted)
            Text(viewModel.member?.nickname.appending("님") ?? "오늘도 한 문제씩")
                .font(AppType.title)
                .foregroundStyle(Color.appTextPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var streakCard: some View {
        let current = viewModel.streak?.currentStreak
        let solvedToday = viewModel.streak?.solvedToday ?? false
        let longest = viewModel.streak?.longestStreak

        return VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: solvedToday ? "flame.fill" : "flame")
                    .foregroundStyle(solvedToday ? Color.semanticWarning : Color.appTextSubtle)
                Text("연속 학습")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer()
                Text(current.map { "\($0)일" } ?? "— 일")
                    .font(AppType.monoNumericLarge)
                    .foregroundStyle(Color.brandPrimary)
            }
            if let longest, longest > 0 {
                Text("최장 \(longest)일 · \(solvedToday ? "오늘 풀이 완료" : "오늘 한 문제 풀어보세요")")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            } else {
                Text("아직 푼 기록이 없어요")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var quickStatsRow: some View {
        HStack(spacing: Spacing.md) {
            statCard(
                label: "전체 평균 풀이",
                value: viewModel.stats.map { String(format: "%.1f", $0.avgDailyCount) } ?? "—",
                hint: "하루 평균",
                color: .brandPrimary
            )
            statCard(
                label: "내 연속",
                value: viewModel.streak.map { "\($0.currentStreak)" } ?? "—",
                hint: "현재 streak",
                color: .semanticInfo
            )
        }
    }

    private func statCard(label: String, value: String, hint: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(label)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
            Text(value)
                .font(AppType.monoNumericLarge)
                .foregroundStyle(color)
            Text(hint)
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private func errorBanner(message: String) -> some View {
        HStack(alignment: .top, spacing: Spacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color.semanticDanger)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text("불러오기 실패")
                    .font(AppType.bodyEmph)
                Text(message)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
            Spacer()
            Button("재시도") {
                Task { await viewModel.load() }
            }
            .font(AppType.footnote)
        }
        .padding(Spacing.base)
        .background(Color.semanticDanger.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.semanticDanger.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

#Preview {
    DashboardView()
}
