import Foundation
import Observation

@Observable
final class ProfileViewModel {
    private(set) var me: MemberMe?
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            me = try await MemberService.me()
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func updateLocalNickname(_ nickname: String) {
        guard let current = me else { return }
        me = MemberMe(
            id: current.id,
            nickname: nickname,
            provider: current.provider,
            createdAt: current.createdAt
        )
    }
}
