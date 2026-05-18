import Foundation

/// 오프라인 풀이 큐를 서버에 재전송.
///
/// Android `AppRepository.drainPendingSolves` 와 동치 흐름:
///  - 미동기화 row 를 createdAt asc 순회.
///  - subjectId 가 채워졌으면 MOBILE_PRACTICE source 로 단일 채점 흐름.
///  - mockExamId 가 채워졌으면 모의고사 흐름.
///  - 멱등키 clientSubmissionId 로 중복 row 방지.
///  - 개별 row 실패 시 break (다음 네트워크 이벤트나 앱 재시작 때 재시도).
@MainActor
final class SolveSyncManager {
    static let shared = SolveSyncManager()
    private var draining = false
    private init() {}

    func tryDrain() async {
        guard !draining else { return }
        draining = true
        defer { draining = false }

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
            } catch {
                break
            }
        }
    }
}
