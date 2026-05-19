import Foundation
import Observation

@Observable
final class HistoryViewModel {
    private(set) var solves: [SolveSummary] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let raw = try await SolveService.myHistory()
            solves = raw.sorted { $0.solvedAt > $1.solvedAt }
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
