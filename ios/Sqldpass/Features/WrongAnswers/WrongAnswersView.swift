import SwiftUI

struct WrongAnswersView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "오답 노트 준비 중",
                systemImage: "arrow.uturn.left.circle",
                description: Text("Phase 3 에서 /api/wrong-answers 연결")
            )
            .background(Color.appPage)
            .navigationTitle("오답")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

#Preview {
    WrongAnswersView()
}
