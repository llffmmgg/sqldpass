# Step 5 — SimpleSyntaxHighlighter 신규 + CodeBlockView 다크톤 적용

## 배경

웹 `frontend/src/components/QuestionCodeBlock.tsx:64-82` 는 `react-syntax-highlighter` + Prism + oneDark 로 SQL/Java/Python/C/JS/TS/Bash 키워드·문자열·주석·숫자 색을 분리한다. iOS `CodeBlockView.swift` 는 현재 `Text(code).foregroundStyle(.appTextPrimary)` 단색이라 긴 SELECT 문이 한 덩어리로 보인다.

사용자 결정: 외부 SPM 없이 순수 Swift `NSRegularExpression` 기반 가벼운 하이라이터 추가.

Step 4 에서 추가된 다크톤 토큰을 본 step 에서 CodeBlockView 에 적용한다 (Step 4 의존).

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Core/Content/SimpleSyntaxHighlighter.swift` | 신규 — 순수 Swift 키워드/문자열/주석/숫자 하이라이터. |
| `ios/Sqldpass/Features/Solve/Components/CodeBlockView.swift` | 본문 배경/헤더/보더/텍스트를 `appCode*` 토큰으로 교체 + `Text(SimpleSyntaxHighlighter.highlight(code, language:))` 로 교체 + 헤더-본문 1px 구분선 추가. |
| `ios/Sqldpass/project.yml` | 새 파일이 sources 패턴으로 자동 포함되므로 변경 불필요 (`sources: [Sqldpass]` 한 줄). 확인만. |

## 구현 상세

### 신규: `SimpleSyntaxHighlighter.swift`

위치: `ios/Sqldpass/Core/Content/SimpleSyntaxHighlighter.swift` (기존 `EnsureCodeFences.swift`, `ParseQuestionContent.swift`, `ParseQuestion.swift` 와 같은 폴더).

API:
```swift
import Foundation
import SwiftUI

enum SimpleSyntaxHighlighter {
    static func highlight(_ code: String, language: String?) -> AttributedString
}
```

구현 윤곽 (마스킹 기법으로 패스 충돌 회피):

1. 언어 정규화 (lowercased):
   - `sql` → SQL
   - `python` | `py` → Python
   - `java` → Java
   - `c` | `cpp` | `c++` → C/C++
   - `javascript` | `js` | `typescript` | `ts` → JS/TS
   - `bash` | `sh` | `shell` → Shell
   - 그 외 / nil → plain (폰트만 적용, 색 없음).

2. 기본 attribute 컨테이너 만들고 전체에 `.font(.system(.footnote, design: .monospaced))` + `.foregroundColor(.appCodeText)` 적용.

3. 토큰 색 (모두 디자인 토큰 사용):
   - 주석 → `Color.appTextMuted` (디자인 토큰; 코드 본문 다크톤 위에서 충분히 muted 보임)
   - 문자열 → `Color.appCodeInlineFG` (amber)
   - 숫자 → `Color.semanticInfo` (blue)
   - 키워드 → `Color.brandPrimary` (emerald)

4. 매칭 순서 — 충돌 회피 방식:
   - **방법 A (권장)**: `NSRegularExpression` 으로 모든 패턴을 한 번에 찾되, 매치 결과 array 를 시작 위치 기준 정렬 후 **앞 매치가 이미 차지한 range 와 겹치는 매치는 skip**. 이러면 주석(`-- SELECT ...`) 안의 `SELECT` 가 키워드로 잘못 칠해지지 않음.
   - 매치 순서: 주석 > 문자열 > 숫자 > 키워드. 같은 시작 위치에서는 더 긴 매치 우선.

5. 언어별 패턴 (NSRegularExpression):
   - 주석 — SQL: `--[^\n]*` + `/\*[\s\S]*?\*/`; C/Java/JS/TS: `//[^\n]*` + `/\*[\s\S]*?\*/`; Python/Bash: `#[^\n]*`.
   - 문자열 — 공통: `'(?:\\.|[^'\\])*'` + `"(?:\\.|[^"\\])*"`.
   - 숫자 — 공통: `\b\d+(?:\.\d+)?\b`.
   - 키워드 — 언어별 단어 boundary `\b(...)\b`:
     - SQL (case-insensitive): `SELECT|FROM|WHERE|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|GROUP|ORDER|BY|HAVING|JOIN|INNER|LEFT|RIGHT|FULL|CROSS|OUTER|ON|UNION|ALL|INTERSECT|MINUS|EXCEPT|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|ALTER|DROP|TABLE|INDEX|VIEW|TRIGGER|PROCEDURE|FUNCTION|AS|DISTINCT|CASE|WHEN|THEN|ELSE|END|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT|BEGIN|TRANSACTION|LIMIT|OFFSET|FETCH|RETURNING|PARTITION|WINDOW|RANGE|ROWS|ASC|DESC|EXISTS|ANY|SOME|COUNT|SUM|AVG|MIN|MAX`
     - Java: `public|private|protected|static|final|abstract|class|interface|extends|implements|new|this|super|return|void|int|long|short|byte|char|boolean|float|double|String|if|else|switch|case|default|for|while|do|break|continue|try|catch|finally|throw|throws|import|package|null|true|false`
     - Python: `def|class|return|if|elif|else|for|while|in|not|and|or|is|None|True|False|import|from|as|pass|break|continue|try|except|finally|raise|with|lambda|yield|global|nonlocal|self`
     - C/C++: `int|long|short|char|float|double|void|unsigned|signed|struct|union|enum|typedef|const|static|extern|return|if|else|switch|case|default|for|while|do|break|continue|sizeof|include|define|true|false|NULL`
     - JS/TS: `var|let|const|function|return|if|else|switch|case|default|for|while|do|break|continue|try|catch|finally|throw|new|this|class|extends|export|import|from|as|of|in|typeof|instanceof|null|undefined|true|false|async|await|yield`
     - Shell: `if|then|else|elif|fi|for|while|do|done|case|esac|in|function|return|export|local|readonly|echo|cd|exit`

