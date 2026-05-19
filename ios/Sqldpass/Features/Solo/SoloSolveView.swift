import SwiftUI

/// 단일 채점 풀이 화면 — 웹 frontend/src/app/solve/SolveClient.tsx phase="solve" 와 동치.
///
/// `SoloSolveViewModel(subjectId:subjectName:)` 인스턴스를 받아 push.
/// 시스템 NavigationBar 는 숨기고 자체 헤더(`AppProgressPill` 기반)가 상단을 채운다.
struct SoloSolveView: View {
    @State var viewModel: SoloSolveViewModel
    @State private var showExitConfirm = false
    @State private var bookmarkedIds: Set<Int64> = []
    @State private var reportingQuestionId: Int64?
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
        .sheet(item: Binding(
            get: { reportingQuestionId.map(ReportTarget.init) },
            set: { reportingQuestionId = $0?.questionId }
        )) { target in
            FeedbackComposeView(initialType: .questionError, questionId: target.questionId)
        }
    }

    /// `.sheet(item:)` 가 `Identifiable` 을 요구해 Int64 만으로는 부족 — 얇은 래퍼.
    private struct ReportTarget: Identifiable {
        let questionId: Int64
        var id: Int64 { questionId }
    }

    @ViewBuilder
    private var content: some View {
        let current = viewModel.current!
        let parsed = QuestionParser.parse(current.content)
        let isMcq = !(current.questionType.uppercased().contains("SHORT")
                      || current.questionType.uppercased().contains("DESCRIPTIVE")
                      || current.questionType.uppercased().contains("TEXT"))

        VStack(spacing: 0) {
            // 헤더: 닫기 + 진행률 알약 + 북마크 + 신고
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

                AppProgressPill(
                    current: min(viewModel.solvedCount + 1, SoloSolveViewModel.setSize),
                    total: SoloSolveViewModel.setSize,
                    label: nil
                )
                .frame(maxWidth: .infinity)

                Button {
                    toggleBookmark(current.id)
                } label: {
                    Image(systemName: bookmarkedIds.contains(current.id) ? "bookmark.fill" : "bookmark")
                        .foregroundStyle(bookmarkedIds.contains(current.id) ? Color.brandPrimary : Color.appTextMuted)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(bookmarkedIds.contains(current.id) ? "즐겨찾기 해제" : "즐겨찾기")

                Button {
                    report(questionId: current.id)
                } label: {
                    Image(systemName: "ellipsis")
                        .rotationEffect(.degrees(90))
                        .foregroundStyle(Color.appTextMuted)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("메뉴")
            }
            .padding(.horizontal, Spacing.base)
            .padding(.vertical, Spacing.sm)
            .background(Color.appSurface)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Color.appBorder).frame(height: 1)
            }

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
                                AppOptionRow(
                                    optionNumber: num,
                                    optionText: text,
                                    state: AppOptionRow.appOptionStateOf(
                                        selected: viewModel.selectedOption == num,
                                        revealed: viewModel.revealed,
                                        isCorrectOption: viewModel.detail?.correctOption == num
                                    ),
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
            if viewModel.revealed {
                AppBottomActionBar(
                    primary: BottomAction(
                        title: viewModel.isLastBeforeComplete ? "결과 보기" : "다음 문제",
                        action: { Task { await viewModel.goNext() } },
                        isEnabled: true,
                        isLoading: false,
                        variant: .primary
                    )
                )
            } else {
                AppBottomActionBar(
                    primary: BottomAction(
                        title: "정답 확인",
                        action: { Task { await viewModel.submit() } },
                        isEnabled: viewModel.hasAnswer,
                        isLoading: viewModel.submitting,
                        variant: .primary
                    )
                )
            }
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

    /// 더보기 메뉴 → 문제 신고. FeedbackComposeView 를 sheet 로 띄워 백엔드 `/api/feedback` 로 전송.
    private func report(questionId: Int64) {
        reportingQuestionId = questionId
    }
}

private struct QuestionBodyCard: View {
    let content: String
    var body: some View {
        AppCard(surface: .card) {
            QuestionContentView(text: content)
        }
    }
}

private struct ShortAnswerInput: View {
    @Binding var value: String
    let disabled: Bool
    let onImeSubmit: () -> Void

    var body: some View {
        AppTextField(
            text: $value,
            label: "답안 입력",
            placeholder: "답안을 입력하세요",
            helper: "대소문자·앞뒤 공백은 자동으로 무시됩니다.",
            isEnabled: !disabled,
            isSecure: false,
            keyboardType: .default,
            leadingSystemImage: nil,
            onSubmit: onImeSubmit
        )
    }
}

private struct LoadingState: View {
    var body: some View {
        AppStateView(state: .loading)
            .background(Color.appPage.ignoresSafeArea())
    }
}

private struct FatalErrorState: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: Spacing.md) {
            AppMascot(pose: .review, sizeDp: 96)
            Text("문제를 가져올 수 없습니다")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Text(message)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .multilineTextAlignment(.center)
            AppButton(title: "닫기", variant: .secondary, action: onDismiss)
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

    private var total: Int { max(solvedCount, 1) }
    private var rate: Int { correctCount * 100 / total }
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
        VStack(spacing: Spacing.lg) {
            AppSectionHeader(title: subjectName, eyebrow: "세션 완료")

            HStack(spacing: Spacing.md) {
                AppNumberCell(value: "\(correctCount)",
                              label: "맞힌 문제",
                              unit: "/\(total)",
                              accent: rateColor,
                              size: .display)
                    .frame(maxWidth: .infinity)
                AppNumberCell(value: "\(rate)%",
                              label: "정답률",
                              accent: rateColor,
                              size: .display)
                    .frame(maxWidth: .infinity)
            }

            AppMascot(pose: .celebrate, sizeDp: 88)

            Text(message)
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
                .multilineTextAlignment(.center)

            Spacer()

            VStack(spacing: Spacing.sm) {
                AppButton(title: "새 10문제", variant: .primary, action: onNewRandom)
                AppButton(title: "같은 10문제 다시", variant: .secondary, action: onReplaySame)
                AppButton(title: "다른 과목 선택", variant: .tertiary, action: onExit)
            }
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color.appPage.ignoresSafeArea())
    }
}
