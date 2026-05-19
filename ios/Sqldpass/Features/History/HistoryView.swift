import SwiftUI

struct HistoryView: View {
    @State private var viewModel = HistoryViewModel()

    var body: some View {
        // ProfileView 가 이미 NavigationStack 안에 있어 본 화면은 nested stack 을 피한다.
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
            .navigationDestination(for: SolveSummary.self) { summary in
                HistoryDetailView(summary: summary)
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
                    ForEach(viewModel.solves) { summary in
                        NavigationLink(value: summary) {
                            HistoryCard(summary: summary)
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
    let summary: SolveSummary

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
                Label("\(summary.correctCount) / \(summary.totalCount)", systemImage: "checkmark.circle")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                kindBadge
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

    private var scoreLabel: String { "\(summary.score)" }

    private var scoreColor: Color {
        if summary.score >= 80 { return .brandPrimary }
        if summary.score >= 60 { return .semanticInfo }
        return .semanticDanger
    }

    @ViewBuilder
    private var kindBadge: some View {
        if summary.mockExamId != nil {
            Label("모의고사", systemImage: "doc.text")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
        } else if summary.subjectId != nil {
            Label("실전 문제", systemImage: "play.circle")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.semanticInfo)
        }
    }

    private var formattedDate: String {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: summary.solvedAt)
            ?? ISO8601DateFormatter().date(from: summary.solvedAt)
        guard let date else { return summary.solvedAt }
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
