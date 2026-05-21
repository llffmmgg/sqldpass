# Step 1 — UINavigationController+SwipeBack.swift 신규

## 배경

iOS NavigationStack 은 `navigationBarBackButtonHidden(true)` / `toolbar(.hidden, for: .navigationBar)` 적용 시 기본적으로 swipe-to-go-back 제스처가 비활성. 다른 앱들(인스타/카톡/슬랙) 처럼 좌→우 swipe back 을 활성화하기 위해 UINavigationController extension 으로 delegate 강제 등록. 풀이 화면은 답안 보호를 위해 swipe 가로채 confirm alert 트리거.

## 작업 디렉터리

```
ios/
```

## 변경

신규 파일: `ios/Sqldpass/App/UINavigationController+SwipeBack.swift`

구조:
- `final class SwipeBackInterceptor` singleton — `onSwipeAttempt: (() -> Void)?` 보관.
- `extension UINavigationController: UIGestureRecognizerDelegate`:
  - `viewDidLoad()` 에서 `interactivePopGestureRecognizer?.delegate = self`.
  - `gestureRecognizerShouldBegin(_:)`: `viewControllers.count > 1` 가드 → handler 있으면 호출+false, 없으면 true.

## Acceptance Criteria

1. SwipeBackInterceptor.shared 가 모든 풀이 화면에서 동일 인스턴스 참조.
2. UINavigationController extension 이 viewControllers.count <= 1 일 때 swipe 차단(루트에서 무효).
3. interceptor handler 가 nil 이면 표준 pop.
4. interceptor handler 가 있으면 swipe 차단 + handler 호출.

## 금지

- `UINavigationController` 의 다른 동작(toolbar/appearance) 변경 금지.
- delegate 를 한 화면씩 개별 set 금지.

## Status 규칙

- 성공: `completed` + summary "UINavigationController+SwipeBack.swift 신규, SwipeBackInterceptor + delegate".
- 실패: 3회 재시도 후 `error`.
