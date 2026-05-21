# Step 3 — 풀이 3 화면에 SwipeBackInterceptor handler 등록

## 배경

Step 1 이 만든 SwipeBackInterceptor 에 풀이 화면이 자기 종료 trigger 를 등록/해제. swipe 시작 시 X 버튼과 동일하게 `showExitConfirm = true` set → 기존 종료 alert 등장 → "종료" 누르면 dismiss, "계속 풀기" 누르면 닫음 = 답안 보호.

## 변경

각 풀이 화면 body 끝(.hideCustomTabBar() 뒤)에 onAppear/onDisappear 페어 추가:

```swift
.onAppear {
    SwipeBackInterceptor.shared.onSwipeAttempt = { showExitConfirm = true }
}
.onDisappear {
    SwipeBackInterceptor.shared.onSwipeAttempt = nil
}
```

대상:
- `ios/Sqldpass/Features/Solve/SolveView.swift`
- `ios/Sqldpass/Features/Solo/SoloSolveView.swift`
- `ios/Sqldpass/Features/PastExams/PastExamRunnerView.swift`

각 +6 라인.

## Acceptance Criteria

1. 3 파일 각 onAppear/onDisappear 페어 추가.
2. onAppear: handler 가 showExitConfirm = true set.
3. onDisappear: handler nil 해제 (race 방지).
4. 기존 alert("정말 종료할까요?", isPresented: $showExitConfirm) 무변경.

## 금지

- 풀이 화면 외 다른 화면에서 SwipeBackInterceptor.shared.onSwipeAttempt set 금지.
- handler 가 dismiss() 직접 호출 금지 — confirm alert 가 dismiss 책임.

## Status 규칙

- 성공: `completed` + summary "풀이 3 화면에 SwipeBackInterceptor handler 등록/해제 +18 라인".
- 실패: 3회 재시도 후 `error`.
