# Step 4 — Color tokens: 코드블록 다크톤 surface 토큰 추가

## 배경

iOS 디자인 토큰 (`ios/Sqldpass/Core/DesignSystem/Color+Tokens.swift`) 는 라이트/다크 자동 적응으로 정의되어 있다 (`Color(light:dark:)`).
사용자 결정: 코드블록은 라이트/다크 무관 다크 톤 고정 (웹 zinc-900 정책과 동치).

`CodeBlockView.swift` 가 현재 `Color.appSurface`(라이트 `#ffffff` / 다크 `#2e2e2e`) 와 `Color.appElevated`(라이트 `#f4f4f4` / 다크 `#242424`) 를 사용해 라이트 모드에서 페이지(`appPage` `#fafafa`) 와 코드블록 본문(`appSurface` `#ffffff`) 이 거의 동색이라 분리가 약하다.

또한 Step 2 의 인라인 코드 스타일링이 참조할 인라인 코드 배경/텍스트 토큰도 같이 추가.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Core/DesignSystem/Color+Tokens.swift` | 코드 surface 토큰 4 개 + 인라인 코드 토큰 2 개 추가 (총 6 개). 라이트/다크 모두 동일 다크 값 — 코드 영역은 모드 불변. |

## 구현 상세

기존 `// MARK: Certification accents` 섹션 다음(파일 끝) 에 새 섹션 `// MARK: Code surface (다크톤 고정 — 라이트/다크 무관)` 추가:

```swift
    // MARK: Code surface (다크톤 고정 — 라이트/다크 무관, 웹 zinc-900 톤과 매칭)
    static let appCodeSurface   = Color(light: "#1e1e1e", dark: "#1e1e1e")
    static let appCodeHeader    = Color(light: "#2a2a2a", dark: "#2a2a2a")
    static let appCodeBorder    = Color(light: "#3a3a3a", dark: "#3a3a3a")
    static let appCodeText      = Color(light: "#e6e6e6", dark: "#e6e6e6")
    static let appCodeInline    = Color(light: "#1f1f1f", dark: "#1f1f1f")
    static let appCodeInlineFG  = Color(light: "#fcd34d", dark: "#fcd34d")
```

각 토큰 용도:
- `appCodeSurface` — 코드블록 본문 배경 (Step 5 CodeBlockView 본문).
- `appCodeHeader` — 코드블록 헤더(언어 칩) 배경 (Step 5).
- `appCodeBorder` — 코드블록 보더 + 헤더-본문 구분선 (Step 5).
- `appCodeText` — 코드블록 본문 기본 텍스트 (Step 5 SimpleSyntaxHighlighter fallback color).
- `appCodeInline` — 인라인 코드 배경 (Step 2 MarkdownTextView).
- `appCodeInlineFG` — 인라인 코드 텍스트 (Step 2, 웹 amber-300 톤).

## 검증

- 컴파일 에러 0 — 토큰 추가만 (참조 없음).
- 색상 값은 디자인 토큰 정책(`Color(hex:)` 직접 호출은 토큰 정의 안에서만 허용) 을 따름.
- 라이트/다크 동일 값 입력은 명시적 의도 — 코드블록은 모드 불변 다크 정책.

## Acceptance Criteria

1. `Color+Tokens.swift` 에 위 6 토큰이 추가됨.
2. 각 토큰의 `light:` 와 `dark:` 인자 값이 동일 (코드 영역 모드 불변 정책).
3. 기존 다른 토큰들(brand/surface/border/text/semantic/cert) 무변경.
4. import 변경 없음 (`SwiftUI` + `UIKit` 그대로).

## 금지 사항

- 기존 `appSurface` / `appElevated` / `appBorder` 의 값을 변경하지 마라. **이유**: 다른 화면 전역에 영향. 코드블록 전용 토큰을 별도로 신설하는 것이 본 step 의 핵심.
- `Color(hex:)` 외 다른 헬퍼 사용 금지. **이유**: AGENTS.md 의 디자인 토큰 정책 준수.
- 다크 톤 값으로 임의의 다른 hex(#000000, #111 등) 를 쓰지 마라. **이유**: 웹 oneDark / zinc-900 톤과의 시각 매칭을 위해 `#1e1e1e / #2a2a2a / #3a3a3a / #e6e6e6 / #1f1f1f / #fcd34d` 6 개 값을 그대로 사용.

## Status 규칙

- 성공: `completed` + summary "Color+Tokens 에 appCodeSurface/Header/Border/Text + appCodeInline/InlineFG 6 토큰 추가, 라이트/다크 동일 다크값".
- 실패: 3회 재시도 후 `error`.
