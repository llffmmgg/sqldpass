import SwiftUI

struct SolveResultView: View {
    let result: Solve
    let questions: [MockExamQuestionItem]
    let onDone: () -> Void

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
                        SectionHeader(title: "과목별 정답률")
                        ForEach(subjectBreakdown, id: \.name) { item in
                            SubjectBreakdownCard(
                                subjectName: item.name,
                                correct: item.correct,
                                total: item.total
                            )
                        }
                    }
                }

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    SectionHeader(title: "문항별 검토")
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
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: Spacing.sm) {
                Button {
                    onDone()
                } label: {
                    Text("목록으로")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
            }
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.sm)
            .background(Color.appPage)
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
                chosenAnswer: ans.chosenAnswer,
                isCorrect: ans.correct
            )
        }
    }
}

private struct SectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(AppType.bodyEmph)
            .foregroundStyle(Color.appTextPrimary)
    }
}
