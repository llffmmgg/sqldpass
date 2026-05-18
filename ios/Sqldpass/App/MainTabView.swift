import SwiftUI

enum MainTab: Hashable {
    case dashboard, mockExams, wrongAnswers, insights, profile
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
                    Label("모의고사", systemImage: "doc.text.fill")
                }
                .tag(MainTab.mockExams)

            WrongAnswersView()
                .tabItem {
                    Label("오답노트", systemImage: "arrow.uturn.backward.circle.fill")
                }
                .tag(MainTab.wrongAnswers)

            NavigationStack {
                InsightsView()
            }
            .tabItem {
                Label("인사이트", systemImage: "chart.line.uptrend.xyaxis")
            }
            .tag(MainTab.insights)

            ProfileView()
                .tabItem {
                    Label("마이", systemImage: "book.closed.fill")
                }
                .tag(MainTab.profile)
        }
        .tint(.brandPrimary)
    }
}

#Preview {
    MainTabView()
}