6. SQL 만 case-insensitive 매칭, 나머지는 case-sensitive.

7. 매치 결과 적용:
   ```swift
   var attributed = AttributedString(code)
   attributed.font = .system(.footnote, design: .monospaced)
   attributed.foregroundColor = .appCodeText

   let occupied: NSMutableIndexSet = NSMutableIndexSet()
   for (regex, color) in patterns {  // 주석 → 문자열 → 숫자 → 키워드 순
       let ns = code as NSString
       for m in regex.matches(in: code, range: NSRange(location: 0, length: ns.length)) {
           let r = m.range
           if occupied.intersects(in: r) { continue }
           occupied.add(in: r)
           if let range = Range(r, in: attributed) {  // 또는 String→AttributedString index 변환
               attributed[range].foregroundColor = color
           }
       }
   }
   ```
   - `NSRange` → `AttributedString.Index` 변환은 `String` 의 `Range<String.Index>` 를 거쳐야 함. 헬퍼 메서드 작성 권장.

8. 알 수 없는 언어 / nil → 마스킹/패턴 매칭 skip 하고 폰트+기본색만 적용한 `AttributedString` 반환.

### 수정: `CodeBlockView.swift`

전체 교체본:

```swift
import SwiftUI

/// 코드블록 — 다크톤 surface + 가벼운 신택스 하이라이팅 + 가로 스크롤.
///
/// 라이트/다크 모드 무관 다크톤 고정 (웹 zinc-900 / oneDark 정책과 매칭).
struct CodeBlockView: View {
    let language: String?
    let code: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let lang = language, !lang.isEmpty {
                Text(lang.uppercased())
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appCodeText.opacity(0.7))
                    .padding(.horizontal, Spacing.md)
                    .padding(.vertical, Spacing.xs)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.appCodeHeader)
                Rectangle()
                    .fill(Color.appCodeBorder)
                    .frame(height: 1)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                Text(SimpleSyntaxHighlighter.highlight(code, language: language))
                    .lineLimit(nil)
                    .fixedSize(horizontal: true, vertical: false)
                    .padding(Spacing.md)
            }
            .background(Color.appCodeSurface)
        }
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appCodeBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }
}
```

