import Foundation

/// 백엔드 응답에 섞여 오는 HTML 코드/표를 markdown 으로 정규화.
///
/// Android `mobile/.../text/EnsureCodeFences.kt` 와 동치:
///  1. `<pre><code class="language-xx">...</code></pre>` → ```xx\n...\n```
///  2. 남은 인라인 `<code>...</code>` → `` `...` ``
///  3. `<table>...</table>` → markdown table (단순 구조만, colspan/rowspan/nested 미지원)
///  4. HTML 엔티티 디코드 (`&lt;`, `&gt;`, `&quot;`, `&#39;`, `&apos;`, `&nbsp;`, `&amp;`)
///
/// `ParseQuestionContent.parse` 호출 전 1회 거친다.
enum EnsureCodeFences {

    static func normalize(_ input: String) -> String {
        var text = input
        text = replaceHtmlCodeBlocks(text)
        text = replaceHtmlInlineCode(text)
        text = replaceHtmlTables(text)
        return text
    }

    // MARK: - <pre><code> → fenced markdown

    private static let codeBlockRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"<pre[^>]*>\s*<code(?:\s+class="language-(\w+)")?[^>]*>([\s\S]*?)</code>\s*</pre>"#,
            options: [.caseInsensitive]
        )
    }()

    private static func replaceHtmlCodeBlocks(_ text: String) -> String {
        let ns = text as NSString
        let matches = codeBlockRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        guard !matches.isEmpty else { return text }
        var result = text
        for match in matches.reversed() {
            let langRange = match.range(at: 1)
            let codeRange = match.range(at: 2)
            let lang = (langRange.location != NSNotFound) ? ns.substring(with: langRange) : ""
            let raw = (codeRange.location != NSNotFound) ? ns.substring(with: codeRange) : ""
            let decoded = decodeHtmlEntities(raw)
                .trimmingCharacters(in: CharacterSet(charactersIn: "\n "))
            let replacement = lang.isEmpty
                ? "```\n\(decoded)\n```"
                : "```\(lang)\n\(decoded)\n```"
            if let range = Range(match.range, in: result) {
                result.replaceSubrange(range, with: replacement)
            }
        }
        return result
    }

    // MARK: - 인라인 <code>...</code> → `...`

    private static let inlineCodeRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<code[^>]*>([^<]+)</code>"#, options: [.caseInsensitive])
    }()

    private static func replaceHtmlInlineCode(_ text: String) -> String {
        let ns = text as NSString
        let matches = inlineCodeRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        guard !matches.isEmpty else { return text }
        var result = text
        for match in matches.reversed() {
            let inner = ns.substring(with: match.range(at: 1))
            let decoded = decodeHtmlEntities(inner)
            if let range = Range(match.range, in: result) {
                result.replaceSubrange(range, with: "`\(decoded)`")
            }
        }
        return result
    }

    // MARK: - <table> → markdown table

    private static let tableRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<table\b[^>]*>([\s\S]*?)</table>"#, options: [.caseInsensitive])
    }()
    private static let trRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<tr\b[^>]*>([\s\S]*?)</tr>"#, options: [.caseInsensitive])
    }()
    private static let cellRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<(th|td)\b[^>]*>([\s\S]*?)</\1>"#, options: [.caseInsensitive])
    }()
    private static let brRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<br\s*/?>"#, options: [.caseInsensitive])
    }()
    private static let inlineTagRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<[^>]+>"#)
    }()
    private static let newlineRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"[\r\n]+"#)
    }()

    private static func replaceHtmlTables(_ text: String) -> String {
        let ns = text as NSString
        let matches = tableRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        guard !matches.isEmpty else { return text }
        var result = text
        for match in matches.reversed() {
            let innerHtml = ns.substring(with: match.range(at: 1))
            guard let markdown = convertHtmlTableToMarkdown(innerHtml) else { continue }
            if let range = Range(match.range, in: result) {
                result.replaceSubrange(range, with: markdown)
            }
        }
        return result
    }

    private static func convertHtmlTableToMarkdown(_ innerHtml: String) -> String? {
        let ns = innerHtml as NSString
        let trMatches = trRegex.matches(in: innerHtml, options: [], range: NSRange(location: 0, length: ns.length))
        var rows: [[String]] = []
        for tr in trMatches {
            let trInner = ns.substring(with: tr.range(at: 1))
            let trNs = trInner as NSString
            let cellMatches = cellRegex.matches(in: trInner, options: [], range: NSRange(location: 0, length: trNs.length))
            var row: [String] = []
            for cell in cellMatches {
                let raw = trNs.substring(with: cell.range(at: 2))
                var cleaned = decodeHtmlEntities(raw)
                cleaned = brRegex.stringByReplacingMatches(
                    in: cleaned,
                    options: [],
                    range: NSRange(location: 0, length: (cleaned as NSString).length),
                    withTemplate: " / "
                )
                cleaned = inlineTagRegex.stringByReplacingMatches(
                    in: cleaned,
                    options: [],
                    range: NSRange(location: 0, length: (cleaned as NSString).length),
                    withTemplate: ""
                )
                cleaned = newlineRegex.stringByReplacingMatches(
                    in: cleaned,
                    options: [],
                    range: NSRange(location: 0, length: (cleaned as NSString).length),
                    withTemplate: " "
                )
                cleaned = cleaned.replacingOccurrences(of: "|", with: "\\|")
                let trimmed = cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
                row.append(trimmed.isEmpty ? " " : trimmed)
            }
            rows.append(row)
        }
        guard !rows.isEmpty, let width = rows.map(\.count).max(), width > 0 else { return nil }
        let normalized = rows.map { row -> [String] in
            row.count == width ? row : row + Array(repeating: " ", count: width - row.count)
        }
        var sb = "\n"
        let header = normalized[0]
        sb += "| " + header.joined(separator: " | ") + " |\n"
        sb += "|" + Array(repeating: " --- ", count: width).joined(separator: "|") + "|\n"
        for row in normalized.dropFirst() {
            sb += "| " + row.joined(separator: " | ") + " |\n"
        }
        sb += "\n"
        return sb
    }

    // MARK: - HTML 엔티티 디코드

    private static func decodeHtmlEntities(_ text: String) -> String {
        text
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&apos;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")
    }
}
