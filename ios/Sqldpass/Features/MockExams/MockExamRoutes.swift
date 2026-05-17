import Foundation

/// MockExams 탭의 NavigationStack 라우트.
/// Sheet 모달 대신 push 로 풀이→결과 흐름을 연결한다.
enum MockExamRoute: Hashable {
    case detail(examId: Int64)
    case solve(examId: Int64, questions: [MockExamQuestionItem])
    case result(result: Solve, questions: [MockExamQuestionItem])

    // Solve / MockExamQuestionItem 의 Hashable 합성을 강제하지 않도록
    // 식별자 기반 커스텀 Hashable 구현.
    static func == (lhs: MockExamRoute, rhs: MockExamRoute) -> Bool {
        switch (lhs, rhs) {
        case (.detail(let a), .detail(let b)):
            return a == b
        case (.solve(let a, _), .solve(let b, _)):
            return a == b
        case (.result(let a, _), .result(let b, _)):
            return a.id == b.id
        default:
            return false
        }
    }

    func hash(into hasher: inout Hasher) {
        switch self {
        case .detail(let id):
            hasher.combine(0)
            hasher.combine(id)
        case .solve(let id, _):
            hasher.combine(1)
            hasher.combine(id)
        case .result(let result, _):
            hasher.combine(2)
            hasher.combine(result.id)
        }
    }
}
