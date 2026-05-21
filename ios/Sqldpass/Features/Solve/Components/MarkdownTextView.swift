import SwiftUI

/// Markdown 텍스트를 SwiftUI native `AttributedString(markdown:)` 으로 렌더.
///
/// 지원 범위:
///  - `**bold**`, `*italic*`, `` `inline code` ``, 링크
///  - 헤딩/리스트는 SwiftUI native AttributedString 이 인라인 스타일만 처리하므로
///    문단 단위로 분리 + 헤딩은 굵게/큰 폰트로 별도 처리
///
/// GFM Table / fenced code 는 본 뷰가 받지 않는다(상위 `QuestionContentView` 에서 별도 세그먼트로 분리).
struct MarkdownTextView: View {
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                blockView(block)
            }
        }
    }

    @ViewBuilder
    private func blockView(_ block: Block) -> some View {
        switch block {
        case .heading(let level, let content):
            inlineText(content)
                .font(headingFont(level))
                .foregroundStyle(Color.appTextPrimary)
                .padding(.top, level == 1 ? Spacing.sm : Spacing.xs)
        case .bulletItem(let content):
            HStack(alignment: .firstTextBaseline, spacing: Spacing.sm) {
                Text("•")
                    .foregroundStyle(Color.appTextMuted)
                inlineText(content)
                    .foregroundStyle(Color.appTextPrimary)
            }
        case .orderedItem(let n, let content):
            HStack(alignment: .firstTextBaseline, spacing: Spacing.sm) {
                Text("\(n).")
                    .monospacedDigit()
                    .foregroundStyle(Color.appTextMuted)
                inlineText(content)
                    .foregroundStyle(Color.appTextPrimary)
            }
        case .paragraph(let content):
            inlineText(content)
                .foregroundStyle(Color.appTextPrimary)
        case .blank:
            Color.clear.frame(height: Spacing.xs)
        }
    }

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

    private func headingFont(_ level: Int) -> Font {
        switch level {
        case 1: return AppType.heading
        case 2: return AppType.subheading
        default: return AppType.bodyEmph
        }
    }

    // MARK: - Block parsing

    private enum Block {
        case heading(level: Int, content: String)
        case bulletItem(String)
        case orderedItem(number: Int, content: String)
        case paragraph(String)
        case blank
    }

    private var blocks: [Block] {
        let lines = text.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        var out: [Block] = []
        var paragraphBuffer: [String] = []

        func flushParagraph() {
            if !paragraphBuffer.isEmpty {
                let joined = paragraphBuffer.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
                if !joined.isEmpty {
                    out.append(.paragraph(joined))
                }
                paragraphBuffer.removeAll()
            }
        }

        for rawLine in lines {
            let line = rawLine.trimmingCharacters(in: CharacterSet(charactersIn: " \t"))
            if line.isEmpty {
                flushParagraph()
                continue
            }
            if let heading = parseHeading(line) {
                flushParagraph()
                out.append(heading)
                continue
            }
            if let bullet = parseBullet(line) {
                flushParagraph()
                out.append(bullet)
                continue
            }
            if let ordered = parseOrdered(line) {
                flushParagraph()
                out.append(ordered)
                continue
            }
            paragraphBuffer.append(line)
        }
        flushParagraph()
        return out
    }

    private func parseHeading(_ line: String) -> Block? {
        var level = 0
        var idx = line.startIndex
        while idx < line.endIndex, line[idx] == "#", level < 6 {
            level += 1
            idx = line.index(after: idx)
        }
        guard level > 0, idx < line.endIndex, line[idx] == " " else { return nil }
        let content = line[line.index(after: idx)...].trimmingCharacters(in: .whitespaces)
        return .heading(level: level, content: String(content))
    }

    private func parseBullet(_ line: String) -> Block? {
        guard line.hasPrefix("- ") || line.hasPrefix("* ") else { return nil }
        let content = String(line.dropFirst(2)).trimmingCharacters(in: .whitespaces)
        return .bulletItem(content)
    }

    private static let orderedRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"^(\d+)\.\s+(.+)$"#, options: [])
    }()

    private func parseOrdered(_ line: String) -> Block? {
        let ns = line as NSString
        guard let m = Self.orderedRegex.firstMatch(
            in: line,
            options: [],
            range: NSRange(location: 0, length: ns.length)
        ) else { return nil }
        let n = Int(ns.substring(with: m.range(at: 1))) ?? 0
        let content = ns.substring(with: m.range(at: 2))
        return .orderedItem(number: n, content: content)
    }
}
