import SwiftUI

struct AnswerReviewRow: View {
    struct Item {
        let questionId: Int64
        let displayOrder: Int
        let content: String
        let selectedOption: Int?
        let correctOption: Int?
        let isCorrect: Bool
    }

    let item: Item

    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Button {
                isExpanded.toggle()
            } label: {
                HStack(alignment: .top, spacing: Spacing.sm) {
                    Image(systemName: item.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(item.isCorrect ? Color.semanticSuccess : Color.semanticDanger)
                        .padding(.top, 2)
                    VStack(alignment: .leading, spacing: Spacing.xxs) {
                        Text("문제 \(item.displayOrder)")
                            .font(AppType.bodyEmph)
                            .foregroundStyle(Color.appTextPrimary)
                        Text("제출 답안: \(submittedAnswerText)")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextMuted)
                        if let correctOption = item.correctOption, !item.isCorrect {
                            Text("정답: \(correctOption)번")
                                .font(AppType.footnote)
                                .foregroundStyle(Color.semanticSuccess)
                        }
                    }
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.footnote)
                        .foregroundStyle(Color.appTextSubtle)
                }
            }
            .buttonStyle(.plain)

            if isExpanded {
                QuestionContentView(text: item.content)
                    .padding(.top, Spacing.xs)
            }
        }
        .padding(Spacing.md)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }

    private var submittedAnswerText: String {
        guard let selectedOption = item.selectedOption else { return "미응답" }
        return "\(selectedOption)번"
    }
}
