import SwiftUI

struct HistoryView: View {
    @State private var viewModel = HistoryViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationTitle("학습 기록")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.solves.isEmpty {
                    await viewModel.load()
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.solves.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.solves.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.solves.isEmpty {
            ContentUnavailableView(
                "아직 푼 기록이 없어요",
                systemImage: "list.bullet.rectangle.portrait",
                description: Text("모의고사를 풀면 기록이 여기 쌓입니다")
            )
        } else {
            ScrollView {
                LazyVStack(spacing: Spacing.md) {
                    ForEach(viewModel.solves) { solve in
                        NavigationLink {
                            SolveResultView(
                                result: solve,
                                questions: [],
                                onDone: {}
                            )
                        } label: {
                            HistoryCard(solve: solve)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(Spacing.base)
            }
        }
    }
}

private struct HistoryCard: View {
    let solve: Solve

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(alignment: .firstTextBaseline) {
                Text(scoreLabel)
                    .font(AppType.monoNumericLarge)
                    .foregroundStyle(scoreColor)
                Text("점")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
                Text(formattedDate)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            }
            HStack(spacing: Spacing.sm) {
                Label("\(solve.correctCount) / \(solve.totalCount)", systemImage: "checkmark.circle")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                if let streak = solve.currentStreak, streak > 0 {
                    Label("\(streak)일 연속", systemImage: "flame")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticWarning)
                }
                Spacer()
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

    private var scoreLabel: String { "\(solve.score)" }

    private var scoreColor: Color {
        if solve.score >= 80 { return .brandPrimary }
        if solve.score >= 60 { return .semanticInfo }
        return .semanticDanger
    }

    private var formattedDate: String {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: solve.solvedAt)
            ?? ISO8601DateFormatter().date(from: solve.solvedAt)
        guard let date else { return solve.solvedAt }
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "MM월 dd일 HH:mm"
        return f.string(from: date)
    }
}

#Preview {
    NavigationStack {
        HistoryView()
    }
}
