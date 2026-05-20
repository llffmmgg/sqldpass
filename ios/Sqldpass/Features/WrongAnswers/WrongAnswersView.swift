import SwiftUI

struct WrongAnswersView: View {
    @State private var viewModel = WrongAnswersViewModel()
    @State private var retryTarget: WrongAnswer?
    @State private var showPaywall = false

    var body: some View {
        NavigationStack {
            content
                .background(Color.appPage)
                .navigationTitle("오답노트")
                .navigationBarTitleDisplayMode(.inline)
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
                .sheet(isPresented: $showPaywall) {
                    PaywallView()
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if viewModel.isLocked {
            lockedView
        } else if let errorMessage = viewModel.errorMessage, viewModel.items.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("다시 시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ContentUnavailableView(
                "오답이 없어요",
                systemImage: "checkmark.seal",
                description: Text("틀린 문제가 생기면 여기에 다시 풀 수 있게 모아둘게요.")
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    VStack(alignment: .leading, spacing: Spacing.xs) {
                        Text("오답노트")
                            .font(AppType.heading.weight(.bold))
                            .foregroundStyle(Color.appTextPrimary)
                        Text("가장 최근에 틀린 문제부터 다시 풀어볼 수 있어요.")
                            .font(AppType.callout)
                            .foregroundStyle(Color.appTextMuted)
                    }
                    .padding(.top, Spacing.base)

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

    /// 401/403 응답 — 플랜 미가입 사용자에게 잠금 안내 + 결제 화면 진입 CTA.
    private var lockedView: some View {
        ContentUnavailableView {
            Label("플랜 전용 기능이에요", systemImage: "lock.fill")
        } description: {
            Text("플랜에 가입하면 오답노트와 약점 영역 분석을 이용할 수 있어요.")
        } actions: {
            Button("플랜 보기") { showPaywall = true }
                .buttonStyle(.borderedProminent)
        }
    }

    private var statsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            AppSectionHeader(title: "과목별 오답률")
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
            AppSectionHeader(title: "오답 문제 \(viewModel.items.count)개")
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
        AppPanel {
            Text(stat.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(1)
            Text("\(stat.wrongRate)%")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(rateColor)
            Text("\(stat.wrongCount) / \(stat.totalSolved)")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
        }
        .frame(width: 132)
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
        AppPanel {
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
        }
    }
}

#Preview {
    WrongAnswersView()
}
