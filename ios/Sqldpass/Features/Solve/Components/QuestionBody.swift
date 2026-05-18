import SwiftUI

/// 모의고사 풀이 화면 문제 카드 (Inked OMR 디자인 시스템 chrome).
///
/// 본문 렌더링은 frozen 한 `QuestionContentView` 가 그대로 담당한다.
/// `parsed.body` 가 있으면 본문만 분리 표시, 없으면 원본 content 그대로 표시.
struct QuestionBody: View {
    let question: MockExamQuestionItem

    var body: some View {
        let parsed = QuestionParser.parse(question.content)
        let bodyText = parsed.options.isEmpty || parsed.body.isEmpty
            ? question.content
            : parsed.body

        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                AppBadge(label: question.subjectName, tone: .accent)
                AppBadge(label: "문제 \(question.displayOrder)", tone: .neutral)
                Spacer(minLength: 0)
            }

            AppCard(surface: .card) {
                QuestionContentView(text: bodyText)
                    .textSelection(.enabled)
            }
        }
    }
}
