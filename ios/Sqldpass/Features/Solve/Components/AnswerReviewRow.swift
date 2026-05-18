import SwiftUI

struct AnswerReviewRow: View {
    struct Item {
        let questionId: Int64
        let displayOrder: Int
        let content: String
        let chosenAnswer: String?
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
                        Text("내 답: \(item.chosenAnswer ?? "미응답")")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextMuted)
                    }
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.footnote)
                        .foregroundStyle(Color.appTextSubtle)
                }
            }
            .buttonStyle(.plain)

            if isExpanded {
                Text(item.content)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
                    .padding(.top, Spacing.xs)
                Text("자세한 해설은 곧 지원 예정입니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
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
}
