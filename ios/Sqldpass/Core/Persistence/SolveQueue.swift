import Foundation
import Observation
import SwiftData

/// 풀이 큐 관리 — enqueue / listUnsynced / markSynced / pendingCount.
///
/// `@MainActor` 격리 — SwiftData ModelContext 는 main thread 안전성이 요구된다.
/// `pendingCount` 는 SwiftUI 가 관찰하도록 `@Observable` 매크로 또는 ObservableObject.
@MainActor
@Observable
final class SolveQueue {
    static let shared = SolveQueue()

    private(set) var pendingCount: Int = 0

    private var context: ModelContext { SqldpassModelContainer.shared.mainContext }

    private init() {
        refreshCount()
    }

    /// 단일 채점 풀이 enqueue.
    @discardableResult
    func enqueueSolo(
        subjectId: Int64,
        questionId: Int64,
        selectedOption: Int?,
        answerText: String?,
        clientSubmissionId: String = "ios-\(UUID().uuidString)"
    ) throws -> PendingSolve {
        let answer = SolveService.SubmitRequest.Answer(
            questionId: questionId,
            selectedOption: selectedOption,
            answerText: answerText
        )
        let data = try JSONEncoder().encode([answer])
        let json = String(data: data, encoding: .utf8) ?? "[]"
        let entity = PendingSolve(
            subjectId: subjectId,
            mockExamId: nil,
            totalCount: 1,
            correctCount: 0,
            answersJSON: json,
            clientSubmissionId: clientSubmissionId
        )
        context.insert(entity)
        try context.save()
        refreshCount()
        return entity
    }

    /// 모의고사 전체 답안 enqueue.
    /// `SolveSyncManager.tryDrain` 이 `mockExamId != nil` 분기로 `/api/solves` 에 재전송.
    @discardableResult
    func enqueueMockExam(
        mockExamId: Int64,
        answers: [SolveService.SubmitRequest.Answer],
        clientSubmissionId: String
    ) throws -> PendingSolve {
        let data = try JSONEncoder().encode(answers)
        let json = String(data: data, encoding: .utf8) ?? "[]"
        let entity = PendingSolve(
            subjectId: nil,
            mockExamId: mockExamId,
            totalCount: answers.count,
            correctCount: 0,
            answersJSON: json,
            clientSubmissionId: clientSubmissionId
        )
        context.insert(entity)
        try context.save()
        refreshCount()
        return entity
    }

    func listUnsynced() throws -> [PendingSolve] {
        let descriptor = FetchDescriptor<PendingSolve>(
            predicate: #Predicate { !$0.synced },
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )
        return try context.fetch(descriptor)
    }

    func markSynced(_ entity: PendingSolve, serverSolveId: Int64) throws {
        entity.synced = true
        entity.serverSolveId = serverSolveId
        entity.syncedAt = Date()
        try context.save()
        refreshCount()
    }

    private func refreshCount() {
        let descriptor = FetchDescriptor<PendingSolve>(predicate: #Predicate { !$0.synced })
        pendingCount = (try? context.fetchCount(descriptor)) ?? 0
    }
}
