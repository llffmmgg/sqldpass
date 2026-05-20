import SwiftUI
import UIKit

/// 하단 5탭 (좌→우): 홈 / 모의고사 / 기출복원 / 실전 문제 / 마이.
///
/// 단일 진실 원천: `docs/MOBILE_UX_SPEC.md` § 1.
/// 변경 이력 (phase `mobile-ux-restructure` step 5):
///  - `.dashboard` → `.home` 으로 교체, `DashboardView()` 호출은 `HomeView()` 로 대체.
///  - `.wrongAnswers`, `.insights` 탭 제거 — ProfileView 안 NavigationLink 으로 진입 (step 7).
///  - `.pastExams` 탭 신규 — `PastExamsListView`.
///  - `.soloSolve` 탭 신규 — `SoloHubView` (Android `SolveTab` 동등).
///  - `fullScreenCover` 기반 임시 SoloSolveContext 진입 제거 — SoloHubView 의 NavigationLink push 로 일원화.
enum MainTab: Hashable {
    case home, mockExams, pastExams, soloSolve, profile
}

struct MainTabView: View {
    @State private var selection: MainTab = .home

    init() {
        // TabBar 평면 직사각형 강제 — iOS 18 floating glass 톤 끔.
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground
        appearance.backgroundEffect = nil   // 반투명 블러 제거
        appearance.shadowColor = .clear     // 상단 hairline 제거
        appearance.shadowImage = UIImage()
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView(selection: $selection) {
            // HomeView / MockExamsListView / ProfileView 는 자체 NavigationStack 을 갖고 있어
            // 추가로 감싸지 않는다. PastExamsListView / SoloHubView 는 본 step 신규로 만들면서
            // NavigationStack 을 MainTabView 쪽에 둔다 — 자식 화면이 더 단순해진다.
            HomeView(selectedTab: $selection)
                .tabItem {
                    Label("홈", systemImage: "house.fill")
                }
                .tag(MainTab.home)

            MockExamsListView()
                .tabItem {
                    Label("모의고사", systemImage: "doc.text.fill")
                }
                .tag(MainTab.mockExams)

            NavigationStack {
                PastExamsListView()
            }
            .tabItem {
                Label("기출복원", systemImage: "clock.arrow.circlepath")
            }
            .tag(MainTab.pastExams)

            NavigationStack {
                SoloHubView()
            }
            .tabItem {
                Label("실전 문제", systemImage: "play.circle.fill")
            }
            .tag(MainTab.soloSolve)

            ProfileView()
                .tabItem {
                    Label("내 정보", systemImage: "person.crop.circle.fill")
                }
                .tag(MainTab.profile)
        }
        .tint(.brandPrimary)
        // iOS 18 의 floating glass tab bar 효과 끔 — 평면 불투명 강제.
        .toolbarBackground(Color.appSurface, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
    }
}

#Preview {
    MainTabView()
}
