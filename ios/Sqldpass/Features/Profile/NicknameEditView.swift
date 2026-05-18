import SwiftUI

struct NicknameEditView: View {
    let current: String
    let onUpdated: (String) -> Void

    @State private var input: String = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss
    @FocusState private var focused: Bool

    var body: some View {
        Form {
            Section {
                TextField("닉네임", text: $input)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focused)
                    .submitLabel(.done)
                    .onSubmit {
                        Task { await submit() }
                    }
            } footer: {
                Text("2~20자, 다른 사용자에게 보입니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
        }
        .navigationTitle("프로필 편집")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await submit() }
                } label: {
                    if isSubmitting {
                        ProgressView()
                    } else {
                        Text("저장")
                            .fontWeight(.semibold)
                    }
                }
                .disabled(!isValid || isSubmitting)
            }
        }
        .onAppear {
            input = current
            focused = true
        }
    }

    private var isValid: Bool {
        let trimmed = input.trimmingCharacters(in: .whitespaces)
        return trimmed.count >= 2 && trimmed.count <= 20 && trimmed != current
    }

    private func submit() async {
        guard isValid else { return }
        isSubmitting = true
        errorMessage = nil
        let nickname = input.trimmingCharacters(in: .whitespaces)
        do {
            _ = try await MemberService.updateNickname(nickname)
            onUpdated(nickname)
            dismiss()
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isSubmitting = false
    }
}
