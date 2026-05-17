import Foundation
import Observation

@Observable
final class WrongAnswersViewModel {
    private(set) var items: [WrongAnswer] = []
    private(set) var stats: [WrongAnswerStats] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let itemsTask = WrongAnswerService.list()
            async let statsTask = WrongAnswerService.stats()
            let (i, s) = try await (itemsTask, statsTask)
            items = i
            stats = s
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// retry 성공 시 해당 문항을 목록에서 제거 (백엔드 정책: 정답이면 자동 마스터)
    func markMastered(questionId: Int64) {
        items.removeAll { $0.questionId == questionId }
    }
}
