# Step 1 — Color+Tokens.swift: appPage 라이트 순백 통일

## 배경

라이트 모드의 `Color.appPage = Color(light: "#fafafa", dark: "#121212")` 는 미세하게 회색기 있는 off-white. 사용자가 실기기에서 탭바 주변 "애매한 흰색" 띠로 인식. 사용자 결정: "초록색(`brandPrimary`) 영역이 아닌 곳은 그냥 흰색으로".

## 작업 디렉터리

```
ios/
```

## 변경

`ios/Sqldpass/Core/DesignSystem/Color+Tokens.swift:22`:
- Before: `static let appPage = Color(light: "#fafafa", dark: "#121212")`
- After:  `static let appPage = Color(light: "#ffffff", dark: "#121212")`

## Acceptance Criteria

1. 라이트 #ffffff, 다크 #121212 유지.
2. 다른 토큰 무변경.
3. `appSurface` 와 라이트 모드 동색이지만 카드 보더로 분리 유지.

## 금지

- `appSurface`, `appElevated`, `appSurfaceHover` 등 다른 surface 토큰 변경 금지. **이유**: 사용자 명시 범위는 페이지 톤만.
- 색 계열 변경 금지(메모리 정책 `feedback_color_token_changes`). 본 변경은 같은 흰색 계열 내 shade 조정이라 정책 부합.

## Status 규칙

- 성공: `completed` + summary "appPage 라이트 #fafafa → #ffffff 1 라인 교체".
- 실패: 3회 재시도 후 `error`.
