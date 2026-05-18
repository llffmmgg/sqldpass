import Foundation
import Observation

/// 단일 채점 풀이 화면의 상태/액션을 보관.
///
/// Android `SoloSession` + `AppViewModel.soloXxx` 메서드와 동치 UX.
/// 1문제씩 즉시 채점 → 정답 공개 → 다음 문제 → SET_SIZE(10) 도달 시 sessionComplete.
///
/// `start()` 를 `.task` 에서 호출, 또는 외부에서 awaited 후 진입.
@MainActor
@Observable
final class SoloSolveViewModel {

    static let setSize = 10

    let subjectId: Int64
    let subjectName: String

    private(set) var sessionQuestions: [Question] = []
    private(set) var queue: [Question] = []
    private(set) var solvedCount = 0
    private(set) var correctCount = 0
    private(set) var selectedOption: Int? = nil
    var answerText: String = ""
    private(set) var detail: QuestionDetail? = nil
    private(set) var revealed = false
    private(set) var submitting = false
    private(set) var sessionComplete = false
    private(set) var submitError: String? = nil
    private(set) var loading = false
    private(set) var fatalError: String? = nil

    init(subjectId: Int64, subjectName: String) {
        self.subjectId = subjectId
        self.subjectName = subjectName
    }

    var current: Question? { queue.first }

    var hasAnswer: Bool {
        guard let q = current else { return false }
        let type = q.questionType.uppercased()
        return if type.contains("MCQ") || type.contains("MULTIPLE") {
            selectedOption != nil
        } else {
            !answerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    var isLastBeforeComplete: Bool { solvedCount + 1 >= Self.setSize }

    var isCorrect: Bool {
        guard let d = detail else { return false }
        return Self.evaluate(detail: d, selectedOption: selectedOption, answerText: answerText)
    }

    // MARK: - Lifecycle

    func start() async {
        guard sessionQuestions.isEmpty else { return }
        loading = true
        fatalError = nil
        do {
            let questions = try await QuestionService.list(subjectId: subjectId, size: Self.setSize)
            if questions.isEmpty {
                fatalError = "이 과목에서 가져올 문제가 없습니다."
            } else {
                sessionQuestions = questions
                queue = questions
            }
        } catch {
            fatalError = error.localizedDescription
        }
        loading = false
    }

    // MARK: - Actions

    func selectOption(_ num: Int) {
        guard !revealed, current != nil else { return }
        selectedOption = num
    }

    func setAnswerText(_ text: String) {
        guard !revealed else { return }
        answerText = text
    }

    func submit() async {
        guard !revealed, !submitting, let current else { return }
        guard hasAnswer else { return }

        submitting = true
        submitError = nil

        let fetched: QuestionDetail
        do {
            fetched = try await QuestionService.fullDetail(id: current.id)
        } catch {
            submitError = error.localizedDescription
            submitting = false
            return
        }

        detail = fetched
        let correct = Self.evaluate(detail: fetched, selectedOption: selectedOption, answerText: answerText)
        solvedCount += 1
        if correct { correctCount += 1 }
        revealed = true
        submitting = false

        // 백그라운드 풀이 기록 — 실패해도 흐름 막지 않음. step 6 에서 큐잉 통합.
        Task { [weak self] in
            guard let self else { return }
            await self.recordAnswer(questionId: current.id, type: current.questionType)
        }
    }

    private func recordAnswer(questionId: Int64, type: String) async {
        let upper = type.uppercased()
        let isText = upper.contains("SHORT") || upper.contains("DESCRIPTIVE") || upper.contains("TEXT")
        let clientSubmissionId = "ios-\(UUID().uuidString)"
        let request = SolveService.SubmitRequest(
            subjectId: subjectId,
            mockExamId: nil,
            source: "MOBILE_PRACTICE",
            answers: [
                .init(
                    questionId: questionId,
                    selectedOption: isText ? nil : selectedOption,
                    answerText: isText ? answerText : nil,
                ),
            ],
            clientSubmissionId: clientSubmissionId,
        )
        do {
            _ = try await SolveService.submit(request)
        } catch {
            // 네트워크/서버 실패 — 큐잉 시도. 큐잉 성공이면 submitError 안 띄움(인디케이터로 충분).
            do {
                _ = try SolveQueue.shared.enqueueSolo(
                    subjectId: subjectId,
                    questionId: questionId,
                    selectedOption: isText ? nil : selectedOption,
                    answerText: isText ? answerText : nil,
                    clientSubmissionId: clientSubmissionId
                )
            } catch {
                self.submitError = error.localizedDescription
            }
        }
    }

    func goNext() async {
        guard revealed else { return }

        if solvedCount >= Self.setSize {
            sessionComplete = true
            return
        }

        let remaining = Array(queue.dropFirst())
        if !remaining.isEmpty {
            queue = remaining
            resetCurrentInput()
            return
        }

        // 큐 소진 — 추가 fetch
        submitting = true
        do {
            let fresh = try await QuestionService.list(subjectId: subjectId, size: Self.setSize)
            queue = fresh
            resetCurrentInput()
        } catch {
            submitError = error.localizedDescription
        }
        submitting = false
    }

    func replaySame() {
        queue = sessionQuestions
        resetCounters()
        resetCurrentInput()
    }

    func newRandom() async {
        sessionQuestions = []
        queue = []
        resetCounters()
        resetCurrentInput()
        await start()
    }

    // MARK: - Helpers

    private func resetCurrentInput() {
        selectedOption = nil
        answerText = ""
        revealed = false
        detail = nil
        submitError = nil
    }

    private func resetCounters() {
        solvedCount = 0
        correctCount = 0
        sessionComplete = false
    }

    static func evaluate(detail: QuestionDetail, selectedOption: Int?, answerText: String) -> Bool {
        let type = detail.questionType.uppercased()
        if type.contains("MCQ") || type.contains("MULTIPLE") {
            guard let sel = selectedOption, let c = detail.correctOption else { return false }
            return sel == c
        }
        let normalize: (String) -> String = {
            $0.trimmingCharacters(in: .whitespacesAndNewlines)
              .lowercased()
              .replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
        }
        let submitted = normalize(answerText)
        if submitted.isEmpty { return false }
        if let ans = detail.answer, normalize(ans) == submitted { return true }
        return detail.keywords.contains { normalize($0) == submitted }
    }
}
