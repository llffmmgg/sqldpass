import SwiftUI

struct MockExamsListView: View {
    @State private var viewModel = MockExamsViewModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .background(Color.appPage)
                .navigationTitle("모의고사")
                .navigationBarTitleDisplayMode(.large)
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
                                path = NavigationPath() // 모의고사 목록으로 복귀
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
                Button("재시도") {
                    Task { await viewModel.load() }
                }
            }
        } else if viewModel.exams.isEmpty {
            ContentUnavailableView(
                "모의고사가 없어요",
                systemImage: "doc.text",
                description: Text("관리자가 모의고사를 등록하면 여기에 표시됩니다")
            )
        } else {
            ScrollView {
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
                .padding(Spacing.base)
            }
        }
    }
}

private struct ExamCard: View {
    let exam: MockExamSummary

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                // 종류 chip
                Text(exam.typeLabel)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(exam.typeAccentColor)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.full)
                            .stroke(exam.typeAccentColor.opacity(0.5), lineWidth: 1)
                    )

                if exam.isPastExam {
                    Text("기출")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.semanticInfo)
                        .padding(.horizontal, Spacing.sm)
                        .padding(.vertical, Spacing.xxs)
                        .background(Color.semanticInfo.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: Radius.full))
                }

                if exam.expertVerified {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.caption)
                        .foregroundStyle(Color.brandPrimary)
                }

                Spacer()

                if exam.isPremium && !exam.purchased {
                    Image(systemName: "lock.fill")
                        .font(.callout)
                        .foregroundStyle(Color.semanticWarning)
                }
            }

            Text(exam.name)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: Spacing.md) {
                Label("\(exam.totalQuestions)문제", systemImage: "list.number")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)

                if let label = exam.difficultyLabel {
                    Label(label, systemImage: "speedometer")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }

                Spacer()

                if let best = exam.bestScoreLabel {
                    Text(best)
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(exam.solved ? Color.brandPrimary : Color.appTextMuted)
                }
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
    MockExamsListView()
}
