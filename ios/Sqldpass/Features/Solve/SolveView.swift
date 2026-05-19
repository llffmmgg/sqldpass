import SwiftUI

/// 모의고사 풀이 화면 (Inked OMR 디자인 시스템).
///
/// 자체 헤더(닫기 / 진행 알약 / 북마크 / 다시보기 플래그) + 본문 + 하단 액션바 로 구성.
/// 시스템 NavigationBar 는 숨기고 자체 chrome 으로 대체한다.
struct SolveView: View {
    @State var viewModel: SolveViewModel
    @State private var showExitConfirm = false
    @Environment(\.dismiss) private var dismiss

    var onSubmitted: ((Solve) -> Void)?

    /// 오프라인 큐 — `@Observable` SolveQueue 의 pendingCount 를 view 에서 관찰.
    private let solveQueue = SolveQueue.shared

    var body: some View {
        VStack(spacing: 0) {
            topBar

            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    // 오프라인 큐 인디케이터 (count > 0 일 때만)
                    OfflineQueueChip(count: solveQueue.pendingCount)

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
                    } else {
                        Text("문제가 없습니다")
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
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
        .alert(
            "오프라인 제출",
            isPresented: Binding(
                get: { viewModel.offlineSubmitted },
                set: { if !$0 { viewModel.acknowledgeOfflineSubmitted() } }
            ),
            actions: {
                Button("확인", role: .cancel) {
                    viewModel.acknowledgeOfflineSubmitted()
                    dismiss()
                }
            },
            message: {
                Text("네트워크가 불안정해 답안을 기기에 보관했어요. 인터넷이 돌아오면 자동으로 전송돼 학습 기록에 추가됩니다.")
            }
        )
        .onAppear {
            viewModel.start()
        }
    }

    // MARK: - Top bar

    /// QuizScreen (screens.jsx ~492-623) 패턴:
    ///   row1: [X 닫기] [중앙 카운터+타이머] [북마크] [Flag]
    ///   row2: AppSegmentedProgress (N 셀)
    ///   meta: [문제 번호] ... [과목명 칩]
    @ViewBuilder
    private var topBar: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
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

            if let q = viewModel.currentQuestion {
                metaStrip(question: q)
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

    /// 헤더 하단 meta 줄: 좌측에 "문제 N", 우측에 과목명 칩.
    /// CertBadge 는 모의고사 컨텍스트에서 자격증 정보가 없어 표시하지 않는다.
    @ViewBuilder
    private func metaStrip(question: MockExamQuestionItem) -> some View {
        HStack(spacing: Spacing.sm) {
            Text("문제 \(question.displayOrder)")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)

            Spacer(minLength: Spacing.sm)

            Text(question.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
                .padding(.horizontal, Spacing.sm + 1)
                .padding(.vertical, 3)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.full, style: .continuous)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
        }
    }

    // MARK: - Answer inputs

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
