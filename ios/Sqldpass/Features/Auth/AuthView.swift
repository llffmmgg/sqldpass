import AuthenticationServices
import SwiftUI

struct AuthView: View {
    @State private var isExchanging = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            Color.appPage.ignoresSafeArea()

            VStack {
                Spacer()
                logoSection
                Spacer()
            }
            .padding(.horizontal, Spacing.lg)
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: Spacing.md) {
                if let errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, Spacing.md)
                }

                SignInWithAppleButton(.signIn) { request in
                    request.requestedScopes = [.fullName, .email]
                } onCompletion: { result in
                    handleAppleResult(result)
                }
                .signInWithAppleButtonStyle(.black)
                .frame(height: 50)
                .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
                .accessibilityLabel("Apple로 로그인")
                .disabled(isExchanging)

                Button {
                    handleGoogleTap()
                } label: {
                    HStack(spacing: Spacing.sm) {
                        Image(systemName: "g.circle.fill")
                            .font(.title3)
                        Text("Google로 로그인")
                            .font(AppType.bodyEmph)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.appSurface)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.sm)
                            .stroke(Color.appBorder, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
                    .foregroundStyle(Color.appTextPrimary)
                }
                .disabled(isExchanging)

                Text("로그인하면 이용약관과 개인정보처리방침에 동의한 것으로 간주됩니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .multilineTextAlignment(.center)
                    .padding(.top, Spacing.xs)
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.bottom, Spacing.md)
            .background(Color.appPage)
        }
    }

    private var logoSection: some View {
        VStack(spacing: Spacing.lg) {
            ZStack {
                Circle()
                    .fill(Color.brandPrimary.opacity(0.12))
                    .frame(width: 96, height: 96)
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.brandPrimary)
            }
            VStack(spacing: Spacing.xs) {
                Text("문어CBT")
                    .font(AppType.display)
                    .foregroundStyle(Color.appTextPrimary)
                Text("SQLD · 정보처리기사 · 컴활")
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
            }
            .multilineTextAlignment(.center)
        }
    }

    private func handleAppleResult(_ result: Result<ASAuthorization, Error>) {
        errorMessage = nil
        switch result {
        case .success(let auth):
            guard let credential = auth.credential as? ASAuthorizationAppleIDCredential else {
                errorMessage = "Apple 인증 정보를 읽지 못했습니다."
                return
            }
            isExchanging = true
            Task {
                do {
                    try await AppleAuthService.exchange(credential: credential)
                } catch {
                    errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
                }
                isExchanging = false
            }
        case .failure(let error):
            if (error as NSError).code != ASAuthorizationError.canceled.rawValue {
                errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            }
        }
    }

    private func handleGoogleTap() {
        errorMessage = nil
        isExchanging = true
        Task {
            do {
                try await GoogleAuthService.signIn()
            } catch {
                errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            }
            isExchanging = false
        }
    }
}

#Preview {
    AuthView()
}
