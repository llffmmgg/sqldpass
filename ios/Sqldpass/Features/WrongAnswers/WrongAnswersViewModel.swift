import Foundation
import Observation

@Observable
final class WrongAnswersViewModel {
    private(set) var items: [WrongAnswer] = []
    private(set) var stats: [WrongAnswerStats] = []
    /// 401/403 응답 — 플랜 미가입 등 권한 부족. View 는 잠금 안내로 분기한다.
    private(set) var isLocked = false
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
            isLocked = false
            errorMessage = nil
        } catch APIError.cancelled {
            // 동일 endpoint 동시 호출에서 한 쪽이 취소된 경우 — 화면 에러로 띄우지 않는다.
        } catch APIError.unauthorized, APIError.forbidden {
            items = []
            stats = []
            isLocked = true
            errorMessage = nil
        } catch let error as APIError {
            isLocked = false
            errorMessage = error.errorDescription
        } catch {
            isLocked = false
            errorMessage = error.localizedDescription
        }
    }

    /// retry 성공 시 해당 문항을 목록에서 제거 (백엔드 정책: 정답이면 자동 마스터)
    func markMastered(questionId: Int64) {
        items.removeAll { $0.questionId == questionId }
    }
}
