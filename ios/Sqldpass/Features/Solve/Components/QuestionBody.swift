import SwiftUI

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
            Text(question.content)
                .font(AppType.body)
                .foregroundStyle(Color.appTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
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
