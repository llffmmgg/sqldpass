import SwiftUI

struct WrongAnswersView: View {
    @State private var viewModel = WrongAnswersViewModel()
    @State private var retryTarget: WrongAnswer?

    var body: some View {
        NavigationStack {
            content
                .background(Color.appPage)
                .navigationTitle("오답")
                .navigationBarTitleDisplayMode(.large)
                .refreshable {
                    await viewModel.load()
                }
                .task {
                    if viewModel.items.isEmpty {
                        await viewModel.load()
                    }
                }
                .sheet(item: $retryTarget) { target in
                    WrongAnswerRetrySheet(
                        item: target,
                        onMastered: {
                            viewModel.markMastered(questionId: target.questionId)
                            retryTarget = nil
                        }
                    )
                    .presentationDetents([.medium, .large])
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.items.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ContentUnavailableView(
                "오답이 없어요",
                systemImage: "checkmark.seal",
                description: Text("틀린 문제가 생기면 여기서 다시 풀 수 있어요")
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    if !viewModel.stats.isEmpty {
                        statsSection
                    }
                    if !viewModel.items.isEmpty {
                        itemsSection
                    }
                }
                .padding(Spacing.base)
            }
        }
    }

    private var statsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("과목별 오답률")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.md) {
                    ForEach(viewModel.stats) { stat in
                        StatPill(stat: stat)
                    }
                }
            }
        }
    }

    private var itemsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("오답 문항 \(viewModel.items.count)개")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            ForEach(viewModel.items) { item in
                Button {
                    retryTarget = item
                } label: {
                    WrongAnswerCard(item: item)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct StatPill: View {
    let stat: WrongAnswerStats

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(stat.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            Text("\(stat.wrongRate)%")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(rateColor)
            Text("\(stat.wrongCount) / \(stat.totalSolved)")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
        }
        .padding(Spacing.md)
        .frame(width: 120, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var rateColor: Color {
        if stat.wrongRate >= 50 { return .semanticDanger }
        if stat.wrongRate >= 25 { return .semanticWarning }
        return .brandPrimary
    }
}

private struct WrongAnswerCard: View {
    let item: WrongAnswer

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(item.subjectName)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.brandPrimary)
                Text(item.questionContent)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            Spacer()
            VStack(spacing: Spacing.xxs) {
                Text("\(item.wrongCount)")
                    .font(AppType.monoNumericLarge.weight(.bold))
                    .foregroundStyle(Color.semanticDanger)
                Text("회 틀림")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
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
}

#Preview {
    WrongAnswersView()
}
