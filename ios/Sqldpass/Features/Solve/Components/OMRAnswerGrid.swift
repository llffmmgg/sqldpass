import SwiftUI

struct OMRAnswerGrid: View {
    let question: MockExamQuestionItem
    let selectedOption: Int?
    let onSelect: (Int) -> Void

    private let choices = [1, 2, 3, 4]

    @State private var feedbackTrigger: Int = 0

    var body: some View {
        VStack(spacing: Spacing.sm) {
            ForEach(choices, id: \.self) { value in
                Button {
                    feedbackTrigger &+= 1
                    onSelect(value)
                } label: {
                    HStack(spacing: Spacing.md) {
                        ZStack {
                            Circle()
                                .stroke(
                                    selectedOption == value ? Color.brandPrimary : Color.appBorder,
                                    lineWidth: selectedOption == value ? 2 : 1
                                )
                                .frame(width: 32, height: 32)
                            Text("\(value)")
                                .font(AppType.bodyEmph)
                                .foregroundStyle(
                                    selectedOption == value ? Color.brandPrimary : Color.appTextPrimary
                                )
                        }
                        Text("\(value)번")
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextPrimary)
                        Spacer()
                    }
                    .padding(Spacing.md)
                    .background(
                        selectedOption == value ? Color.brandPrimary.opacity(0.1) : Color.appSurface
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(
                                selectedOption == value ? Color.brandPrimary : Color.appBorder,
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(value)번 선택")
                .accessibilityAddTraits(selectedOption == value ? .isSelected : [])
            }
        }
        .sensoryFeedback(.selection, trigger: feedbackTrigger)
    }
}
