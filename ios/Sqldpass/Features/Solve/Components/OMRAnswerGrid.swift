import SwiftUI

struct OMRAnswerGrid: View {
    let question: MockExamQuestionItem
    let chosen: String?
    let onSelect: (String) -> Void

    /// 객관식 선택지 — 4지선다 가정. 단답형 등은 향후 확장.
    private let choices = ["1", "2", "3", "4"]

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
                                    chosen == value ? Color.brandPrimary : Color.appBorder,
                                    lineWidth: chosen == value ? 2 : 1
                                )
                                .frame(width: 32, height: 32)
                            Text(value)
                                .font(AppType.bodyEmph)
                                .foregroundStyle(
                                    chosen == value ? Color.brandPrimary : Color.appTextPrimary
                                )
                        }
                        Text("\(value)번")
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextPrimary)
                        Spacer()
                    }
                    .padding(Spacing.md)
                    .background(
                        chosen == value ? Color.brandPrimary.opacity(0.1) : Color.appSurface
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(
                                chosen == value ? Color.brandPrimary : Color.appBorder,
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(value)번 선택")
                .accessibilityAddTraits(chosen == value ? .isSelected : [])
            }
        }
        .sensoryFeedback(.selection, trigger: feedbackTrigger)
    }
}
