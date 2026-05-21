# Step 2 — CustomTabBar.swift 신규

## 배경

SwiftUI `TabView` 가 iOS 18/26 시스템 강제로 floating pill 모양을 그리므로 평면 직사각형 풀폭 탭바를 만들 수 없음. UIKit `UITabBarAppearance` / SwiftUI `.toolbarBackground` 는 색/투명/hairline 만 제어. 본 step 은 시스템 TabView 우회용 커스텀 바 컴포넌트를 만든다.

## 작업 디렉터리

```
ios/
```

## 변경

신규 파일: `ios/Sqldpass/App/CustomTabBar.swift` (~70 라인)

구조:
- `struct CustomTabBar: View` + `@Binding var selection: MainTab`
- private `struct Item { tab, label, icon }` + static `items: [Item]` 5 개 (홈/모의고사/기출복원/실전 문제/내 정보, systemImage 기존 MainTabView 와 1:1).
- body: VStack(spacing: 0) { 1px 상단 분리선 Rectangle + HStack 5 버튼 } + `.background(Color.appPage)`.
- 각 버튼: `VStack { Image(systemName: icon).font(.system(size: 22)) + Text(label).font(.system(size: 10, weight: ...)) }` + `.foregroundStyle(isSelected ? brandPrimary : appTextMuted)` + `.frame(maxWidth: .infinity)` + `.contentShape(Rectangle())`.
- 탭 전환은 기존 MainTabView 의 `Transaction.disablesAnimations` + `UIView.performWithoutAnimation` 패턴 그대로.

## Acceptance Criteria

1. 시스템 `TabView` 미사용.
2. 5 버튼 균등 분포 (`maxWidth: .infinity`).
3. 선택 색 `brandPrimary`, 비선택 `appTextMuted`.
4. 상단 1px `appBorder.opacity(0.5)` 분리선.
5. 그림자/블러/glow 없음 (AGENTS.md "AI 스러운 흐릿한 효과 금지").
6. 디자인 토큰만 사용 (`Color(hex:)` 직접 호출 금지).

## 금지

- 그림자/블러/glow 추가 금지.
- `Color(hex:)` 직접 호출 금지.
- 아이콘 systemImage 변경 금지 (기존 MainTabView 와 1:1 유지).

## Status 규칙

- 성공: `completed` + summary "CustomTabBar.swift 신규 ~70 라인 + 5 Item + Transaction.disablesAnimations 전환".
- 실패: 3회 재시도 후 `error`.
