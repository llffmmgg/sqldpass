import Foundation
import Observation

@Observable
final class MockExamsViewModel {
    private(set) var exams: [MockExamSummary] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            exams = try await ExamService.list()
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
