import SwiftUI

/// 문제 본문(Markdown / Inline SVG / Code Block / Table / Image) 을 분리 렌더.
///
/// Android `MarkdownContent` (mobile/.../ui/runner/QuestionRunnerScreen.kt) 와 동치.
/// `EnsureCodeFences.normalize` → `ParseQuestionContent.parse` 로 세그먼트화한 뒤
/// 각 세그먼트 전용 뷰로 렌더.
struct QuestionContentView: View {
    let text: String

    private var segments: [QuestionSegment] {
        ParseQuestionContent.parse(EnsureCodeFences.normalize(text))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            ForEach(Array(segments.enumerated()), id: \.offset) { _, seg in
                segmentView(seg)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func segmentView(_ seg: QuestionSegment) -> some View {
        switch seg {
        case .markdown(let md):
            MarkdownTextView(text: md)
        case .codeBlock(let lang, let code):
            CodeBlockView(language: lang, code: code)
        case .inlineSVG(let xml):
            InlineSVGView(svgXML: xml)
        case .image(let src, let alt):
            if let url = URL(string: src) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        Color.appSurfaceHover
                            .frame(height: 80)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                    case .failure:
                        Text(alt ?? "이미지를 불러올 수 없습니다")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextSubtle)
                    @unknown default:
                        EmptyView()
                    }
                }
            }
        case .table(let rows):
            MarkdownTableView(rows: rows)
        }
    }
}
