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
        let questionType: String
        let selectedOption: Int?
        let correctOption: Int?
        let isCorrect: Bool
        let submittedAnswerText: String?
        let answer: String?
        let keywords: [String]
        let explanation: String?
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
                let parsed = QuestionParser.parseNormalized(item.content)
                let bodyText = parsed.options.isEmpty || parsed.body.isEmpty
                    ? item.content
                    : parsed.body
                VStack(alignment: .leading, spacing: 0) {
                    Rectangle()
                        .fill(Color.appBorder)
                        .frame(height: 1)
                    VStack(alignment: .leading, spacing: Spacing.md) {
                        QuestionContentView(text: bodyText)

                        if isMCQ && !parsed.options.isEmpty {
                            VStack(spacing: Spacing.sm) {
                                ForEach(Array(parsed.options.enumerated()), id: \.offset) { idx, option in
                                    let optionNumber = idx + 1
                                    AppOptionRow(
                                        optionNumber: optionNumber,
                                        optionText: option,
                                        state: .revealed(
                                            isCorrect: item.correctOption == optionNumber,
                                            wasSelected: item.selectedOption == optionNumber
                                        )
                                    )
                                }
                            }
                        } else {
                            textAnswerReview
                        }

                        if let explanation = item.explanation, !explanation.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Divider()
                            Text("해설")
                                .font(AppType.bodyEmph)
                                .foregroundStyle(Color.appTextPrimary)
                            QuestionContentView(text: explanation)
                        }
                    }
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

    private var isTextAnswerType: Bool {
        item.questionType.uppercased().contains("SHORT")
            || item.questionType.uppercased().contains("DESCRIPTIVE")
            || item.questionType.uppercased().contains("TEXT")
    }

    private var isMCQ: Bool {
        !isTextAnswerType
    }

    @ViewBuilder
    private var textAnswerReview: some View {
        if isTextAnswerType || item.submittedAnswerText != nil || item.answer != nil || !item.keywords.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("내 답")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)
                Text(item.submittedAnswerText?.nilIfBlank ?? "미응답")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                if let answer = item.answer?.nilIfBlank {
                    Text("모범답안")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.appTextMuted)
                        .padding(.top, Spacing.xs)
                    QuestionContentView(text: answer)
                }

                if !item.keywords.isEmpty {
                    Text("키워드: \(item.keywords.joined(separator: ", "))")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                }
            }
            .padding(Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appElevated)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.md)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
        }
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
