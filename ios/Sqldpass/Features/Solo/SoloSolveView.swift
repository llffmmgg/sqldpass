import SwiftUI

/// 단일 채점 풀이 화면 — 웹 frontend/src/app/solve/SolveClient.tsx phase="solve" 와 동치.
///
/// `SoloSolveViewModel(subjectId:subjectName:)` 인스턴스를 받아 push.
/// 시스템 NavigationBar 는 숨기고 자체 `SoloProgressHeader` 가 상단을 채운다.
struct SoloSolveView: View {
    @State var viewModel: SoloSolveViewModel
    @State private var showExitConfirm = false
    @State private var bookmarkedIds: Set<Int64> = []
    @State private var reportSubmitting = false
    @Environment(\.dismiss) private var dismiss

    /// 오프라인 큐 — `@Observable` SolveQueue 의 pendingCount 를 view 에서 관찰.
    private let solveQueue = SolveQueue.shared

    var body: some View {
        Group {
            if let err = viewModel.fatalError {
                FatalErrorState(message: err, onDismiss: { dismiss() })
            } else if viewModel.sessionComplete {
                SessionCompleteCard(
                    subjectName: viewModel.subjectName,
                    solvedCount: viewModel.solvedCount,
                    correctCount: viewModel.correctCount,
                    onReplaySame: { viewModel.replaySame() },
                    onNewRandom: { Task { await viewModel.newRandom() } },
                    onExit: { dismiss() }
                )
            } else if viewModel.current == nil {
                LoadingState()
            } else {
                content
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await viewModel.start() }
        .onChange(of: viewModel.revealed) { _, isRevealed in
            guard isRevealed else { return }
            if viewModel.isCorrect {
                Haptics.success()
            } else {
                Haptics.warning()
            }
        }
        .confirmationDialog(
            "지금까지의 진행은 저장되지 않습니다.",
            isPresented: $showExitConfirm,
            titleVisibility: .visible
        ) {
            Button("종료하기", role: .destructive) { dismiss() }
            Button("계속 풀기", role: .cancel) {}
        }
    }

    @ViewBuilder
    private var content: some View {
        let current = viewModel.current!
        let parsed = QuestionParser.parse(current.content)
        let isMcq = !(current.questionType.uppercased().contains("SHORT")
                      || current.questionType.uppercased().contains("DESCRIPTIVE")
                      || current.questionType.uppercased().contains("TEXT"))

        VStack(spacing: 0) {
            SoloProgressHeader(
                solvedCount: viewModel.solvedCount,
                totalCount: SoloSolveViewModel.setSize,
                correctCount: viewModel.correctCount,
                isBookmarked: bookmarkedIds.contains(current.id),
                onClose: { showExitConfirm = true },
                onToggleBookmark: { toggleBookmark(current.id) },
                onReport: { report(questionId: current.id) }
            )

            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.base) {
                    // 오프라인 큐 인디케이터 (count > 0 일 때만)
                    OfflineQueueChip(count: solveQueue.pendingCount)

                    // 과목 라벨
                    Text(viewModel.subjectName)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.brandPrimary)

                    // 문제 본문 카드
                    QuestionBodyCard(content: isMcq ? parsed.body.isEmpty ? current.content : parsed.body
                                                    : current.content)

                    if isMcq {
                        let displayCount = parsed.options.isEmpty ? 4 : parsed.options.count
                        VStack(spacing: Spacing.sm) {
                            ForEach(1...displayCount, id: \.self) { num in
                                let text = parsed.options.indices.contains(num - 1)
                                    ? parsed.options[num - 1] : nil
                                SolveOptionRow(
                                    optionNumber: num,
                                    optionText: text,
                                    selected: viewModel.selectedOption == num,
                                    revealed: viewModel.revealed,
                                    isCorrectOption: viewModel.detail?.correctOption == num,
                                    onTap: {
                                        Haptics.light()
                                        viewModel.selectOption(num)
                                    },
                                    onDoubleTap: {
                                        Haptics.medium()
                                        viewModel.selectOption(num)
                                        Task { await viewModel.submit() }
                                    }
                                )
                            }
                        }
                    } else {
                        ShortAnswerInput(
                            value: Binding(
                                get: { viewModel.answerText },
                                set: { viewModel.setAnswerText($0) }
                            ),
                            disabled: viewModel.revealed,
                            onImeSubmit: { Task { await viewModel.submit() } }
                        )
                    }

                    if viewModel.revealed, let detail = viewModel.detail {
                        SoloExplanationCard(detail: detail, isCorrect: viewModel.isCorrect)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                    }

                    if let err = viewModel.submitError {
                        Text("풀이 기록 저장 실패 — \(err)")
                            .font(AppType.caption)
                            .foregroundStyle(Color.semanticWarning)
                    }
                }
                .padding(Spacing.lg)
                .animation(.easeOut(duration: 0.25), value: viewModel.revealed)
            }
            .background(Color.appPage)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            SoloBottomActionBar(
                revealed: viewModel.revealed,
                hasAnswer: viewModel.hasAnswer,
                submitting: viewModel.submitting,
                isLastBeforeComplete: viewModel.isLastBeforeComplete,
                onSubmit: { Task { await viewModel.submit() } },
                onNext: { Task { await viewModel.goNext() } }
            )
        }
        .background(Color.appPage.ignoresSafeArea())
    }

    private func toggleBookmark(_ questionId: Int64) {
        let wasBookmarked = bookmarkedIds.contains(questionId)
        if wasBookmarked { bookmarkedIds.remove(questionId) } else { bookmarkedIds.insert(questionId) }
        Haptics.light()
        Task {
            do {
                if wasBookmarked {
                    try await BookmarkService.remove(questionId: questionId)
                } else {
                    try await BookmarkService.add(questionId: questionId)
                }
            } catch {
                // revert
                if wasBookmarked { bookmarkedIds.insert(questionId) }
                else { bookmarkedIds.remove(questionId) }
            }
        }
    }

    private func report(questionId: Int64) {
        guard !reportSubmitting else { return }
        reportSubmitting = true
        // FeedbackService 가 별 phase 에 있을 수 있어서 본 step 은 단순 placeholder.
        // 실제 신고 흐름은 별 phase 에서 작업.
        _ = questionId
        reportSubmitting = false
    }
}

