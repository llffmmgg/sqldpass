import Foundation
import Observation

@Observable
final class ProfileViewModel {
    private(set) var me: MemberMe?
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    /// 내정보 KPI 2x2 그리드 값.
    ///
    /// 본 phase(`mobile-ux-restructure` step 7)에서는 `longestStreak` 만
    /// `/api/streak/me` 의 실데이터로 채운다.
    /// 나머지 세 항목(누적 풀이·평균 정답률·합격 확률)은 별 phase
    /// `kpi-backend-support` 에서 채울 예정 — 본 step 에서는 `nil` 유지.
    private(set) var kpi: ProfileKpi = .empty

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
        await loadKpi()
    }

    /// KPI 그리드용 값 적재.
    ///
    /// 현재 채우는 항목:
    /// - `longestStreak`: `/api/streak/me` 의 `longestStreak` 필드
    ///
    /// 나머지 세 항목은 본 phase 범위 외 — 별 phase `kpi-backend-support`
    /// 에서 백엔드 신설로 채운다. `passProbability` 의 클라이언트 자체
    /// 계산식 포팅 역시 본 step 의 책임 범위 밖이므로 시도하지 않는다.
    func loadKpi() async {
        var longestStreak: Int? = nil
        do {
            let info = try await StreakService.me()
            longestStreak = info.longestStreak
        } catch {
            // KPI 로드 실패는 화면 전체 실패로 승격하지 않는다.
            // streak 호출이 실패하면 해당 타일도 "—" 로 표시된다.
            longestStreak = nil
        }
        kpi = ProfileKpi(
            totalSolved: nil,
            avgCorrectRate: nil,
            longestStreak: longestStreak,
            passProbability: nil
        )
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
