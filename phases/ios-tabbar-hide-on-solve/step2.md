# Step 2 — 풀이 3 화면에 .hideCustomTabBar() 추가

## 배경

Step 1 의 PreferenceKey 신호를 풀이 화면 3 개에서 발신.

## 작업 디렉터리

```
ios/
```

## 변경

| 파일 | 위치 | 추가 |
|---|---|---|
| `ios/Sqldpass/Features/Solve/SolveView.swift` | body ZStack 의 `.animation(...)` 뒤 | `.hideCustomTabBar()` |
| `ios/Sqldpass/Features/Solo/SoloSolveView.swift` | body Group 의 `.sheet(item: ...)` 뒤 | `.hideCustomTabBar()` |
| `ios/Sqldpass/Features/PastExams/PastExamRunnerView.swift` | body Group 의 "정말 종료할까요?" `.alert` 뒤 | `.hideCustomTabBar()` |

각 파일 +1 라인. body 의 최외곽 modifier chain 끝에 추가 — graded/loading/result 분기까지 포함해 풀이 전체 흐름에서 탭바 숨김 유지.

## Acceptance Criteria

1. 3 파일 각각 `.hideCustomTabBar()` 1 줄 추가.
2. body 최외곽 modifier chain 끝 위치(분기 내부가 아니라).
3. 다른 modifier 무변경.
4. 컴파일 에러 0 (Step 1 의 extension 이 같은 모듈에 정의됨).

## 금지

- body 내부 분기(예: Group { if ... { ... } else { ... } }) 안의 특정 case 에만 적용 금지. **이유**: graded/loading 상태에서도 탭바 숨김 일관성 유지.
- ActionBar 자체 변경 금지.

## Status 규칙

- 성공: `completed` + summary "3 풀이 화면 body 끝에 .hideCustomTabBar() 1 줄씩 추가".
- 실패: 3회 재시도 후 `error`.
