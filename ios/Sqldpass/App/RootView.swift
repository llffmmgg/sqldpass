import SwiftUI

/// 최상위 컨테이너. SessionGate 가 인증 상태에 따라 AuthView ↔ MainTabView 를 스왑.
struct RootView: View {
    var body: some View {
        SessionGate()
    }
}

#Preview("Authenticated") {
    let _ = AuthStore.shared.signIn(token: "preview-token", nickname: "데모유저")
    return RootView()
}

#Preview("Unauthenticated") {
    let _ = AuthStore.shared.signOut()
    return RootView()
}
