import SwiftUI

struct MockExamsListView: View {
    @State private var viewModel = MockExamsViewModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .background(Color.appPage)
                .navigationTitle("모의고사")
                .navigationBarTitleDisplayMode(.inline)
                .refreshable {
                    await viewModel.load()
                }
                .task {
                    if viewModel.exams.isEmpty {
                        await viewModel.load()
                    }
                }
                .navigationDestination(for: MockExamRoute.self) { route in
                    switch route {
                    case .detail(let examId):
                        MockExamDetailView(examId: examId, path: $path)
                    case .solve(let examId, let questions):
                        SolveView(
                            viewModel: SolveViewModel(mockExamId: examId, questions: questions),
                            onSubmitted: { result in
                                path.append(MockExamRoute.result(result: result, questions: questions))
                            }
                        )
                    case .result(let result, let questions):
                        SolveResultView(
                            result: result,
                            questions: questions,
                            onDone: {
                                path = NavigationPath()
                            }
                        )
                    }
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.exams.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.exams.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("다시 시도") {
                    Task { await viewModel.load() }
                }
            }
        } else if viewModel.exams.isEmpty {
            ContentUnavailableView(
                "모의고사가 없어요",
                systemImage: "doc.text",
                description: Text("등록된 시험이 생기면 여기에 표시됩니다.")
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    VStack(alignment: .leading, spacing: Spacing.xs) {
                        Text("모의고사")
                            .font(AppType.heading.weight(.bold))
                            .foregroundStyle(Color.appTextPrimary)
                        Text("실전 감각을 유지할 수 있게 한 회차씩 정리했어요.")
                            .font(AppType.callout)
                            .foregroundStyle(Color.appTextMuted)
                    }
                    .padding(.top, Spacing.base)

                    LazyVStack(spacing: Spacing.md) {
                        ForEach(viewModel.exams) { exam in
                            Button {
                                path.append(MockExamRoute.detail(examId: exam.id))
                            } label: {
                                ExamCard(exam: exam)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(Spacing.base)
            }
        }
    }
}

private struct ExamCard: View {
    let exam: MockExamSummary

    private var locked: Bool {
        exam.isPremium && !exam.purchased
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(alignment: .center, spacing: Spacing.sm) {
                Text(exam.typeLabel)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(exam.typeAccentColor)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .background(exam.typeAccentColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: Radius.full))

                Text("No.\(String(format: "%02d", exam.sequence))")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .background(Color.appElevated)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.full))

                if exam.isPastExam {
                    Text("기출")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.semanticInfo)
                }

                Spacer()

                if locked {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(Color.semanticWarning)
                } else if exam.expertVerified {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(Color.brandPrimary)
                }
            }

            Text(exam.name)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: Spacing.md) {
                Label("\(exam.totalQuestions)문제", systemImage: "list.number")
                if let label = exam.difficultyLabel {
                    Label(label, systemImage: "speedometer")
                }
                Spacer()
                if let best = exam.bestScoreLabel {
                    Text(best)
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                }
            }
            .font(AppType.footnote)
            .foregroundStyle(Color.appTextMuted)

            HStack {
                Spacer()
                Label(locked ? "프리미엄 필요" : "응시하기", systemImage: locked ? "lock.fill" : "play.fill")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.brandPrimaryFG)
                Spacer()
            }
            .frame(height: 48)
            .background(locked ? Color.appTextSubtle.opacity(0.28) : Color.brandPrimary)
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(locked ? Color.appElevated : Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(locked ? Color.appBorderStrong : Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

#Preview {
    MockExamsListView()
}
