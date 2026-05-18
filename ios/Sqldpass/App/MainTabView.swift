import SwiftUI

enum MainTab: Hashable {
    case dashboard, mockExams, wrongAnswers, profile
}

struct MainTabView: View {
    @State private var selection: MainTab = .dashboard

    var body: some View {
        TabView(selection: $selection) {
            DashboardView()
                .tabItem {
                    Label("홈", systemImage: "house.fill")
                }
                .tag(MainTab.dashboard)

            MockExamsListView()
                .tabItem {
                    Label("모의고사", systemImage: "doc.text.image.fill")
                }
                .tag(MainTab.mockExams)

            WrongAnswersView()
                .tabItem {
                    Label("오답", systemImage: "arrow.uturn.left.circle.fill")
                }
                .tag(MainTab.wrongAnswers)

            ProfileView()
                .tabItem {
                    Label("내정보", systemImage: "person.circle.fill")
                }
                .tag(MainTab.profile)
        }
        .tint(.brandPrimary)
    }
}

#Preview {
    MainTabView()
}
