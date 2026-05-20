import Foundation
import Observation

/// 홈 탭 상태 로더 — 닉네임, streak, KPI(총 풀이/평균 정답률), 7일 출석, 약점 통계.
///
/// KPI 와 7일 출석은 `SolveService.myHistory()` 응답을 클라이언트에서 집계.
@Observable
final class HomeViewModel {
    private(set) var member: MemberMe?
    private(set) var streak: StreakInfo?
    private(set) var kpi: ProfileKpi = .empty
    /// 오늘 포함 최근 7일 일자별 풀이 문항 수 — 인덱스 0 = 6일 전, 인덱스 6 = 오늘.
    /// GitHub commit chart 식 4단계 강도 (0 / 1~5 / 6~20 / 21+).
    private(set) var weekCounts: [Int] = Array(repeating: 0, count: 7)
    /// 약점 보강 섹션용. 실패 시 빈 배열 — errorMessage 에는 전파하지 않는다.
    private(set) var wrongStats: [WrongAnswerStats] = []
    /// 오답노트 API 가 구독 권한 부족(401/403)으로 거부됐는지. 약점 섹션을 잠금 안내로 전환.
    private(set) var wrongStatsLocked = false
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }

        // 1) Member — critical. 실패 시 errorMessage 트리거 + 다른 데이터는 비움.
        do {
            member = try await MemberService.me()
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }

        // 2) Streak — optional. 실패해도 화면 다른 영역에 영향 X.
        var loadedStreak: StreakInfo? = nil
        do {
            let s = try await StreakService.me()
            streak = s
            loadedStreak = s
        } catch {
            streak = nil
        }

        // 3) History — optional. KPI/잔디는 부분 표시되며, 실패해도 critical 에러 아님.
        //    cancelled(중복 호출 취소) 는 기존 값 유지 — pull-to-refresh 중 깜빡임 방지.
        do {
            let history = try await SolveService.myHistory()
            kpi = HomeViewModel.aggregate(history: history, longestStreak: loadedStreak?.longestStreak)
            weekCounts = HomeViewModel.last7DaysCounts(history: history)
        } catch APIError.cancelled {
            // 기존 kpi/weekCounts 유지.
        } catch {
            kpi = ProfileKpi(
                totalSolved: nil,
                avgCorrectRate: nil,
                longestStreak: loadedStreak?.longestStreak,
                passProbability: nil
            )
            weekCounts = Array(repeating: 0, count: 7)
        }

        // 4) WrongAnswerStats — 권한(401/403)이면 잠금 카드, cancelled 는 현재 값 유지, 그 외는 비움.
        do {
            wrongStats = try await WrongAnswerService.stats()
            wrongStatsLocked = false
        } catch APIError.unauthorized, APIError.forbidden {
            wrongStats = []
            wrongStatsLocked = true
        } catch APIError.cancelled {
            // 동일 endpoint 동시 호출의 취소 — 기존 값 유지.
        } catch {
            wrongStats = []
            wrongStatsLocked = false
        }
    }

    /// 풀이 이력에서 KPI 집계. 합격 확률은 백엔드 미통합으로 `nil` 유지.
    static func aggregate(history: [SolveSummary], longestStreak: Int?) -> ProfileKpi {
        guard !history.isEmpty else {
            return ProfileKpi(
                totalSolved: 0,
                avgCorrectRate: nil,
                longestStreak: longestStreak,
                passProbability: nil
            )
        }
        let totalSolved = history.reduce(0) { $0 + $1.totalCount }
        let totalCorrect = history.reduce(0) { $0 + $1.correctCount }
        let avgCorrectRate: Int? = totalSolved > 0
            ? Int(round(Double(totalCorrect) / Double(totalSolved) * 100))
            : nil
        return ProfileKpi(
            totalSolved: totalSolved,
            avgCorrectRate: avgCorrectRate,
            longestStreak: longestStreak,
            passProbability: nil
        )
    }

    /// 오늘 포함 최근 7일 일자별 풀이 문항 수 합계.
    /// 인덱스 0 = 6일 전, 인덱스 6 = 오늘. `solvedAt` 은 ISO 8601 또는 timezone 없는 LocalDateTime.
    static func last7DaysCounts(history: [SolveSummary], now: Date = Date()) -> [Int] {
        let cal = Calendar(identifier: .gregorian)
        let today = cal.startOfDay(for: now)

        // solvedAt → startOfDay 매핑하며 totalCount 누적.
        var totalsByDay: [Date: Int] = [:]
        for s in history {
            guard let d = parseSolvedAt(s.solvedAt) else { continue }
            let key = cal.startOfDay(for: d)
            totalsByDay[key, default: 0] += s.totalCount
        }

        return (0..<7).map { offset in
            guard let target = cal.date(byAdding: .day, value: offset - 6, to: today) else {
                return 0
            }
            return totalsByDay[target] ?? 0
        }
    }

    /// 백엔드 `solvedAt` 파서 — ISO 8601 변형 우선, 실패 시 KST LocalDateTime fallback.
    private static func parseSolvedAt(_ s: String) -> Date? {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = iso.date(from: s) { return d }
        iso.formatOptions = [.withInternetDateTime]
        if let d = iso.date(from: s) { return d }

        // Spring Boot LocalDateTime — timezone 없음. KST 로 해석.
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        for format in ["yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                       "yyyy-MM-dd'T'HH:mm:ss.SSS",
                       "yyyy-MM-dd'T'HH:mm:ss"] {
            df.dateFormat = format
            if let d = df.date(from: s) { return d }
        }
        return nil
    }
}
