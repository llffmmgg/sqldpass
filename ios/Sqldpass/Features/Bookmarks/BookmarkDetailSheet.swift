import SwiftUI

struct BookmarkDetailSheet: View {
    let questionId: Int64

    @State private var question: Question?
    @State private var isLoading = true
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.md) {
                    if isLoading {
                        ProgressView().controlSize(.large).frame(maxWidth: .infinity, minHeight: 200)
                    } else if let errorMessage {
                        Text(errorMessage)
                            .font(AppType.body)
                            .foregroundStyle(Color.semanticDanger)
                    } else if let question {
                        Text("문제 #\(question.id)")
                            .font(AppType.caption.weight(.semibold))
                            .foregroundStyle(Color.brandPrimary)
                        Text(question.content)
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                            .textSelection(.enabled)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
            .navigationTitle("북마크 미리보기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .task { await load() }
        }
    }

    private func load() async {
        isLoading = true
        do {
            question = try await QuestionService.detail(id: questionId)
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
