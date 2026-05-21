import UIKit

/// SwiftUI ↔ UIKit 브릿지 — 풀이 화면이 swipe back 시작을 가로채 자체 confirm alert 트리거.
///
/// 사용법:
///   - 풀이 화면 `.onAppear { SwipeBackInterceptor.shared.onSwipeAttempt = { showExitConfirm = true } }`
///   - `.onDisappear { SwipeBackInterceptor.shared.onSwipeAttempt = nil }`
///
/// 풀이 화면 외 일반 push 자식은 handler 가 nil → swipe back 표준 동작(즉시 pop).
final class SwipeBackInterceptor {
    static let shared = SwipeBackInterceptor()
    private init() {}

    var onSwipeAttempt: (() -> Void)?
}

/// 앱 전체 `NavigationStack` 의 swipe back 활성화.
///
/// SwiftUI `NavigationStack` 은 내부적으로 UIKit `UINavigationController` 를 사용한다.
/// `navigationBarBackButtonHidden(true)` / `toolbar(.hidden, for: .navigationBar)` 적용 시
/// 기본적으로 `interactivePopGestureRecognizer.delegate` 가 nil 또는 자동 차단되어
/// 좌→우 swipe back 이 동작하지 않는데, 본 extension 으로 모든 NavigationController 에
/// delegate 를 self 로 강제 등록해 navbar 가시성과 무관하게 swipe back 활성화.
extension UINavigationController: UIGestureRecognizerDelegate {
    open override func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard viewControllers.count > 1 else { return false }
        if let handler = SwipeBackInterceptor.shared.onSwipeAttempt {
            handler()
            return false   // 풀이 화면은 swipe 차단 + alert 트리거.
        }
        return true        // 일반 화면은 표준 pop.
    }
}
