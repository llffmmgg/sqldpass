# Step 3 — MainTabView.swift 전체 교체

## 배경

기존 `MainTabView.swift` 는 SwiftUI `TabView` + `init()` 안 `UITabBarAppearance` 설정으로 평면 직사각형을 시도했으나 iOS 18/26 시스템 강제로 floating pill 이 적용됨. Step 2 의 `CustomTabBar` 와 결합해 컨테이너를 ZStack + safeAreaInset 패턴으로 재작성.

## 작업 디렉터리

```
ios/
```

## 변경

`ios/Sqldpass/App/MainTabView.swift` 전체 교체.

핵심:
- `init()` 삭제 — `UITabBarAppearance` 설정은 `TabView` 미사용 시 무의미.
- `enum MainTab` 그대로 유지.
- body: `ZStack(alignment: .bottom) { content }` + `.background(Color.appPage)` + `.safeAreaInset(edge: .bottom, spacing: 0) { CustomTabBar(selection: $selection) }` + `.tint(Color.brandPrimary)`.
- `@ViewBuilder private var content` 에서 switch selection 으로 5 화면 분기. `pastExams`/`soloSolve` 만 `NavigationStack { ... }` 으로 래핑 (기존 정책 유지).
- HomeView 의 `selectedTab` binding 은 `$selection` 그대로 전달 (다른 탭으로 점프 기능 보존).

## Acceptance Criteria

1. SwiftUI `TabView` 미사용.
2. `UITabBarAppearance` / `UITabBar.appearance()` 호출 없음.
3. `safeAreaInset(edge: .bottom)` 으로 CustomTabBar 가 home indicator 영역까지 자동 흡수 → 흰 띠 0.
4. NavigationStack 래핑은 `pastExams`, `soloSolve` 만 (기존 정책).
5. 비활성 탭은 switch unmount (회귀 발견 시 ZStack 옵션으로 후속 fix).
6. tint `brandPrimary` 유지.

## 금지

- SwiftUI `TabView` 재도입 금지.
- 비활성 탭에 force-load (preload) 추가 금지 — 단순 switch 로 유지.
- 그림자/블러/glow 추가 금지.

## Status 규칙

- 성공: `completed` + summary "MainTabView.swift 전체 교체. ZStack + safeAreaInset + switch 분기. UITabBarAppearance 제거".
- 실패: 3회 재시도 후 `error`.
