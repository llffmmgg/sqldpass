import Foundation
import SwiftData

/// 오프라인 풀이 제출 큐 1 row.
///
/// Android `PendingSolveEntity` 와 동일 필드. 단일 채점 풀이는 `subjectId` 가 양수 +
/// `mockExamId` 가 nil. 모의고사는 반대. drain 시 둘 중 채워진 쪽으로 분기.
///
/// 멱등키 `clientSubmissionId` 는 백엔드의 SolveRequest.clientSubmissionId(@Size max=64) 와 동치.
@Model
final class PendingSolve {
    /// 음수 timestamp 기반 로컬 id — 합성 SolveResponse 의 id 로도 활용 가능.
    @Attribute(.unique) var localId: Int64
    var subjectId: Int64?
    var mockExamId: Int64?
    var totalCount: Int
    var correctCount: Int
    /// JSON 직렬화된 [SolveService.SubmitRequest.Answer].
    var answersJSON: String
    @Attribute(.unique) var clientSubmissionId: String
    var createdAt: Date
    var synced: Bool
    var serverSolveId: Int64?
    var syncedAt: Date?

    init(
        subjectId: Int64? = nil,
        mockExamId: Int64? = nil,
        totalCount: Int = 1,
        correctCount: Int = 0,
        answersJSON: String,
        clientSubmissionId: String = "ios-\(UUID().uuidString)"
    ) {
        self.localId = -Int64(Date().timeIntervalSince1970 * 1000)
        self.subjectId = subjectId
        self.mockExamId = mockExamId
        self.totalCount = totalCount
        self.correctCount = correctCount
        self.answersJSON = answersJSON
        self.clientSubmissionId = clientSubmissionId
        self.createdAt = Date()
        self.synced = false
        self.serverSolveId = nil
        self.syncedAt = nil
    }
}
