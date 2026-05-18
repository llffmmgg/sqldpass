import SwiftUI

/// 모의고사 채점 결과 화면 (Inked OMR 디자인 시스템).
///
/// PASS/FAIL 배너 → 과목별 정답률 → 문항별 검토 순으로 스크롤 구성.
/// 하단 액션바는 `AppBottomActionBar` 사용.
struct SolveResultView: View {
    let result: Solve
    let questions: [MockExamQuestionItem]
    let onDone: () -> Void
    var onRestart: (() -> Void)? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                ScoreHeadline(
                    score: result.score,
                    correctCount: result.correctCount,
                    totalCount: result.totalCount,
                    milestoneReached: result.milestoneReached,
                    currentStreak: result.currentStreak
                )

                if !subjectBreakdown.isEmpty {
                    VStack(alignment: .leading, spacing: Spacing.sm) {
                        AppSectionHeader(title: "과목별 정답률")
                        AppCard(surface: .card) {
                            VStack(alignment: .leading, spacing: Spacing.md) {
                                ForEach(Array(subjectBreakdown.enumerated()), id: \.element.name) { idx, item in
                                    SubjectBreakdownCard(
                                        subjectName: item.name,
                                        correct: item.correct,
                                        total: item.total
                                    )
                                    if idx < subjectBreakdown.count - 1 {
                                        Rectangle()
                                            .fill(Color.appBorder)
                                            .frame(height: 1)
                                    }
                                }
                            }
                        }
                    }
                }

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    AppSectionHeader(title: "문항별 검토")
                    ForEach(reviewItems, id: \.questionId) { item in
                        AnswerReviewRow(item: item)
                    }
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle("채점 결과")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if let onRestart {
                AppBottomActionBar(
                    primary: BottomAction(title: "닫기",
                                          action: onDone,
                                          variant: .primary),
                    secondary: BottomAction(title: "다시 풀기",
                                            action: onRestart,
                                            variant: .secondary)
                )
            } else {
                AppBottomActionBar(
                    primary: BottomAction(title: "닫기",
                                          action: onDone,
                                          variant: .primary)
                )
            }
        }
    }

    // MARK: Derived

    private struct SubjectStat {
        let name: String
        var correct: Int
        var total: Int
    }

    private var subjectBreakdown: [SubjectStat] {
        let subjectByQuestion: [Int64: String] = Dictionary(
            uniqueKeysWithValues: questions.map { ($0.id, $0.subjectName) }
        )
        var grouped: [String: SubjectStat] = [:]
        for answer in result.answers {
            let subject = subjectByQuestion[answer.questionId] ?? "기타"
            var stat = grouped[subject] ?? SubjectStat(name: subject, correct: 0, total: 0)
            stat.total += 1
            if answer.correct { stat.correct += 1 }
            grouped[subject] = stat
        }
        return grouped.values.sorted { $0.name < $1.name }
    }

    private var reviewItems: [AnswerReviewRow.Item] {
        let questionByid: [Int64: MockExamQuestionItem] = Dictionary(
            uniqueKeysWithValues: questions.map { ($0.id, $0) }
        )
        return result.answers.enumerated().map { idx, ans in
            let q = questionByid[ans.questionId]
            return AnswerReviewRow.Item(
                questionId: ans.questionId,
                displayOrder: q?.displayOrder ?? (idx + 1),
                content: q?.content ?? "(문제 정보 누락)",
                selectedOption: ans.selectedOption,
                correctOption: ans.correctOption,
                isCorrect: ans.correct
            )
        }
    }
}
