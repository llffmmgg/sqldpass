import Foundation
import Observation

@Observable
final class DashboardViewModel {
    private(set) var member: MemberMe?
    private(set) var streak: StreakInfo?
    private(set) var stats: OverallStats?
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
    }
}
