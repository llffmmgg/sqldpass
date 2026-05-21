import Foundation
import Observation

@Observable
final class MockExamsViewModel {
    private(set) var exams: [MockExamSummary] = []
    /// 미니 모의고사 회차(MockExamKind=MINI) 목록. 정규 목록과 분리해 진입 섹션을 만든다.
    /// 백엔드 정책상 미니+모의 합산 일일 1회 — 미니 진입 경로가 없으면 정책이 무의미해
    /// 본 ViewModel 에서 별도 fetch 한다.
    private(set) var miniExams: [MockExamSummary] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    /// 서버 402 → 무료 회원 일일 모의 한도 도달 시 노출되는 페이월 정보.
    /// 자체 카운터 없음 — 서버 단일 진실. 현재 `/api/mock-exams` 리스트는
    /// 가드 대상이 아니지만, 추후 확장(예: 모의 진입 트리거) 시 동일 패턴 사용.
    var quotaPaywall: QuotaPaywallInfo?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        await loadRegular()
        await loadMini()
    }

    /// 정규 회차 (`/api/mock-exams`) — MOCK + PAST_EXAM 통합 목록.
    private func loadRegular() async {
        do {
            exams = try await ExamService.list()
            errorMessage = nil
        } catch APIError.quotaExceeded(let code, let used, let limit, let resetAt) {
            quotaPaywall = QuotaPaywallInfo(code: code, used: used, limit: limit, resetAt: resetAt)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// 미니 회차 (`/api/mock-exams/mini`) — 실패 시 조용히 비움(메인 흐름을 막지 않는다).
    private func loadMini() async {
        do {
            miniExams = try await ExamService.listMini()
        } catch {
            // 미니 fetch 실패는 화면 에러로 띄우지 않는다 — 정규 목록은 정상 노출돼야 한다.
            miniExams = []
        }
    }
}
