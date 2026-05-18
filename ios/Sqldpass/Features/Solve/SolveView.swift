import SwiftUI

struct SolveView: View {
    @State var viewModel: SolveViewModel
    @State private var showExitConfirm = false
    @Environment(\.dismiss) private var dismiss

    var onSubmitted: ((Solve) -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            SolveHeader(
                progress: viewModel.progress,
                currentIndex: viewModel.currentIndex,
                totalCount: viewModel.totalCount,
                answeredCount: viewModel.answeredCount,
                elapsedSeconds: viewModel.elapsedSeconds
            )
            .padding(.horizontal, Spacing.base)
            .padding(.vertical, Spacing.sm)
            .background(Color.appSurface)
            .overlay(alignment: .bottom) {
                Rectangle()
                    .fill(Color.appBorder)
                    .frame(height: 1)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    if let question = viewModel.currentQuestion {
                        QuestionBody(question: question)
                        answerInput(for: question)

                        if viewModel.currentEntry?.isAnswered == true {
                            Button {
                                viewModel.clearAnswer()
                            } label: {
                                Label("답안 지우기", systemImage: "arrow.uturn.backward")
                                    .font(AppType.footnote)
                            }
                            .buttonStyle(.borderless)
                            .foregroundStyle(Color.appTextMuted)
                        }
                    } else {
                        Text("문제가 없습니다")
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)

            SolveActionBar(
                canGoPrevious: viewModel.currentIndex > 0,
                canGoNext: viewModel.currentIndex < viewModel.totalCount - 1,
                isLastQuestion: viewModel.currentIndex == viewModel.totalCount - 1,
                isSubmitting: viewModel.isSubmitting,
                onPrevious: { viewModel.goPrevious() },
                onNext: { viewModel.goNext() },
                onSubmit: {
                    Task {
                        await viewModel.submit()
                        if let result = viewModel.submittedResult {
                            onSubmitted?(result)
                        }
                    }
                }
            )
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("종료") { showExitConfirm = true }
                    .foregroundStyle(Color.semanticDanger)
            }
            ToolbarItem(placement: .topBarTrailing) {
                if let q = viewModel.currentQuestion {
                    HStack(spacing: Spacing.sm) {
                        BookmarkToggleButton(questionId: q.id)
                        Button {
                            viewModel.toggleMark()
                        } label: {
                            Image(systemName: viewModel.currentEntry?.markedForReview == true ? "flag.fill" : "flag")
                                .foregroundStyle(
                                    viewModel.currentEntry?.markedForReview == true
                                    ? Color.semanticWarning
                                    : Color.appTextSubtle
                                )
                        }
                        .accessibilityLabel("다시 볼 문제로 표시")
                    }
                }
            }
        }
        .confirmationDialog(
            "정말 종료할까요?",
            isPresented: $showExitConfirm,
            titleVisibility: .visible
        ) {
            Button("종료하기", role: .destructive) {
                viewModel.stopTimer()
                dismiss()
            }
            Button("계속 풀기", role: .cancel) {}
        } message: {
            Text("지금까지의 답안은 저장되지 않습니다.")
        }
        .alert(
            "제출 실패",
            isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.dismissError() } }
            ),
            actions: {
                Button("확인", role: .cancel) { viewModel.dismissError() }
            },
            message: {
                Text(viewModel.errorMessage ?? "")
            }
        )
        .onAppear {
            viewModel.start()
        }
    }

    @ViewBuilder
    private func answerInput(for question: MockExamQuestionItem) -> some View {
        if question.isTextAnswerType {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("답안 입력")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                TextField(
                    "답안을 입력하세요",
                    text: Binding(
                        get: { viewModel.currentEntry?.answerText ?? "" },
                        set: { viewModel.updateAnswerText($0) }
                    ),
                    axis: .vertical
                )
                .textFieldStyle(.plain)
                .lineLimit(3...8)
                .padding(Spacing.md)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.md)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                .submitLabel(.done)
            }
        } else {
            OMRAnswerGrid(
                question: question,
                selectedOption: viewModel.currentEntry?.selectedOption,
                onSelect: { viewModel.select(option: $0) }
            )
        }
    }
}

private extension MockExamQuestionItem {
    var isTextAnswerType: Bool {
        let type = questionType.uppercased()
        return type.contains("SHORT") || type.contains("DESCRIPTIVE") || type.contains("TEXT")
    }
}
