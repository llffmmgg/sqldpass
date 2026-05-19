import SwiftUI

/// 학습 기록 상세 — 목록의 `SolveSummary` 행을 탭하면 진입.
///
/// 동작:
/// 1) `GET /api/solves/{id}` 로 답안 포함 `Solve` 를 다시 조회.
/// 2) mock 풀이면 `GET /api/mock-exams/{mockExamId}` 로 문항 본문을 함께 가져와
///    `SolveResultView` 가 문항별 검토 카드를 본문과 함께 표시할 수 있게 한다.
/// 3) 실전 문제(`subjectId != nil`) 풀이는 문항 본문 일괄 조회 API 가 없으므로
///    `questions: []` 로 진입 — 본문은 "문제 #\(questionId)" 로 폴백(`SolveResultView` 가 처리).
struct HistoryDetailView: View {
    let summary: SolveSummary

    @State private var detail: Solve?
    @State private var questions: [MockExamQuestionItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if let detail {
                SolveResultView(
                    result: detail,
                    questions: questions,
                    onDone: { dismiss() }
                )
            } else if isLoading {
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.appPage)
            } else if let errorMessage {
                ContentUnavailableView {
                    Label("불러오기 실패", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("다시 시도") { Task { await load() } }
                }
            }
        }
        .task { await load() }
    }

    private func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let fetched = try await SolveService.detail(id: summary.id)
            var stems: [MockExamQuestionItem] = []
            if let mockExamId = fetched.mockExamId {
                if let exam = try? await ExamService.detail(id: mockExamId) {
                    stems = exam.questions
                }
            }
            detail = fetched
            questions = stems
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
