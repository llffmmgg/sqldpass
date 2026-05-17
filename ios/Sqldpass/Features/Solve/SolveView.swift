import SwiftUI

struct SolveView: View {
    @State var viewModel: SolveViewModel
    @State private var showExitConfirm = false
    @Environment(\.dismiss) private var dismiss

    /// 제출 성공 후 결과 화면 푸시 트리거 (Step 5 에서 wiring)
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
                        OMRAnswerGrid(
                            question: question,
                            chosen: viewModel.currentEntry?.chosenAnswer,
                            onSelect: { viewModel.select($0) }
                        )
                        if viewModel.currentEntry?.chosenAnswer != nil {
                            Button {
                                viewModel.clearAnswer()
                            } label: {
                                Label("선택 지우기", systemImage: "arrow.uturn.backward")
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
                            Image(systemName: viewModel.currentEntry?.markedForReview == true
                                  ? "flag.fill" : "flag")
                                .foregroundStyle(viewModel.currentEntry?.markedForReview == true
                                                 ? Color.semanticWarning : Color.appTextSubtle)
                        }
                        .accessibilityLabel("다시 보기 표시 토글")
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
            Text("지금까지 푼 답안은 저장되지 않습니다.")
        }
        .alert("제출 실패", isPresented: .constant(viewModel.errorMessage != nil), actions: {
            Button("확인", role: .cancel) { /* viewModel.errorMessage clearing handled implicitly on next submit */ }
        }, message: {
            Text(viewModel.errorMessage ?? "")
        })
        .onAppear {
            viewModel.start()
        }
    }
}
