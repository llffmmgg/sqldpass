# Step 1 — MainTabView: HideCustomTabBarKey + hideTabBar 분기

## 배경

직전 커밋 `73ba3e0` 의 CustomTabBar 는 `.safeAreaInset(.bottom)` 으로 모든 자식 위에 깔린다. NavigationStack push 로 진입하는 풀이 화면 3 개도 자체 `.safeAreaInset(.bottom) { ActionBar }` 를 갖고 있어 두 바가 같은 좌표에 겹쳐 ActionBar 아래쪽이 가려진다.

본 step 은 부모(MainTabView) 가 자식의 "탭바 숨겨" 신호를 받아 inset content 를 빈 View 로 분기. SwiftUI PreferenceKey 표준 패턴.

## 작업 디렉터리

```
ios/
```

## 변경

`ios/Sqldpass/App/MainTabView.swift`:
- `@State private var hideTabBar: Bool = false` 추가
- `safeAreaInset(.bottom) { content }` 안에서 `if !hideTabBar { CustomTabBar(selection: $selection) }` 분기
- `.onPreferenceChange(HideCustomTabBarKey.self) { hide in withTransaction(disableAnimations) { hideTabBar = hide } }`
- 파일 하단에 `struct HideCustomTabBarKey: PreferenceKey` + `extension View { func hideCustomTabBar(_:) }` 추가

## 동작

- 풀이 화면 push → 자식이 `.hideCustomTabBar()` preference 전파 → MainTabView 가 hide=true → safeAreaInset content 빈 View → inset 영역 0 으로 → home indicator 영역 자식 ActionBar 가 자연 흡수.
- 풀이 화면 pop → preference defaultValue(false) 적용 → hide=false → CustomTabBar 복원.
- Transaction.disablesAnimations 로 등장/사라짐 시각 튐 방지.

## Acceptance Criteria

1. `HideCustomTabBarKey` PreferenceKey 정의 + `reduce` 가 `||` 연산.
2. `View.hideCustomTabBar(_ hide: Bool = true)` extension 노출.
3. `MainTabView` 의 safeAreaInset content 가 hideTabBar 분기.
4. `onPreferenceChange` 가 Transaction.disablesAnimations 안에서 set.
5. 기존 CustomTabBar/HomeView/MockExamsListView/등 무변경 시 동작 100% 동일.

## 금지

- CustomTabBar 자체 변경 금지. **이유**: 본 phase 범위는 컨테이너만.
- `Color.appPage` 토큰 변경 금지. **이유**: 직전 phase 의 흰색 통일 유지.
- hideTabBar 를 Environment 로 대체 금지 (PreferenceKey 가 자식→부모 신호의 정석).
- 풀이 화면 진입 라우팅(navigationDestination) 변경 금지. **이유**: 본 phase 는 탭바 가시성만.

## Status 규칙

- 성공: `completed` + summary "MainTabView 에 HideCustomTabBarKey + hideTabBar 분기 추가".
- 실패: 3회 재시도 후 `error`.
