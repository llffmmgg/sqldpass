import SwiftUI

/// 피드백/오류 신고 작성 시트.
///
/// 사용처:
///  - Profile → 피드백 (questionId 없음, 기본 type = OTHER)
///  - SoloSolveView 더보기 메뉴 → 문제 신고 (questionId 지정, 기본 type = QUESTION_ERROR)
struct FeedbackComposeView: View {
    let initialType: FeedbackService.FeedbackType
    let questionId: Int64?

    @State private var type: FeedbackService.FeedbackType
    @State private var content: String = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var submitted = false

    @Environment(\.dismiss) private var dismiss

    init(initialType: FeedbackService.FeedbackType = .other, questionId: Int64? = nil) {
        self.initialType = initialType
        self.questionId = questionId
        self._type = State(initialValue: initialType)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("종류") {
                    Picker("종류", selection: $type) {
                        ForEach(FeedbackService.FeedbackType.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                if let questionId {
                    Section("문제") {
                        Text("문제 #\(questionId)")
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextMuted)
                    }
                }

                Section {
                    TextField("내용을 5자 이상 입력해주세요.", text: $content, axis: .vertical)
                        .lineLimit(6...12)
                } header: {
                    Text("내용")
                } footer: {
                    Text("\(content.count) / 2000자")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextSubtle)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .font(AppType.footnote)
                            .foregroundStyle(Color.semanticDanger)
                    }
                }
            }
            .navigationTitle(questionId != nil ? "문제 신고" : "피드백 보내기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await submit() }
                    } label: {
                        if isSubmitting {
                            ProgressView()
                        } else {
                            Text("보내기").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSubmit || isSubmitting)
                }
            }
            .alert(
                "전송 완료",
                isPresented: $submitted,
                actions: {
                    Button("확인", role: .cancel) { dismiss() }
                },
                message: {
                    Text("피드백이 전송됐어요. 빠른 시일 내 확인 후 답변 드릴게요.")
                }
            )
        }
    }

    private var canSubmit: Bool {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.count >= 5 && trimmed.count <= 2000
    }

    private func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        errorMessage = nil
        do {
            try await FeedbackService.create(
                type: type,
                questionId: questionId,
                content: content.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            submitted = true
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isSubmitting = false
    }
}
