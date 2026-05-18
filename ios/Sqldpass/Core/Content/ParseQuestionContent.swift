import Foundation

/// 문제 본문을 `QuestionSegment` 배열로 분리.
///
/// Android `mobile/.../text/MarkdownSegments.kt` 의 `splitMarkdownSegments` 와 동치 + GFM Table 추가.
/// 사용 순서: `EnsureCodeFences.normalize(text)` → `ParseQuestionContent.parse(_:)`.
///
/// - fenced code (` ```lang\n...``` `): `.codeBlock`
/// - inline `<svg>...</svg>`: `.inlineSVG`
/// - `<img src="...">`: `.image`
/// - GFM table (`| col |...`): `.table`
/// - 나머지: `.markdown`
///
/// 코드블록 내부의 svg/img/table 은 무시 (코드 예시일 수 있음).
enum ParseQuestionContent {

    private struct Hit {
        let range: NSRange
        let segment: QuestionSegment
    }

    static func parse(_ text: String) -> [QuestionSegment] {
        let ns = text as NSString
        var hits: [Hit] = []

        // 1) fenced code
        let fenced = fencedRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for m in fenced {
            let lang = m.range(at: 1).location != NSNotFound
                ? ns.substring(with: m.range(at: 1))
                : ""
            let code = m.range(at: 2).location != NSNotFound
                ? ns.substring(with: m.range(at: 2))
                : ""
            let trimmed = code.trimmingCharacters(in: CharacterSet(charactersIn: "\n "))
            hits.append(.init(
                range: m.range,
                segment: .codeBlock(language: lang.isEmpty ? nil : lang, code: trimmed)
            ))
        }

        // 2) inline SVG — 코드블록 내부는 스킵
        let svgs = svgRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for m in svgs {
            if Self.isInside(m.range, hits: hits) { continue }
            hits.append(.init(range: m.range, segment: .inlineSVG(ns.substring(with: m.range))))
        }

        // 3) <img>
        let imgs = imgRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for m in imgs {
            if Self.isInside(m.range, hits: hits) { continue }
            let attrs = ns.substring(with: m.range(at: 1))
            guard let src = extractAttr(srcRegex, from: attrs), !src.isEmpty else { continue }
            let alt = extractAttr(altRegex, from: attrs)
            hits.append(.init(range: m.range, segment: .image(src: src, alt: alt)))
        }

        // 4) GFM table — 헤더 + separator + 한 줄 이상
        let tables = tableRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for m in tables {
            if Self.isInside(m.range, hits: hits) { continue }
            let raw = ns.substring(with: m.range)
            guard let rows = parseGfmTable(raw) else { continue }
            hits.append(.init(range: m.range, segment: .table(rows: rows)))
        }

        if hits.isEmpty {
            return [.markdown(text)]
        }

        let sorted = hits.sorted { $0.range.location < $1.range.location }
        var out: [QuestionSegment] = []
        var cursor = 0
        for h in sorted {
            if h.range.location > cursor {
                let before = ns.substring(with: NSRange(location: cursor, length: h.range.location - cursor))
                if !before.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    out.append(.markdown(before))
                }
            }
            out.append(h.segment)
            cursor = h.range.location + h.range.length
        }
        if cursor < ns.length {
            let tail = ns.substring(with: NSRange(location: cursor, length: ns.length - cursor))
            if !tail.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                out.append(.markdown(tail))
            }
        }
        return out
    }

    // MARK: - Helpers

    private static func isInside(_ range: NSRange, hits: [Hit]) -> Bool {
        hits.contains { h in
            // codeBlock 안에 있는지만 검사 (svg/img/table 끼리는 겹치지 않는다 가정)
            if case .codeBlock = h.segment {
                return range.location >= h.range.location
                    && range.location < h.range.location + h.range.length
            }
            return false
        }
    }

    private static func extractAttr(_ regex: NSRegularExpression, from attrs: String) -> String? {
        let ns = attrs as NSString
        guard let m = regex.firstMatch(in: attrs, options: [], range: NSRange(location: 0, length: ns.length))
        else { return nil }
        let g1 = m.range(at: 1)
        if g1.location != NSNotFound, g1.length > 0 {
            return ns.substring(with: g1).trimmingCharacters(in: .whitespaces)
        }
        let g2 = m.range(at: 2)
        if g2.location != NSNotFound, g2.length > 0 {
            return ns.substring(with: g2).trimmingCharacters(in: .whitespaces)
        }
        return nil
    }

    /// GFM table 본문을 row 배열로 파싱. 첫 줄 = 헤더, 둘째 줄 = separator(스킵), 나머지 = data.
    /// 셀 좌우 공백 trim, 빈 셀은 " " 로 유지.
    private static func parseGfmTable(_ raw: String) -> [[String]]? {
        let lines = raw
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        guard lines.count >= 2 else { return nil }
        // separator (---) 라인 확인
        let separatorOk = lines[1].contains("---")
        guard separatorOk else { return nil }
        var rows: [[String]] = []
        for (idx, line) in lines.enumerated() where idx != 1 {
            let cells = splitTableRow(line)
            rows.append(cells)
        }
        guard !rows.isEmpty else { return nil }
        let width = rows.map(\.count).max() ?? 0
        guard width > 0 else { return nil }
        return rows.map { row in
            row.count == width ? row : row + Array(repeating: " ", count: width - row.count)
        }
    }

    private static func splitTableRow(_ line: String) -> [String] {
        var trimmed = line
        if trimmed.hasPrefix("|") { trimmed.removeFirst() }
        if trimmed.hasSuffix("|") { trimmed.removeLast() }
        return trimmed
            .split(separator: "|", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .map { $0.isEmpty ? " " : $0 }
    }

    // MARK: - Regexes

    private static let fencedRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"```([a-zA-Z0-9_+\-]*)\s*\n([\s\S]*?)```"#, options: [])
    }()
    private static let svgRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<svg\b[^>]*>[\s\S]*?</svg>"#, options: [.caseInsensitive])
    }()
    private static let imgRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"<img\b([^>]*?)/?>"#, options: [.caseInsensitive])
    }()
    private static let tableRegex: NSRegularExpression = {
        // 헤더 라인 + separator(---) 라인 + 한 줄 이상 본문
        try! NSRegularExpression(
            pattern: #"(?m)^\|.+\|\s*\n\|[\s\-:|]+\|\s*\n(?:\|.+\|\s*\n?)+"#,
            options: []
        )
    }()
    private static let srcRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"src\s*=\s*(?:"([^"]+)"|'([^']+)')"#, options: [.caseInsensitive])
    }()
    private static let altRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"alt\s*=\s*(?:"([^"]*)"|'([^']*)')"#, options: [.caseInsensitive])
    }()
}
