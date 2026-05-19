import Foundation
import Observation

/// 홈 탭 상태 로더 — 닉네임 + streak + 누적 통계를 병렬 fetch.
///
/// 이전 `DashboardViewModel` 의 동일 동작을 Home 도메인으로 옮긴 것.
/// Dashboard 화면 자체는 mobile-ux-restructure 에서 제거됐다.
@Observable
final class HomeViewModel {
    private(set) var member: MemberMe?
    private(set) var streak: StreakInfo?
    private(set) var stats: OverallStats?
    /// 약점 보강 섹션용. 실패 시 빈 배열 — errorMessage 에는 전파하지 않는다 (섹션이 단순히 숨겨질 뿐).
    private(set) var wrongStats: [WrongAnswerStats] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }

        do {
            async let memberTask = MemberService.me()
            async let streakTask = StreakService.me()
            async let statsTask = SolveService.overallStats()
            let (m, s, st) = try await (memberTask, streakTask, statsTask)
            member = m
            streak = s
            stats = st
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }

        // 약점 보강 stats 는 별도 분기 — 실패해도 main errorMessage 에 영향 주지 않는다.
        // 권한/네트워크 등 어떤 이유로든 실패하면 섹션을 단순히 숨긴다.
        do {
            wrongStats = try await WrongAnswerService.stats()
        } catch {
            wrongStats = []
        }
    }
}
