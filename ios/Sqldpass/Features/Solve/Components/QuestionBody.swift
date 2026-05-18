import SwiftUI

/// 모의고사 풀이(SolveView) 의 문제 카드.
///
/// 기존에는 단순 `Text(question.content)` 로 본문을 표시해 Markdown / 표 / SVG / 코드블록이
/// 모두 깨졌다. 본 컴포넌트는 `QuestionContentView` 를 호출해 분리 렌더.
struct QuestionBody: View {
    let question: MockExamQuestionItem

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text(question.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
            Text("문제 \(question.displayOrder)")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
            QuestionContentView(text: question.content)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.base)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}
