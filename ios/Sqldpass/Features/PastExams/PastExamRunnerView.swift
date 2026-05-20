import SwiftUI

/// 기출 회차 풀이 화면. SolveView 와 동일한 chrome 을 사용하지만 채점이 백엔드에서
/// 일괄로 일어나며(`POST /api/public/past-exams/{id}/grade`) 응답이 `Solve` 가 아닌
/// `PastExamGradeResponse` 라 result 분기가 다르다.
struct PastExamRunnerView: View {
    @State var viewModel: PastExamRunnerViewModel
    @State private var showExitConfirm = false
    @Environment(\.dismiss) private var dismiss

    /// OMR 답안 카드용 — viewModel.answers 에서 응답이 있는 문항의 0-base 인덱스.
    private var answeredIndices: Set<Int> {
        var set = Set<Int>()
        for (i, q) in viewModel.questions.enumerated() {
            if viewModel.answers[q.id]?.isAnswered == true {
                set.insert(i)
            }
        }
        return set
    }

    var body: some View {
        Group {
            if let graded = viewModel.graded, let detail = viewModel.detail {
                PastExamResultView(
                    examName: detail.name,
                    graded: graded,
                    questions: detail.questions,
                    submittedAnswers: viewModel.answers,
                    onDone: { dismiss() }
                )
            } else if viewModel.detail == nil {
                loadingOrError
            } else {
                ZStack {
                    runner
                    if viewModel.startedAt == nil {
                        startOverlay
                            .transition(.opacity)
                    }
                }
                .animation(.easeOut(duration: 0.2), value: viewModel.startedAt)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await viewModel.load() }
        .alert(
            "제출 실패",
            isPresented: Binding(
                get: { viewModel.submitError != nil },
                set: { if !$0 { viewModel.dismissError() } }
            ),
            actions: {
                Button("확인", role: .cancel) { viewModel.dismissError() }
            },
            message: {
                Text(viewModel.submitError ?? "")
            }
        )
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
    }

    @ViewBuilder
    private var loadingOrError: some View {
        if viewModel.isLoading {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.appPage.ignoresSafeArea())
        } else if let loadError = viewModel.loadError {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(loadError)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
                Button("닫기", role: .cancel) { dismiss() }
            }
        }
    }

    /// 시작 버튼 — 사용자가 명시적으로 trigger. SolveView 패턴과 동일.
    private var startOverlay: some View {
        ZStack {
            Color.appPage.ignoresSafeArea()
            VStack(spacing: Spacing.lg) {
                Image(systemName: "timer")
                    .font(.system(size: 56, weight: .light))
                    .foregroundStyle(Color.brandPrimary)

                VStack(spacing: Spacing.xs) {
                    Text("준비되면 시작해주세요")
                        .font(AppType.heading)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("시작을 누르면 타이머가 동작합니다.")
                        .font(AppType.callout)
                        .foregroundStyle(Color.appTextMuted)
                        .multilineTextAlignment(.center)
                }

                Button {
                    viewModel.start()
                } label: {
                    Text("시작")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.brandPrimaryFG)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, Spacing.md)
                        .background(Color.brandPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, Spacing.xl)
                .padding(.top, Spacing.sm)
            }
        }
    }

    @ViewBuilder
    private var runner: some View {
        VStack(spacing: 0) {
            topBar

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
                                    .font(AppType.footnote.weight(.semibold))
                                    .foregroundStyle(Color.appTextMuted)
                            }
                            .buttonStyle(.plain)
                        }

                        SolveOMRSheet(
                            totalCount: viewModel.totalCount,
                            currentIndex: viewModel.currentIndex,
                            answeredIndices: answeredIndices,
                            onTap: { viewModel.go(to: $0) }
                        )
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            SolveActionBar(
                canGoPrevious: viewModel.currentIndex > 0,
                canGoNext: viewModel.currentIndex < viewModel.totalCount - 1,
                isLastQuestion: viewModel.currentIndex == viewModel.totalCount - 1,
                isSubmitting: viewModel.isSubmitting,
                onPrevious: { viewModel.goPrevious() },
                onNext: { viewModel.goNext() },
                onSubmit: {
                    Task { await viewModel.submit() }
                }
            )
        }
    }

    @ViewBuilder
    private var topBar: some View {
        HStack(spacing: Spacing.sm) {
            Button {
                showExitConfirm = true
            } label: {
                Image(systemName: "xmark")
                    .font(.title3)
                    .foregroundStyle(Color.appTextPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("풀이 종료")

            SolveHeader(
                progress: viewModel.progress,
                currentIndex: viewModel.currentIndex,
                totalCount: viewModel.totalCount,
                answeredCount: viewModel.answeredCount,
                elapsedSeconds: viewModel.elapsedSeconds
            )
            .frame(maxWidth: .infinity, alignment: .leading)

            if let q = viewModel.currentQuestion {
                BookmarkToggleButton(questionId: q.id)
                Button {
                    viewModel.toggleMark()
                } label: {
                    Image(systemName: viewModel.currentEntry?.markedForReview == true ? "flag.fill" : "flag")
                        .font(AppType.body)
                        .foregroundStyle(
                            viewModel.currentEntry?.markedForReview == true
                            ? Color.semanticWarning
                            : Color.appTextMuted
                        )
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("다시 볼 문제로 표시")
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.sm)
        .background(Color.appSurface)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }

    @ViewBuilder
    private func answerInput(for question: MockExamQuestionItem) -> some View {
        if question.isTextAnswerType {
            AppTextField(
                text: Binding(
                    get: { viewModel.currentEntry?.answerText ?? "" },
                    set: { viewModel.updateAnswerText($0) }
                ),
                label: "답안 입력",
                placeholder: "답안을 입력하세요",
                helper: "대소문자·앞뒤 공백은 자동으로 무시됩니다."
            )
        } else {
            OMRAnswerGrid(
                question: question,
                selectedOption: viewModel.currentEntry?.selectedOption,
                onSelect: { viewModel.select(option: $0) },
                onAdvance: {
                    if viewModel.currentIndex < viewModel.totalCount - 1 {
                        viewModel.goNext()
                    }
                }
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
