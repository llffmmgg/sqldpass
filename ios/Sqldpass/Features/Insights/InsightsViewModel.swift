import Foundation
import Observation

@Observable
final class InsightsViewModel {
    private(set) var streak: StreakInfo?
    private(set) var overallStats: OverallStats?
    private(set) var subjectStats: [WrongAnswerStats] = []
    private(set) var recentSolves: [Solve] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let streakTask = StreakService.me()
            async let statsTask = SolveService.overallStats()
            async let subjectsTask = WrongAnswerService.stats()
            async let solvesTask = SolveService.myHistory()
            let (s, o, sub, sv) = try await (streakTask, statsTask, subjectsTask, solvesTask)
            streak = s
            overallStats = o
            subjectStats = sub
            recentSolves = sv
                .sorted { $0.solvedAt < $1.solvedAt }
                .suffix(10)
                .map { $0 }
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
