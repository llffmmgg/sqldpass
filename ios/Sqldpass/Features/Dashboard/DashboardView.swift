import SwiftUI

struct DashboardView: View {
    @State private var viewModel = DashboardViewModel()

    private var nickname: String {
        viewModel.member?.nickname ?? "학습자"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    AppHeroHeader(
                        eyebrow: "문어CBT",
                        title: "\(nickname)님의 오늘 학습",
                        subtitle: heroSubtitle
                    ) {
                        HStack(spacing: Spacing.md) {
                            Image(systemName: "bell")
                            Image(systemName: "person.crop.circle")
                        }
                        .font(.title3)
                        .foregroundStyle(Color.brandPrimaryFG.opacity(0.92))
                    }

                    AppSheet {
                        checkInPanel
                        statsGrid
                        quickStartSection

                        if let errorMessage = viewModel.errorMessage {
                            errorBanner(message: errorMessage)
                        }
                    }
                    .offset(y: -Spacing.base)
                    .padding(.bottom, -Spacing.base)
                }
            }
            .ignoresSafeArea(edges: .top)
            .background(Color.appPage)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
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

    private var heroSubtitle: String {
        if viewModel.streak?.solvedToday == true {
            return "오늘 학습 완료. 내일도 같은 시간에 이어가면 좋아요."
        }
        return "짧게라도 한 세트를 풀고 연속 학습을 이어가세요."
    }

    private var checkInPanel: some View {
        AppPanel {
            HStack(spacing: Spacing.md) {
                Image(systemName: viewModel.streak?.solvedToday == true ? "checkmark.seal.fill" : "sparkles")
                    .font(.title2)
                    .foregroundStyle(Color.brandPrimary)
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(viewModel.streak?.solvedToday == true ? "오늘 출석 완료" : "출석체크하고 기록 쌓기")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("연속 학습 \(viewModel.streak?.currentStreak ?? 0)일")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }
                Spacer()
            }
        }
        .background(
            RoundedRectangle(cornerRadius: Radius.lg)
                .fill(Color.brandPrimary.opacity(0.08))
        )
    }

    private var statsGrid: some View {
        HStack(spacing: Spacing.md) {
            MetricTile(
                title: "일 평균",
                value: viewModel.stats.map { String(format: "%.1f", $0.avgDailyCount) } ?? "-",
                caption: "문제",
                icon: "chart.bar.fill",
                color: .brandPrimary
            )
            MetricTile(
                title: "최장 기록",
                value: viewModel.streak.map { "\($0.longestStreak)" } ?? "-",
                caption: "일",
                icon: "trophy.fill",
                color: .semanticWarning
            )
        }
    }

    private var quickStartSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            AppSectionHeader(title: "바로 시작")
            HStack(spacing: Spacing.md) {
                NavigationLink {
                    MockExamsListView()
                } label: {
                    QuickActionCard(title: "모의고사", subtitle: "실전처럼 풀기", icon: "doc.text.fill")
                }
                .buttonStyle(.plain)

                NavigationLink {
                    WrongAnswersView()
                } label: {
                    QuickActionCard(title: "오답노트", subtitle: "틀린 문제 복습", icon: "arrow.uturn.backward.circle.fill")
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func errorBanner(message: String) -> some View {
        AppPanel {
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
                Button("다시") {
                    Task { await viewModel.load() }
                }
                .font(AppType.footnote)
            }
        }
    }
}

private struct QuickActionCard: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        AppPanel {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(Color.brandPrimary)
            Text(title)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Text(subtitle)
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
        }
    }
}

#Preview {
    DashboardView()
}
