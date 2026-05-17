import SwiftUI

struct AccountDeletionConfirmView: View {
    @State private var checked = false
    @State private var isDeleting = false
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                header

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    Text("삭제 시 즉시 적용됩니다:")
                        .font(AppType.bodyEmph)
                    BulletItem(text: "프로필, 풀이 기록, 오답노트, 북마크 모두 영구 삭제")
                    BulletItem(text: "구독 중이라도 환불은 자동 처리되지 않음 (별도 문의)")
                    BulletItem(text: "동일 소셜 계정으로 재가입은 가능, 데이터 복구 불가")
                }
                .padding(Spacing.base)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.lg)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.lg))

                Toggle(isOn: $checked) {
                    Text("위 내용을 확인했습니다")
                        .font(AppType.body)
                }
                .tint(Color.semanticDanger)

                if let errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle("계정 삭제")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button(role: .destructive) {
                Task { await delete() }
            } label: {
                if isDeleting {
                    ProgressView().frame(maxWidth: .infinity).frame(height: 52)
                } else {
                    Text("영구 삭제하기")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.semanticDanger)
            .disabled(!checked || isDeleting)
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.sm)
            .background(Color.appPage)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.largeTitle)
                .foregroundStyle(Color.semanticDanger)
            Text("정말 계정을 삭제할까요?")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("이 작업은 되돌릴 수 없습니다.")
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
        }
    }

    private func delete() async {
        isDeleting = true
        errorMessage = nil
        do {
            try await MemberService.deleteAccount()
            AuthStore.shared.signOut()
        } catch let error as APIError {
            errorMessage = error.errorDescription
            isDeleting = false
        } catch {
            errorMessage = error.localizedDescription
            isDeleting = false
        }
    }
}

private struct BulletItem: View {
    let text: String
    var body: some View {
        HStack(alignment: .top, spacing: Spacing.xs) {
            Text("•")
                .foregroundStyle(Color.appTextSubtle)
            Text(text)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
