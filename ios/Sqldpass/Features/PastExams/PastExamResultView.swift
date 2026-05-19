import SwiftUI

/// 기출 회차 채점 결과 화면.
///
/// 1) ScoreHeadline 으로 합/불 배너
/// 2) 자격증별 공식 합격 기준 한 줄 안내(`passReason`)
/// 3) 과목별 정답률 + 과락 마킹 (SubjectScore[])
/// 4) 문항별 검토 (AnswerReviewRow 재사용)
struct PastExamResultView: View {
    let examName: String
    let graded: PastExamGradeResponse
    let questions: [MockExamQuestionItem]
    let submittedAnswers: [Int64: SolveAnswerEntry]
    let onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                ScoreHeadline(
                    score: graded.score,
                    correctCount: graded.correctCount,
                    totalCount: graded.totalCount,
                    milestoneReached: graded.milestoneReached,
                    currentStreak: nil
                )

                passBanner

                if !graded.subjectScores.isEmpty {
                    subjectSection
                }

                reviewSection
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle(examName)
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            AppBottomActionBar(
                primary: BottomAction(title: "닫기", action: onDone, variant: .primary)
            )
        }
    }

    @ViewBuilder
    private var passBanner: some View {
        if let reason = graded.passReason, !reason.isEmpty {
            HStack(alignment: .top, spacing: Spacing.sm) {
                Image(systemName: graded.passed ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                    .foregroundStyle(graded.passed ? Color.semanticSuccess : Color.semanticDanger)
                Text(reason)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
            .padding(Spacing.base)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(graded.passed ? Color.semanticSuccess.opacity(0.10) : Color.semanticDanger.opacity(0.10))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(
                        (graded.passed ? Color.semanticSuccess : Color.semanticDanger).opacity(0.35),
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
    }

    private var subjectSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            AppSectionHeader(title: "과목별 정답률")
            AppCard(surface: .card) {
                VStack(alignment: .leading, spacing: Spacing.md) {
                    ForEach(Array(graded.subjectScores.enumerated()), id: \.element.id) { idx, stat in
                        SubjectBreakdownCard(
                            subjectName: stat.failed ? "\(stat.subjectName) · 과락" : stat.subjectName,
                            correct: stat.correct,
                            total: stat.total
                        )
                        if idx < graded.subjectScores.count - 1 {
                            Rectangle()
                                .fill(Color.appBorder)
                                .frame(height: 1)
                        }
                    }
                }
            }
        }
    }

    private var reviewSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            AppSectionHeader(title: "문항별 검토")
            ForEach(reviewItems, id: \.questionId) { item in
                AnswerReviewRow(item: item)
            }
        }
    }

    private var reviewItems: [AnswerReviewRow.Item] {
        let questionByid: [Int64: MockExamQuestionItem] = Dictionary(
            uniqueKeysWithValues: questions.map { ($0.id, $0) }
        )
        return graded.items.enumerated().map { idx, ans in
            let q = questionByid[ans.questionId]
            return AnswerReviewRow.Item(
                questionId: ans.questionId,
                displayOrder: q?.displayOrder ?? (idx + 1),
                content: q?.content ?? "문제 #\(ans.questionId)",
                selectedOption: ans.selectedOption,
                correctOption: ans.correctOption,
                isCorrect: ans.correct
            )
        }
    }
}