핵심 변경점:
- 헤더 배경 `appElevated` → `appCodeHeader`.
- 헤더 텍스트 색 `appTextSubtle` → `appCodeText.opacity(0.7)`.
- 본문 배경 `appSurface` → `appCodeSurface`.
- 보더 `appBorder` → `appCodeBorder`.
- 헤더-본문 사이 1px 구분선 추가 (헤더 존재 시만).
- `Text(code).font(.system(.footnote, design: .monospaced)).foregroundStyle(.appTextPrimary)` →
  `Text(SimpleSyntaxHighlighter.highlight(code, language: language))` (폰트/색은 AttributedString 안에 들어 있음).
- 가로 스크롤 / `fixedSize` / `lineLimit(nil)` / `padding(Spacing.md)` 유지.

### `project.yml` 확인

`ios/project.yml` 의 sources 설정이 `Sqldpass` 폴더 전체를 글롭 패턴으로 포함하면 별도 등록 불필요. xcodegen regenerate 가 필요한지만 확인.

대부분의 xcodegen 프로젝트는 `sources: [path: Sqldpass]` 한 줄로 폴더 전체 자동 포함이므로 신규 .swift 파일 추가만으로 빌드 인식. 안 그렇다면 사용자가 Step 6 에서 `~/bin/xcodegen generate` 1 회 실행 필요 — Step 6 안내에 포함.

## 검증

본 step 의 정확성은 Step 6 시뮬레이터 스크린샷에서 확인:
- SQL 코드블록에서 `SELECT/FROM/WHERE/GROUP BY/HAVING` 이 emerald, `'SQLD'` 가 amber, 숫자가 blue, `--` 주석이 muted 로 표시.
- 라이트 모드에서 코드블록 본문이 페이지보다 명확히 어두운 다크톤.
- 헤더(언어 칩) 배경과 본문 배경이 미세하게 다른 톤 + 1px 보더 구분선.
- 가로 스크롤 유지(80 자 넘는 한 줄 SQL 가로 스와이프).

Windows 셸에서는 본 step 자체 컴파일 검증 불가.

## Acceptance Criteria

1. `SimpleSyntaxHighlighter.swift` 신규 파일 추가, `static func highlight(_:language:) -> AttributedString` 시그니처 노출.
2. SQL/Java/Python/C/JS/TS/Shell 6 언어 토큰 분류 지원 (디자인 토큰 색만 사용).
3. 알 수 없는 언어 / nil 입력은 폰트+기본색만 적용해 그대로 반환 (fallback 동작).
4. `CodeBlockView.swift` 가 `appCode*` 5 토큰을 사용 + `SimpleSyntaxHighlighter.highlight` 호출 + 헤더 구분선 추가.
5. 가로 스크롤 / `fixedSize(horizontal: true, vertical: false)` / `ScrollView(.horizontal)` 보존 (회귀 없음).
6. 컴파일 에러 0 (단, xcodegen regenerate 필요 시 사용자가 Step 6 에서 1 회 실행).
7. Color 직접 hex 호출 없음 — 모두 디자인 토큰.

## 금지 사항

- 외부 SPM 라이브러리(Splash, Sourceful, Down, MarkdownUI 등) 추가하지 마라. **이유**: 순수 Swift 결정 + AGENTS.md 의존 최소화 정책.
- `Color(hex:)` 직접 호출 금지. **이유**: 디자인 토큰만 사용.
- `print()` 디버그 로그 남기지 마라. **이유**: AGENTS.md 금지사항.
- WKWebView 또는 다른 하이브리드 렌더링 도입 금지. **이유**: AGENTS.md 명시 금지.
- `MarkdownTextView` / `MarkdownTableView` / `ParseQuestionContent` / `EnsureCodeFences` 를 본 step 에서 건드리지 마라. **이유**: 각각 다른 step 책임.
- 신택스 하이라이팅 정확도에 과도한 노력 투입 금지(완벽한 토크나이저는 본 phase 범위 밖). false-positive 가 있더라도 단색 fallback 보다 가독성이 개선되면 OK.

## Status 규칙

- 성공: `completed` + summary "SimpleSyntaxHighlighter 신규 (SQL/Java/Python/C/JS/TS/Shell, NSRegularExpression 마스킹 기법) + CodeBlockView 다크톤 토큰 + 하이라이터 적용".
- 실패: 3회 재시도 후 `error`.
