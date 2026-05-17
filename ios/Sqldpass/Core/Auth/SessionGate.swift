import SwiftUI

/// 인증 상태에 따라 AuthView ↔ MainTabView 를 스왑한다.
/// AuthStore.shared 의 `isAuthenticated` 변화를 SwiftUI 가 자동 추적.
struct SessionGate: View {
    @State private var auth = AuthStore.shared

    var body: some View {
        Group {
            if auth.isAuthenticated {
                MainTabView()
                    .transition(.opacity)
            } else {
                AuthView()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: auth.isAuthenticated)
    }
}

#Preview {
    SessionGate()
}
