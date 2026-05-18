import Foundation

/// 문제 본문(`content`) 을 렌더 단위로 분리한 결과.
///
/// 백엔드 응답은 Markdown / Inline SVG / GFM Table / 코드블록 / `<img>` 가 섞여 온다.
/// `EnsureCodeFences.normalize` 로 HTML 잔재를 markdown 형식으로 정규화한 뒤,
/// `ParseQuestionContent.parse` 가 본 enum 의 배열로 분리한다.
///
/// Android 의 `MarkdownSegment` (mobile/.../text/MarkdownSegments.kt) 와 동치.
/// 단 iOS 는 markdown table 을 SwiftUI native 가 렌더 못 하므로 `.table` 케이스를 추가로 분리.
enum QuestionSegment: Equatable {
    case markdown(String)
    case codeBlock(language: String?, code: String)
    case inlineSVG(String)
    case image(src: String, alt: String?)
    case table(rows: [[String]])
}
