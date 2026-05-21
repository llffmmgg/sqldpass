import SwiftUI

struct WrongAnswerRetrySheet: View {
    let item: WrongAnswer
    let onMastered: () -> Void

    @State private var selectedOption: Int?
    @State private var answerText = ""
    @State private var result: WrongAnswerRetryResult?
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss

    private let choices = [1, 2, 3, 4]

    private var parsedQuestion: ParsedQuestion {
        QuestionParser.parseNormalized(item.questionContent)
    }

    private var canSubmit: Bool {
        if item.isTextAnswerType {
            return !answerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSubmitting
        }
        return selectedOption != nil && !isSubmitting
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    Text(item.subjectName)
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                    let parsed = parsedQuestion
                    QuestionContentView(text: parsed.body.isEmpty ? item.questionContent : parsed.body)

                    if let result {
                        resultBlock(result)
                    } else if item.isTextAnswerType {
                        textAnswerBlock
                    } else {
                        choiceBlock
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(AppType.footnote)
                            .foregroundStyle(Color.semanticDanger)
                    }
                }
                .padding(Spacing.base)
            }
            .navigationTitle("다시 풀기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") {
                        if result?.correct == true {
                            onMastered()
                        } else {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private var choiceBlock: some View {
        VStack(spacing: Spacing.sm) {
            let parsed = parsedQuestion
            let count = parsed.options.isEmpty ? choices.count : parsed.options.count
            ForEach(1...count, id: \.self) { value in
                let optionText = parsed.options.indices.contains(value - 1)
                    ? parsed.options[value - 1]
                    : nil
                AppOptionRow(
                    optionNumber: value,
                    optionText: optionText,
                    state: selectedOption == value ? .selected : .idle,
                    onTap: {
                        selectedOption = value
                    }
                )
                .disabled(isSubmitting)
                .opacity(isSubmitting ? 0.55 : 1)
            }

            submitButton
        }
    }

    private var textAnswerBlock: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("답안 입력")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            TextField("답안을 입력하세요", text: $answerText, axis: .vertical)
                .textFieldStyle(.plain)
                .lineLimit(3...8)
                .padding(Spacing.md)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.md)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.md))
            submitButton
        }
    }

    private var submitButton: some View {
        Button {
            Task { await submit() }
        } label: {
            if isSubmitting {
                ProgressView().frame(maxWidth: .infinity).frame(height: 48)
            } else {
                Text("제출")
                    .font(AppType.bodyEmph)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
            }
        }
        .buttonStyle(.borderedProminent)
        .tint(Color.brandPrimary)
        .disabled(!canSubmit)
    }

    private func resultBlock(_ r: WrongAnswerRetryResult) -> some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: r.correct ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(r.correct ? Color.semanticSuccess : Color.semanticDanger)
                Text(r.correct ? "정답입니다" : "틀렸어요")
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
            }
            if let option = r.correctOption {
                Text("정답: \(option)번")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
            } else if let answer = r.correctAnswer {
                Text("정답: \(answer)")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
            }
            if let explanation = r.explanation, !explanation.isEmpty {
                Divider()
                Text("해설")
                    .font(AppType.bodyEmph)
                QuestionContentView(text: explanation)
            }
            if r.correct {
                Text("이 문제는 오답노트에서 제거됩니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        errorMessage = nil
        do {
            result = try await WrongAnswerService.retry(
                questionId: item.questionId,
                selectedOption: item.isTextAnswerType ? nil : selectedOption,
                answerText: item.isTextAnswerType ? answerText : nil
            )
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isSubmitting = false
    }
}
