import Foundation
import Observation

/// 기출 회차 풀이 화면 상태 + 액션.
///
/// 라이프사이클:
///   init(examId:) → load() → answer/navigation → submit() → graded result
///
/// 본 모델은 모의고사 `SolveViewModel` 과 구조는 비슷하지만 백엔드 채점 흐름(`/grade`)이
/// 별도 응답을 반환하므로 result 형식이 다르다. 큐잉/멱등키 인터페이스는 백엔드의
/// `PastExamGradeRequest` 에 idempotency 필드가 없어 본 step 에선 채택하지 않는다.
// SolveViewModel 과 동일하게 클래스에는 @MainActor 를 두지 않는다 — 그러면
// deinit 이 nonisolated 컨텍스트에서 실행되어도 timerTask 를 cancel 할 수 있다.
// 메인 액터 격리가 필요한 async 메서드(load/submit)는 개별 @MainActor 어노테이션으로 처리.
@Observable
final class PastExamRunnerViewModel {
    let examId: Int64

    private(set) var detail: PastExamDetailResponse?
    private(set) var currentIndex: Int = 0
    private(set) var answers: [Int64: SolveAnswerEntry] = [:]
    private(set) var startedAt: Date?
    private(set) var tickToken: Int = 0
    private var timerTask: Task<Void, Never>?

    private(set) var isLoading = false
    private(set) var loadError: String?

    private(set) var isSubmitting = false
    private(set) var graded: PastExamGradeResponse?
    private(set) var submitError: String?

    init(examId: Int64) {
        self.examId = examId
    }

    deinit {
        timerTask?.cancel()
    }

    // MARK: Derived

    var questions: [MockExamQuestionItem] { detail?.questions ?? [] }
    var totalCount: Int { questions.count }
    var currentQuestion: MockExamQuestionItem? {
        guard questions.indices.contains(currentIndex) else { return nil }
        return questions[currentIndex]
    }
    var currentEntry: SolveAnswerEntry? {
        guard let q = currentQuestion else { return nil }
        return answers[q.id]
    }
    var progress: Double {
        guard totalCount > 0 else { return 0 }
        return Double(currentIndex + 1) / Double(totalCount)
    }
    var answeredCount: Int { answers.values.filter(\.isAnswered).count }
    var markedCount: Int { answers.values.filter { $0.markedForReview }.count }
    var elapsedSeconds: Int {
        guard let startedAt else { return 0 }
        _ = tickToken
        return Int(Date().timeIntervalSince(startedAt))
    }

    // MARK: Lifecycle

    @MainActor
    func load() async {
        guard detail == nil else { return }
        isLoading = true
        loadError = nil
        do {
            detail = try await PastExamService.detail(id: examId)
            // 타이머는 사용자가 start() 호출 시점에 시작. 자동 시작 X.
        } catch let error as APIError {
            loadError = error.errorDescription
        } catch {
            loadError = error.localizedDescription
        }
        isLoading = false
    }

    /// 시작 오버레이의 버튼이 호출 — 풀이 데이터가 로드된 뒤에만 동작.
    func start() {
        guard startedAt == nil, detail != nil else { return }
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

    func go(to index: Int) {
        guard questions.indices.contains(index) else { return }
        currentIndex = index
    }

    func goPrevious() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
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
        guard !isSubmitting, graded == nil else { return }
        isSubmitting = true
        submitError = nil

        let payload: [PastExamService.GradeRequest.Answer] = questions.map { q in
            let entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
            return .init(
                questionId: q.id,
                selectedOption: entry.selectedOption,
                answerText: entry.answerText?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            )
        }

        do {
            graded = try await PastExamService.grade(id: examId, answers: payload)
            stopTimer()
        } catch let error as APIError {
            submitError = error.errorDescription
        } catch {
            submitError = error.localizedDescription
        }
        isSubmitting = false
    }

    func dismissError() {
        submitError = nil
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
