# Step 2 — iOS QuestionContent Rich Rendering (Markdown · SVG · Table · Code)

## 배경

iOS `ios/Sqldpass/Features/Solve/Components/QuestionBody.swift` 가 현재 `Text(question.content)` 한 줄. 백엔드가 보내는 문제 본문은 Markdown + Inline `<svg>` + HTML/Markdown 표 + 코드블록이 섞여 오는데, iOS 는 **표가 깨지고 SVG 가 텍스트 그대로 보이고 코드블록 하이라이팅이 없다**. Android 는 이미 `text/MarkdownSegments.kt` + `text/EnsureCodeFences.kt` + `text/SqldpassMarkwon.kt` 로 같은 문제를 해결해 둠.

본 step 은 Android 의 분리 렌더 흐름을 iOS 로 포팅해 양 플랫폼 본문 가독성을 동등하게 맞춘다. step 4(iOS SoloSolveView) 가 이 컴포넌트를 사용한다.

## 작업 디렉터리

```bash
cd ios
```

macOS 셸에서만. Windows 에서 빌드 검증을 시도하지 말 것.

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ios/project.yml` | SVGKit SPM 의존성 추가 (`https://github.com/SVGKit/SVGKit`, 트랙 `branch: main` 또는 최신 안정 태그) |
| `ios/Sqldpass/Models/QuestionSegment.swift` | 신규 — `enum QuestionSegment { case markdown(String), codeBlock(language: String?, code: String), inlineSVG(String), image(src: String, alt: String?), table(rows: [[String]]) }` |
| `ios/Sqldpass/Core/Content/EnsureCodeFences.swift` | 신규 — Android `EnsureCodeFences.kt` 동치. HTML `<pre><code>` → 펜스 markdown, HTML `<table>` → markdown table, inline `<code>` → 백틱, HTML 엔티티 디코드 |
| `ios/Sqldpass/Core/Content/ParseQuestionContent.swift` | 신규 — Android `splitMarkdownSegments` 동치. 정규식 기반 fenced code / inline SVG / `<img>` / markdown table 분리 → `[QuestionSegment]` |
| `ios/Sqldpass/Features/Solve/Components/QuestionContentView.swift` | 신규 — segments 순차 렌더 SwiftUI 뷰 |
| `ios/Sqldpass/Features/Solve/Components/MarkdownTextView.swift` | 신규 — `AttributedString(markdown:)` 기반 markdown 렌더. heading/list/strong/em/inline-code 처리 |
| `ios/Sqldpass/Features/Solve/Components/CodeBlockView.swift` | 신규 — mono font + 가로 스크롤 + 언어 라벨 칩. SwiftUI native(외부 하이라이터 없이 단색 mono 로 시작 — Android CodeBlockCard 와 동일 수준) |
| `ios/Sqldpass/Features/Solve/Components/InlineSVGView.swift` | 신규 — SVGKit `SVGKFastImageView` UIViewRepresentable wrapper. `intrinsicContentSize` 기준 자동 sizing, 최대 폭 = 본문 폭 |
| `ios/Sqldpass/Features/Solve/Components/MarkdownTableView.swift` | 신규 — `ScrollView(.horizontal)` + `Grid` 또는 `LazyVGrid`. mono font 셀, divider 1pt appBorder |
| `ios/Sqldpass/Features/Solve/Components/QuestionBody.swift` | **교체** — 기존 단순 `Text` 구조에서 `QuestionContentView(text: question.content)` 호출로. 과목 라벨/문제 번호 헤더는 유지 |

## 핵심 알고리즘 (Swift 포팅 가이드)

`ParseQuestionContent.swift` 의 처리 순서 (Android 와 동등):

```swift
// 1) 정규식 매치 수집
let fenced = #/```([a-zA-Z0-9_+\-]*)\s*\n([\s\S]*?)```/#
let svg    = #/<svg\b[^>]*>[\s\S]*?<\/svg>/#.ignoresCase()
let img    = #/<img\b([^>]*?)\/?>/#.ignoresCase()
let table  = #/^\|.+\|\s*\n\|[\s\-:|]+\|\s*\n(?:\|.+\|\s*\n?)+/#.anchorsMatchLineEndings()

// 2) 모든 hit 의 range 수집 → 시작 인덱스 정렬
// 3) 코드블록 내부의 svg/img 는 무시 (Android Hit insideCode 동치)
// 4) 분리하지 않은 구간은 .markdown 으로
// 5) hits.isEmpty → 전체를 .markdown 한 덩어리로 반환
```

`EnsureCodeFences.swift` 의 처리 순서:

```swift
// 1) <pre><code class="language-xx">...</code></pre> → ```xx\n...\n```
// 2) 남은 인라인 <code>...</code> → `...`
// 3) <table>...</table> → markdown table (convertHtmlTableToMarkdown 동치)
//    - <tr>/<td>/<th> 만 처리. colspan/rowspan/nested 미지원(원본 보존)
//    - <br> → " / ", 잔여 inline 태그 제거, | → \|
// 4) HTML 엔티티 디코드 (&lt; → < 등 7개)
```

Swift Regex literal 은 iOS 16+ 인데 본 앱은 iOS 17 minimum 이므로 사용 가능. NSRegularExpression 으로 폴백할 필요 없음.

## SwiftUI 컴포넌트 구조

### `QuestionContentView`

