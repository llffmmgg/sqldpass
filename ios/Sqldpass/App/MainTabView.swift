import SwiftUI

/// 하단 5탭 (좌→우): 홈 / 모의고사 / 기출복원 / 실전 문제 / 내 정보.
///
/// 단일 진실 원천: `docs/MOBILE_UX_SPEC.md` § 1.
///
/// 컨테이너: SwiftUI `TabView` 대신 ZStack + `safeAreaInset(edge: .bottom)` + 커스텀 `CustomTabBar`.
/// 이유: iOS 18/26 의 SwiftUI TabView 가 시스템 강제로 floating pill 디자인을 그려서,
/// `UITabBarAppearance` / `.toolbarBackground` 만으로는 평면 직사각형 풀폭 탭바를 만들 수 없음.
///
/// 풀이 화면처럼 fullscreen-like 화면은 `.hideCustomTabBar()` 한 줄로 탭바 숨김 요청 가능.
/// `HideCustomTabBarKey` PreferenceKey 가 자식 → 부모로 신호 전파.
enum MainTab: Hashable {
    case home, mockExams, pastExams, soloSolve, profile
}

struct MainTabView: View {
    @State private var selection: MainTab = .home
    @State private var hideTabBar: Bool = false

    var body: some View {
        ZStack(alignment: .bottom) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color.appPage)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if !hideTabBar {
                CustomTabBar(selection: $selection)
            }
        }
        .onPreferenceChange(HideCustomTabBarKey.self) { hide in
            // push/pop 순간 탭바 등장/사라짐이 튀지 않도록 애니메이션 끔.
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) {
                hideTabBar = hide
            }
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

// MARK: - Tab bar visibility preference

/// 풀이 화면 등 fullscreen-like 화면이 MainTabView 에게 "탭바 숨겨" 라고 신호하는 키.
///
/// 사용 측: `.hideCustomTabBar()` modifier 한 줄. NavigationStack push 시 자동으로 true,
/// pop 시 defaultValue(false) 로 복원.
struct HideCustomTabBarKey: PreferenceKey {
    static var defaultValue: Bool = false
    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

extension View {
    /// 풀이 화면 등이 부모 MainTabView 에게 CustomTabBar 숨김을 요청.
    func hideCustomTabBar(_ hide: Bool = true) -> some View {
        preference(key: HideCustomTabBarKey.self, value: hide)
    }
}

#Preview {
    MainTabView()
}
