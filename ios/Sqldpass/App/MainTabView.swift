import SwiftUI

enum MainTab: Hashable {
    case dashboard, mockExams, wrongAnswers, insights, profile
}

/// SoloSolveView push 용 식별자 — sheet(item:) 바인딩.
struct SoloSolveContext: Identifiable, Hashable {
    let id: Int64       // subjectId
    let name: String
}

struct MainTabView: View {
    @State private var selection: MainTab = .dashboard
    @State private var soloContext: SoloSolveContext? = nil

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                DashboardView()
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button {
                                // TODO(별 phase): 실제 과목 선택 진입 — 본 step 은 SoloSolveView 자체 흐름 검증용 임시 진입.
                                soloContext = SoloSolveContext(id: 1, name: "랜덤 풀이")
                            } label: {
                                Image(systemName: "play.circle.fill")
                                    .foregroundStyle(Color.brandPrimary)
                            }
                            .accessibilityLabel("10문제 풀기")
                        }
                    }
            }
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
        .fullScreenCover(item: $soloContext) { ctx in
            NavigationStack {
                SoloSolveView(viewModel: SoloSolveViewModel(subjectId: ctx.id, subjectName: ctx.name))
            }
        }
    }
}

#Preview {
    MainTabView()
}
