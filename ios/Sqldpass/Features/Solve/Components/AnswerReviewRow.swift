import SwiftUI

/// 문항별 검토 행 (Inked OMR 디자인 시스템).
///
/// `AppListRow` 를 메인 row 로 쓰고, 탭하면 아래에 frozen `QuestionContentView` 본문이 펼쳐진다.
/// 본문은 절대 lineLimit 으로 자르지 않는다. (subtitle 의 짧은 요약만 1 줄 처리)
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
        VStack(alignment: .leading, spacing: 0) {
            AppListRow(
                title: "문제 \(item.displayOrder)",
                subtitle: subtitle,
                leadingSystemImage: item.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill",
                leadingTint: item.isCorrect ? .semanticSuccess : .semanticDanger,
                trailingSystemImage: isExpanded ? "chevron.up" : "chevron.down",
                onTap: {
                    Haptics.light()
                    withAnimation(.easeOut(duration: 0.2)) {
                        isExpanded.toggle()
                    }
                }
            )

            if isExpanded {
                VStack(alignment: .leading, spacing: 0) {
                    Rectangle()
                        .fill(Color.appBorder)
                        .frame(height: 1)
                    QuestionContentView(text: item.content)
                        .padding(Spacing.base)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.appSurface)
                }
            }
        }
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var subtitle: String {
        let submitted = item.selectedOption.map { "\($0)번" } ?? "미응답"
        if let correct = item.correctOption {
            return "정답 \(correct)번 · 선택 \(submitted)"
        }
        return "선택 \(submitted)"
    }
}
