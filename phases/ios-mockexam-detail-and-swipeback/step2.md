# Step 2 — MockExamDetailView .hideCustomTabBar()

## 배경

직전 phase `ios-tabbar-hide-on-solve` 가 풀이 화면 3 개의 정답 버튼 가림을 해결했지만, MockExamDetailView 의 `safeAreaInset(.bottom) { "시험 시작하기" Button }` 도 같은 stacking 겹침 — 버튼 아래쪽이 CustomTabBar 와 같은 좌표에 그려져 가려짐.

## 변경

`ios/Sqldpass/Features/MockExams/MockExamDetailView.swift` body 끝(.task 뒤)에 `.hideCustomTabBar()` 한 줄.

직전 phase 가 만든 `View.hideCustomTabBar()` modifier 재사용. 신규 인프라 0.

## Acceptance Criteria

1. `.hideCustomTabBar()` 1 줄 추가.
2. body 외곽 modifier chain 끝 위치.
3. "시험 시작하기" 버튼 자체 무변경.

## 금지

- 버튼 자체(.padding/.background) 변경 금지.
- `@Binding var path` 무변경.

## Status 규칙

- 성공: `completed` + summary "MockExamDetailView body 끝 .hideCustomTabBar() +1".
- 실패: 3회 재시도 후 `error`.