```swift
struct QuestionContentView: View {
    let text: String

    private var segments: [QuestionSegment] {
        ParseQuestionContent.parse(EnsureCodeFences.normalize(text))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            ForEach(Array(segments.enumerated()), id: \.offset) { _, seg in
                switch seg {
                case .markdown(let md):       MarkdownTextView(text: md)
                case .codeBlock(let lang, let code): CodeBlockView(language: lang, code: code)
                case .inlineSVG(let xml):     InlineSVGView(svgXML: xml)
                case .image(let src, let alt): AsyncImage(url: URL(string: src)) { ... }
                case .table(let rows):        MarkdownTableView(rows: rows)
                }
            }
        }
    }
}
```

### `InlineSVGView` (SVGKit wrapper)

```swift
import SwiftUI
import SVGKit

struct InlineSVGView: UIViewRepresentable {
    let svgXML: String

    func makeUIView(context: Context) -> SVGKFastImageView {
        let image = SVGKImage(data: svgXML.data(using: .utf8))
        let view = SVGKFastImageView(svgkImage: image ?? SVGKImage())
        view.contentMode = .scaleAspectFit
        view.setContentHuggingPriority(.required, for: .vertical)
        return view
    }

    func updateUIView(_ uiView: SVGKFastImageView, context: Context) {
        if let data = svgXML.data(using: .utf8),
           let image = SVGKImage(data: data) {
            uiView.image = image
        }
    }
}
```

### `MarkdownTableView`

```swift
struct MarkdownTableView: View {
    let rows: [[String]]   // rows[0] = header
    private var columnCount: Int { rows.first?.count ?? 0 }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.offset) { idx, row in
                    HStack(spacing: 0) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                            Text(cell)
                                .font(idx == 0 ? AppType.captionEmph : AppType.bodyMono)
                                .padding(.horizontal, Spacing.md)
                                .padding(.vertical, Spacing.sm)
                                .frame(minWidth: 80, alignment: .leading)
                        }
                    }
                    .background(idx == 0 ? Color.appPageElevated : Color.appSurface)
                    Divider().background(Color.appBorder)
                }
            }
            .overlay(
                RoundedRectangle(cornerRadius: Radius.sm)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
        }
    }
}
```

## SVGKit SPM 추가 (project.yml)

```yaml
packages:
  SVGKit:
    url: https://github.com/SVGKit/SVGKit
    branch: main   # 또는 최신 태그(3.x)
```

각 target dependencies 에 `package: SVGKit`, `product: SVGKit` 추가. project.yml 변경 후:

```bash
~/bin/xcodegen generate
sed -i '' 's/objectVersion = 77;/objectVersion = 56;/' Sqldpass.xcodeproj/project.pbxproj
```

## Acceptance Criteria

1. `xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass -destination 'platform=iOS Simulator,name=iPhone 15 Pro' -configuration Debug build` → `** BUILD SUCCEEDED **`
2. SVGKit 가 SPM 으로 추가되어 있고 import 가능.
3. `QuestionSegment.swift`, `EnsureCodeFences.swift`, `ParseQuestionContent.swift`, 5개 컴포넌트(`QuestionContentView`, `MarkdownTextView`, `CodeBlockView`, `InlineSVGView`, `MarkdownTableView`) 가 신규로 추가.
4. `QuestionBody.swift` 가 `QuestionContentView` 를 사용하도록 교체. 과목 라벨/문제 번호 헤더는 유지.
5. 시뮬레이터 스크린샷 1장 — 본문 SVG 포함 문제(테스트 데이터: `MockExamPreview.svgSample` 같은 픽스처 또는 실서버 데이터)에서 SVG/표/코드블록이 모두 렌더되는지 확인. `/tmp/sqldpass-step2.png` 저장.

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대 출력: `** BUILD SUCCEEDED **`

스크린샷:

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
xcrun simctl io booted screenshot /tmp/sqldpass-step2.png
```

**Windows 환경에서 본 step 실행 불가** — 코드/project.yml 변경만 본 step 에서 수행하고, 빌드 검증은 사용자가 macOS 에서 별도 실행 후 결과를 step summary 에 기록.

## 금지 사항

- `WKWebView` 로 HTML 통째 렌더 도입 금지. 이유: 메모리/성능 부담 + 셀 단위 webview 가 모의고사 50문항에서 폭발. 본 phase 는 네이티브 분리 렌더.
- `Down`, `swift-markdown-ui` 같은 외부 markdown 라이브러리 추가 금지. 이유: 본 step 은 SVGKit 만 추가. 마크다운은 iOS 17 native `AttributedString(markdown:)` 로 충분.
- 코드블록에 신택스 하이라이터 라이브러리 도입 금지. 이유: Android CodeBlockCard 와 동일 수준(mono font 단색)으로 시작. 하이라이팅은 별도 phase 에서 검토.
- HTML `<table>` 변환 로직에 colspan/rowspan 지원을 추가하지 마라. 이유: Android 도 동일하게 미지원이며, 본 step 은 양 플랫폼 동등 수준 유지. 보강은 별도 phase.
- 기존 `QuestionBody.swift` 파일을 삭제하지 마라. 이유: SolveView/MockExamDetailView 등이 import. 본 step 은 내용 교체만(파일 경로 유지).
- iOS 토큰 파일을 수정하지 마라. 이유: step 1 의 정합 검증 결과를 본 step 이 신뢰하고 사용. 토큰 변경은 별도 step.

## Status 규칙

- 성공: index.json step 2 `completed`, summary 에 빌드 결과 + 스크린샷 경로.
- 실패: 3회 시도 후 `error` + `error_message`. SVGKit SPM 해상도 실패는 자주 발생하므로 대체 fork 후보(예: `https://github.com/SVGKit/SVGKit` 의 특정 태그 고정) 시도.
- Windows 에서 코드만 작성한 경우: `blocked` + `blocked_reason: "iOS 빌드/스크린샷 검증을 위해 macOS 환경 필요"`.
