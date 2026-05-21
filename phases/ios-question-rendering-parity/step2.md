# Step 2 — MarkdownTextView: 인라인 코드 스타일 + 단락 내 줄바꿈 보존

## 배경

`ios/Sqldpass/Features/Solve/Components/MarkdownTextView.swift` 는 SwiftUI 네이티브 `AttributedString(markdown:, options: .inlineOnlyPreservingWhitespace)` 로 본문/보기/해설/오답노트의 인라인 마크다운(굵게/기울임/링크/인라인 코드) 을 렌더한다.

두 가지 차이가 사용자에게 보이는 가독성 저하를 만든다.

(A) 백틱 인라인 코드(`` `inline code` ``) 는 `AttributedString(markdown:)` 가 `.inlinePresentationIntent == .code` 토큰까지는 만들지만 폰트/배경색을 자동 적용하지 않는다. 결과: iOS 에서 인라인 코드가 본문과 시각적으로 구분되지 않음. 웹 `frontend/src/components/QuestionContent.tsx:152-159` 는 `bg-zinc-800 px-1.5 py-0.5 font-mono text-[0.85em] text-amber-300` 로 명확히 분리.

(B) `blocks` 빌더의 `flushParagraph()` (라인 87-95) 가 `paragraphBuffer.joined(separator: " ")` 로 연속 라인을 공백으로 합쳐 `\n` 정보가 사라진다. 웹은 `frontend/src/components/QuestionContent.tsx:163` 의 `p` 컴포넌트에 `whitespace-pre-line` 클래스를 줘서 `\n` 을 보존. 결과: 백엔드가 `\n` 으로 줄바꿈해 저장한 보기/해설/본문이 iOS 에서는 한 줄로 합쳐짐.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Features/Solve/Components/MarkdownTextView.swift` | `inlineText(_:)` 에서 AttributedString runs 순회 → inline code 스타일 강제. `flushParagraph()` 의 join 구분자 `" "` → `"\n"`. |

## 구현 상세

### (A) inline code 스타일 강제

`inlineText(_:)` (현재 라인 53-62) 를 다음과 같이 보강:

```swift
private func inlineText(_ content: String) -> Text {
    guard var attributed = try? AttributedString(
        markdown: content,
        options: AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)
    ) else {
        return Text(content).font(AppType.body)
    }
    for run in attributed.runs {
        if run.inlinePresentationIntent?.contains(.code) == true {
            attributed[run.range].font = .system(.body, design: .monospaced)
            attributed[run.range].backgroundColor = Color.appCodeInline
            attributed[run.range].foregroundColor = Color.appCodeInlineFG
        }
    }
    return Text(attributed).font(AppType.body)
}
```

- `Color.appCodeInline` 과 `Color.appCodeInlineFG` 는 Step 4 에서 추가되는 토큰. 본 step 은 토큰만 참조하면 됨 (병렬 위탁이라 Step 4 결과를 기다리지 않고 작성. 토큰이 아직 없어 컴파일 실패하면 Step 5 단계에서 한꺼번에 빌드 검증 시 잡힘 — 본 step 의 코드 작성은 그대로 진행).
- `run.range` 는 `AttributedString.Index` 범위. SwiftUI/Foundation Swift 5.9 이상에서 위 attribute set 문법 지원.
- background color 가 SwiftUI `Text(AttributedString)` 에서 정확히 box 형태로 나오는지 OS 별 차이가 있을 수 있음 — iOS 17+ 에서는 정상 동작. 만약 iOS 16 호환이 필요하면 fallback 으로 `attributed[run.range].foregroundColor` 만 적용하고 배경은 생략하지만, AGENTS.md 에 "최소 iOS 17.0" 명시이므로 그대로 적용.

### (B) paragraph join 구분자 변경

`flushParagraph()` 현재 (라인 87-95):
```swift
func flushParagraph() {
    if !paragraphBuffer.isEmpty {
        let joined = paragraphBuffer.joined(separator: " ").trimmingCharacters(in: .whitespaces)
        if !joined.isEmpty {
            out.append(.paragraph(joined))
        }
        paragraphBuffer.removeAll()
    }
}
```

변경:
```swift
func flushParagraph() {
    if !paragraphBuffer.isEmpty {
        let joined = paragraphBuffer.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
        if !joined.isEmpty {
            out.append(.paragraph(joined))
        }
        paragraphBuffer.removeAll()
    }
}
```

핵심:
- `separator: " "` → `"\n"`
- `trimmingCharacters(in: .whitespaces)` → `.whitespacesAndNewlines` (앞뒤 줄바꿈도 trim)

동작 결과:
- 빈 라인 → 여전히 paragraph 분리 트리거 (변화 없음).
- 빈 라인 없이 이어진 라인 2 줄 → 한 paragraph 안에서 `\n` 보존. `AttributedString(markdown:, options: .inlineOnlyPreservingWhitespace)` 가 `\n` 을 보존하므로 SwiftUI Text 는 자연스럽게 줄바꿈.

## 검증

본 step 의 검증은 Step 6 시뮬레이터 스크린샷에서 다음 4 케이스로 확인:
1. 본문 안 `` `member_id` `` 인라인 코드가 다크 배경 + 앰버 톤 + 모노로 표시.
2. 해설에 `\n` 으로 줄바꿈된 2~3 줄 텍스트가 iOS 에서도 줄바꿈 유지.
3. 보기 안에 `\n` 으로 여러 라인이 들어간 정처기 보기(코드 조각 포함) 가 줄바꿈 유지.
4. 빈 라인으로 분리된 두 단락이 여전히 단락 간 여백 유지 (회귀 없음).

본 step 자체로는 Windows 셸에서 컴파일 검증 불가 — 코드 작성 후 즉시 완료 처리.

## Acceptance Criteria

1. `inlineText(_:)` 가 inline code 토큰에 `font(.system(.body, design: .monospaced))` + `backgroundColor(.appCodeInline)` + `foregroundColor(.appCodeInlineFG)` 적용.
2. `flushParagraph` join 구분자 `\n` 으로 변경.
3. 기존 heading / bullet / ordered / blank 분기는 무변경.
4. 컴파일 에러 0 (단, Step 4 미완료 시 토큰 참조 에러 발생 가능 — Step 5 단계에서 통합 빌드 검증).
5. paragraph 내 줄바꿈 보존 + 빈 라인 단락 분리 회귀 없음.

## 금지 사항

- 헤딩/리스트/순서 리스트 분기 로직(`parseHeading`/`parseBullet`/`parseOrdered`) 을 변경하지 마라. **이유**: 본 step 은 인라인 + 줄바꿈만 다룸. 구조 파싱 변경은 별도 검토 필요.
- `Color(hex: "...")` 직접 사용 금지. **이유**: 디자인 토큰 정책. `Color.appCodeInline` / `Color.appCodeInlineFG` 토큰만 참조.
- `Text` 가 아닌 `TextRenderer` 또는 `UIViewRepresentable(UITextView)` 같은 대안 도입 금지. **이유**: SwiftUI 단일 스택 정책.
- `AttributedString` 의 `interpretedSyntax` 옵션을 `.full` 로 바꾸지 마라. **이유**: 헤딩/리스트가 SwiftUI native AttributedString 에서 한 줄로 무너짐 — 기존 `inlineOnlyPreservingWhitespace` 로 분리 처리가 안정.

## Status 규칙

- 성공: `completed` + summary "MarkdownTextView inlineText AttributedString runs 순회로 inline code 스타일 강제 + flushParagraph join ' '→'\\n' 으로 paragraph 내 \\n 보존".
- 실패: 3회 재시도 후 `error`.
