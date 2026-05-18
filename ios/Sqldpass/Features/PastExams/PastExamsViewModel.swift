import Foundation
import Observation

/// 기출복원 탭의 자격증별 회차 캐시.
///
/// 동작: 사용자가 자격증 칩을 선택하면 해당 slug 의 회차 목록을 1회만 fetch.
/// 캐시(`byCert[slug]`)에 이미 있으면 재요청하지 않는다 (수동 refresh 는 `reload`).
///
/// Android 미러: `AppViewModel.loadPastExamsForCert(slug)` 의 단순화.
@MainActor
@Observable
final class PastExamsViewModel {

    /// 자격증 slug → 회차 목록 캐시.
    private(set) var byCert: [String: [PastExamSummary]] = [:]

    /// 현재 선택된 자격증 slug. 기본값은 SQLD.
    var selectedCert: String = "sqld"

    /// 현재 selectedCert 의 fetch 진행 여부.
    private(set) var isLoading: Bool = false

    /// 마지막 fetch 에러 메시지. selectedCert 에 캐시가 비어 있고 에러가 있을 때만 UI 노출.
    private(set) var errorMessage: String? = nil

    var currentExams: [PastExamSummary] {
        byCert[selectedCert] ?? []
    }

    /// 캐시 없으면 fetch. 캐시가 있어도 강제 갱신하고 싶으면 `force = true`.
    func load(slug: String, force: Bool = false) async {
        if !force, byCert[slug] != nil { return }
        isLoading = true
        defer { isLoading = false }
        do {
            let list = try await PastExamService.list(slug: slug)
            byCert[slug] = list
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// 자격증 칩 선택 시 호출. selectedCert 갱신 + 필요 시 fetch.
    func selectCert(_ slug: String) async {
        selectedCert = slug
        await load(slug: slug)
    }
}