private struct QuestionBodyCard: View {
    let content: String
    var body: some View {
        VStack(alignment: .leading) {
            QuestionContentView(text: content)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.base)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

private struct ShortAnswerInput: View {
    @Binding var value: String
    let disabled: Bool
    let onImeSubmit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("답안 입력")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            TextField("답안을 입력하세요", text: $value, axis: .vertical)
                .lineLimit(3...8)
                .padding(Spacing.md)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.md)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                .disabled(disabled)
                .submitLabel(.done)
                .onSubmit { onImeSubmit() }
            Text("대소문자·앞뒤 공백은 자동으로 무시됩니다.")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
        }
    }
}

private struct LoadingState: View {
    var body: some View {
        VStack {
            ProgressView()
            Text("문제를 불러오는 중…")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .padding(.top, Spacing.sm)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appPage.ignoresSafeArea())
    }
}

private struct FatalErrorState: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 36))
                .foregroundStyle(Color.semanticWarning)
            Text("문제를 가져올 수 없습니다")
                .font(AppType.bodyEmph)
            Text(message)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .multilineTextAlignment(.center)
            Button("닫기", action: onDismiss)
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appPage.ignoresSafeArea())
    }
}

private struct SessionCompleteCard: View {
    let subjectName: String
    let solvedCount: Int
    let correctCount: Int
    let onReplaySame: () -> Void
    let onNewRandom: () -> Void
    let onExit: () -> Void

    private var rate: Int {
        let total = max(solvedCount, 1)
        return correctCount * 100 / total
    }
    private var rateColor: Color {
        switch rate {
        case 90...: return .semanticSuccess
        case 70..<90: return .semanticWarning
        default: return .semanticDanger
        }
    }
    private var message: String {
        switch rate {
        case 90...: return "완벽해요! 같은 과목을 더 풀어볼까요?"
        case 70..<90: return "잘하고 있어요. 한 세트 더 풀면 손에 더 익을 거예요."
        default: return "괜찮아요. 약한 문제부터 다시 한 번 풀어보세요."
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.base) {
            Text("세션 완료")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.brandPrimary)
            Text(subjectName)
                .font(AppType.title)
                .foregroundStyle(Color.appTextPrimary)

            HStack(alignment: .lastTextBaseline, spacing: Spacing.xs) {
                Text("\(correctCount)")
                    .font(AppType.display.monospacedDigit())
                    .foregroundStyle(rateColor)
                Text("/ \(max(solvedCount, 1))")
                    .font(AppType.heading.monospacedDigit())
                    .foregroundStyle(Color.appTextMuted)
                Text("\(rate)%")
                    .font(AppType.heading.monospacedDigit())
                    .foregroundStyle(rateColor)
                    .padding(.leading, Spacing.sm)
            }

            Text(message)
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)

            Spacer()

            VStack(spacing: Spacing.sm) {
                Button(action: onReplaySame) {
                    Text("같은 10문제 다시")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .buttonStyle(.bordered)
                .tint(Color.brandPrimary)

                Button(action: onNewRandom) {
                    Text("새 10문제")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)

                Button(action: onExit) {
                    Text("다른 과목 선택")
                        .font(AppType.body)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Color.appTextMuted)
            }
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color.appPage.ignoresSafeArea())
    }
}
