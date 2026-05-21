import SwiftUI

/// 하단 5탭 (좌→우): 홈 / 모의고사 / 기출복원 / 실전 문제 / 내 정보.
///
/// 단일 진실 원천: `docs/MOBILE_UX_SPEC.md` § 1.
///
/// 컨테이너: SwiftUI `TabView` 대신 ZStack + `safeAreaInset(edge: .bottom)` + 커스텀 `CustomTabBar`.
/// 이유: iOS 18/26 의 SwiftUI TabView 가 시스템 강제로 floating pill 디자인을 그려서,
/// `UITabBarAppearance` / `.toolbarBackground` 만으로는 평면 직사각형 풀폭 탭바를 만들 수 없음.
enum MainTab: Hashable {
    case home, mockExams, pastExams, soloSolve, profile
}

struct MainTabView: View {
    @State private var selection: MainTab = .home

    var body: some View {
        ZStack(alignment: .bottom) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color.appPage)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            CustomTabBar(selection: $selection)
        }
        .tint(Color.brandPrimary)
    }

    @ViewBuilder
    private var content: some View {
        // NavigationStack 래핑 정책: HomeView / MockExamsListView / ProfileView 는 자체 NavigationStack 보유.
        // PastExamsListView / SoloHubView 는 본 컨테이너에서 NavigationStack 으로 감싼다.
        switch selection {
        case .home:
            HomeView(selectedTab: $selection)
        case .mockExams:
            MockExamsListView()
        case .pastExams:
            NavigationStack {
                PastExamsListView()
            }
        case .soloSolve:
            NavigationStack {
                SoloHubView()
            }
        case .profile:
            ProfileView()
        }
    }
}

#Preview {
    MainTabView()
}
