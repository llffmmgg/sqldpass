import UIKit

/// 햅틱 헬퍼 — `docs/SOLVE_SCREEN_SPEC.md` 의 햅틱 매트릭스를 1:1 매핑.
///
/// 호출은 메인 스레드(UIImpactFeedbackGenerator 가 main-thread API).
/// 풀이 화면 외에도 결제 완료 등에서 재사용 가능.
enum Haptics {
    static func light() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }
    static func medium() {
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
    }
    static func success() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
    static func warning() {
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
    }
    static func error() {
        UINotificationFeedbackGenerator().notificationOccurred(.error)
    }
}
