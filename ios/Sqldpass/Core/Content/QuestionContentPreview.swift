import Foundation

enum QuestionContentPreview {
    static func make(_ raw: String, maxLength: Int = 80) -> String {
        let normalized = EnsureCodeFences.normalize(raw)
        let parsed = QuestionParser.parse(normalized)
        let source = parsed.body.isEmpty ? normalized : parsed.body
        let stripped = stripRichContent(source)
            .replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard stripped.count > maxLength else { return stripped }
        let end = stripped.index(stripped.startIndex, offsetBy: maxLength)
        return String(stripped[..<end]) + "..."
    }

    private static func stripRichContent(_ text: String) -> String {
        var result = text
        result = result.replacingOccurrences(
            of: #"```[\s\S]*?```"#,
            with: " ",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"<svg\b[^>]*>[\s\S]*?</svg>"#,
            with: " ",
            options: [.regularExpression, .caseInsensitive]
        )
        result = result.replacingOccurrences(
            of: #"<img\b[^>]*>"#,
            with: " ",
            options: [.regularExpression, .caseInsensitive]
        )
        result = result.replacingOccurrences(
            of: #"<[^>]+>"#,
            with: " ",
            options: .regularExpression
        )
        result = result.replacingOccurrences(of: "`", with: "")
        result = result.replacingOccurrences(of: "**", with: "")
        result = result.replacingOccurrences(of: "&lt;", with: "<")
        result = result.replacingOccurrences(of: "&gt;", with: ">")
        result = result.replacingOccurrences(of: "&quot;", with: "\"")
        result = result.replacingOccurrences(of: "&#39;", with: "'")
        result = result.replacingOccurrences(of: "&apos;", with: "'")
        result = result.replacingOccurrences(of: "&nbsp;", with: " ")
        result = result.replacingOccurrences(of: "&amp;", with: "&")
        return result
    }
}
