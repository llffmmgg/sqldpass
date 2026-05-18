import Foundation

/// 오프라인 풀이 큐를 서버에 재전송.
///
/// Android `AppRepository.drainPendingSolves` 와 동치 흐름:
///  - 미동기화 row 를 createdAt asc 순회.
///  - subjectId 가 채워졌으면 MOBILE_PRACTICE source 로 단일 채점 흐름.
///  - mockExamId 가 채워졌으면 모의고사 흐름.
///  - 멱등키 clientSubmissionId 로 중복 row 방지.
///  - 개별 row 실패 시 다음 row 로 continue (partial retry). 401/403 만 break.
@MainActor
final class SolveSyncManager: ObservableObject {
    static let shared = SolveSyncManager()
    private var draining = false

    /// 마지막 drain 에서 성공적으로 동기화한 row 수.
    @Published private(set) var lastDrainSyncedCount: Int = 0
    /// 마지막 drain 에서 실패한 row 수 (인증 만료 break 직전까지 카운트).
    @Published private(set) var lastDrainFailedCount: Int = 0

    private init() {}

    func tryDrain() async {
        guard !draining else { return }
        draining = true
        defer { draining = false }

        // 매 drain 마다 카운트 리셋 — 마지막 drain 결과만 반영.
        lastDrainSyncedCount = 0
        lastDrainFailedCount = 0

        let pending: [PendingSolve]
        do {
            pending = try SolveQueue.shared.listUnsynced()
        } catch {
            return
        }

        for entity in pending {
            do {
                let data = Data(entity.answersJSON.utf8)
                let answers = try JSONDecoder().decode([SolveService.SubmitRequest.Answer].self, from: data)
                let request = SolveService.SubmitRequest(
                    subjectId: entity.subjectId,
                    mockExamId: entity.mockExamId,
                    source: entity.subjectId != nil ? "MOBILE_PRACTICE" : nil,
                    answers: answers,
                    clientSubmissionId: entity.clientSubmissionId
                )
                let response = try await SolveService.submit(request)
                try SolveQueue.shared.markSynced(entity, serverSolveId: response.id)
                lastDrainSyncedCount += 1
            } catch APIError.unauthorized, APIError.forbidden {
                // 토큰 만료/권한 박탈 — 이후 row 도 모두 같은 실패. break.
                break
            } catch {
                // 일시 오류(5xx, 네트워크, 디코딩 등) — 다음 row 시도.
                lastDrainFailedCount += 1
                continue
            }
        }
    }
}
