import Foundation
import Observation

/// 풀이 세션 1 회분의 상태 + 액션.
///
/// 라이프사이클:
///   init(mockExamId:questions:) → start() → answer/navigation → submit() → result
///
/// View 는 `currentQuestion`, `progress`, `elapsedSeconds`, `selectedAnswer` 등을
/// 관찰하고 액션(`select`, `next`, `previous`, `toggleMark`, `submit`) 을 호출.
@Observable
final class SolveViewModel {
    // MARK: Inputs (불변)

    let mockExamId: Int64
    let questions: [MockExamQuestionItem]

    // MARK: State

    private(set) var currentIndex: Int = 0
    private(set) var answers: [Int64: SolveAnswerEntry] = [:]

    /// `start()` 호출 시점. 경과 시간 계산 기준.
    private(set) var startedAt: Date?

    /// 1 초마다 트리거되는 ticker. View 가 elapsedSeconds 를 다시 읽도록.
    private(set) var tickToken: Int = 0
    private var timerTask: Task<Void, Never>?

    // 제출 상태
    private(set) var isSubmitting = false
    private(set) var submittedResult: Solve?
    private(set) var errorMessage: String?

    // MARK: Derived

    var totalCount: Int { questions.count }

    var currentQuestion: MockExamQuestionItem? {
        guard questions.indices.contains(currentIndex) else { return nil }
        return questions[currentIndex]
    }

    var progress: Double {
        guard totalCount > 0 else { return 0 }
        return Double(currentIndex + 1) / Double(totalCount)
    }

    /// 답을 고른 문항 수
    var answeredCount: Int {
        answers.values.filter(\.isAnswered).count
    }

    /// 다시보기 표시된 문항 수
    var markedCount: Int {
        answers.values.filter { $0.markedForReview }.count
    }

    /// `start()` 이후 경과 초. 백그라운드 진입 후 복귀해도 정확함.
    var elapsedSeconds: Int {
        guard let startedAt else { return 0 }
        _ = tickToken // tick 변화에 의존성 등록
        return Int(Date().timeIntervalSince(startedAt))
    }

    /// 현재 문항의 답안 (있으면)
    var currentEntry: SolveAnswerEntry? {
        guard let q = currentQuestion else { return nil }
        return answers[q.id]
    }

    var canSubmit: Bool {
        !isSubmitting && submittedResult == nil && totalCount > 0
    }

    // MARK: Init

    init(mockExamId: Int64, questions: [MockExamQuestionItem]) {
        self.mockExamId = mockExamId
        self.questions = questions
    }

    deinit {
        timerTask?.cancel()
    }

    // MARK: Lifecycle

    func start() {
        guard startedAt == nil else { return }
        startedAt = Date()
        startTicker()
    }

    func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    private func startTicker() {
        timerTask?.cancel()
        timerTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await MainActor.run { [weak self] in
                    self?.tickToken &+= 1
                }
            }
        }
    }

    // MARK: Navigation

    func goNext() {
        guard currentIndex < totalCount - 1 else { return }
        currentIndex += 1
    }

    func goPrevious() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
    }

    func go(to index: Int) {
        guard questions.indices.contains(index) else { return }
        currentIndex = index
    }

    // MARK: Answer actions

    func select(option: Int) {
        guard let q = currentQuestion else { return }
        var entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
        entry.selectedOption = option
        entry.answerText = nil
        answers[q.id] = entry
    }

    func updateAnswerText(_ text: String) {
        guard let q = currentQuestion else { return }
        var entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
        entry.answerText = text
        entry.selectedOption = nil
        answers[q.id] = entry
    }

    func clearAnswer() {
        guard let q = currentQuestion else { return }
        guard var entry = answers[q.id] else { return }
        entry.selectedOption = nil
        entry.answerText = nil
        answers[q.id] = entry
    }

    func toggleMark() {
        guard let q = currentQuestion else { return }
        var entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
        entry.markedForReview.toggle()
        answers[q.id] = entry
    }

    // MARK: Submission

    @MainActor
    func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        errorMessage = nil

        let payload = SolveService.SubmitRequest(
            subjectId: nil,
            mockExamId: mockExamId,
            answers: questions.map { q in
                let entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
                return entry.toSubmitAnswer
            }
        )

        do {
            let result = try await SolveService.submit(payload)
            submittedResult = result
            stopTimer()
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }

        isSubmitting = false
    }

    func dismissError() {
        errorMessage = nil
    }
}
