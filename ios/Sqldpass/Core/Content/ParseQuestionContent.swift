import Foundation

/// Splits question body content into renderable segments.
///
/// Usage order: `EnsureCodeFences.normalize(text)` then `ParseQuestionContent.parse(_:)`.
/// Fenced code is collected first, and later rich-content parsing is skipped inside those ranges.
enum ParseQuestionContent {

    private struct Hit {
        let range: NSRange
        let segment: QuestionSegment
    }

    private struct LineRecord {
        let content: String
        let contentRange: NSRange
        let fullRange: NSRange
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
            let trimmed = code.trimmingCharacters(in: CharacterSet(charactersIn: "\n"))
            hits.append(.init(
                range: m.range,
                segment: .codeBlock(language: lang.isEmpty ? nil : lang, code: trimmed)
            ))
        }

        // 2) inline SVG
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

        // 4) markdown image
        let markdownImages = markdownImageRegex.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for m in markdownImages {
            if Self.isInside(m.range, hits: hits) { continue }
            let alt = ns.substring(with: m.range(at: 1))
            let src = ns.substring(with: m.range(at: 2)).trimmingCharacters(in: .whitespacesAndNewlines)
            guard !src.isEmpty else { continue }
            hits.append(.init(range: m.range, segment: .image(src: src, alt: alt.isEmpty ? nil : alt)))
        }

        // 5) GFM table
        hits.append(contentsOf: findGfmTables(in: ns, excluding: hits))

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

    /// Parses GFM tables line-by-line so escaped pipes and fenced-code ranges are respected.
    private static func findGfmTables(in ns: NSString, excluding hits: [Hit]) -> [Hit] {
        let lines = lineRecords(in: ns)
        var tables: [Hit] = []
        var idx = 0

        while idx + 1 < lines.count {
            let headerLine = lines[idx]
            let delimiterLine = lines[idx + 1]

            guard !isInside(headerLine.contentRange, hits: hits),
                  !isInside(delimiterLine.contentRange, hits: hits),
                  let header = parseTableContentRow(headerLine.content),
                  header.count >= 2,
                  let delimiterWidth = parseDelimiterRow(delimiterLine.content),
                  delimiterWidth == header.count
            else {
                idx += 1
                continue
            }

            let width = header.count
            var rows = [normalizeTableRow(header, width: width)]
            var lastLineIndex = idx + 1
            var bodyIndex = idx + 2

            while bodyIndex < lines.count {
                let bodyLine = lines[bodyIndex]
                let trimmed = bodyLine.content.trimmingCharacters(in: .whitespaces)
                if trimmed.isEmpty || isInside(bodyLine.contentRange, hits: hits) {
                    break
                }
                guard let bodyRow = parseTableContentRow(bodyLine.content) else {
                    break
                }
                rows.append(normalizeTableRow(bodyRow, width: width))
                lastLineIndex = bodyIndex
                bodyIndex += 1
            }

            let start = headerLine.fullRange.location
            let end = lines[lastLineIndex].fullRange.location + lines[lastLineIndex].fullRange.length
            tables.append(.init(
                range: NSRange(location: start, length: end - start),
                segment: .table(rows: rows)
            ))
            idx = lastLineIndex + 1
        }

        return tables
    }

    private static func lineRecords(in ns: NSString) -> [LineRecord] {
        guard ns.length > 0 else { return [] }

        var records: [LineRecord] = []
        var location = 0
        while location < ns.length {
            var lineStart = 0
            var lineEnd = 0
            var contentsEnd = 0
            ns.getLineStart(&lineStart, end: &lineEnd, contentsEnd: &contentsEnd, for: NSRange(location: location, length: 0))

            let contentRange = NSRange(location: lineStart, length: contentsEnd - lineStart)
            let fullRange = NSRange(location: lineStart, length: lineEnd - lineStart)
            records.append(.init(
                content: ns.substring(with: contentRange),
                contentRange: contentRange,
                fullRange: fullRange
            ))
            location = lineEnd
        }
        return records
    }

    private static func parseTableContentRow(_ line: String) -> [String]? {
        let cells = splitTableRow(line)
        return cells.count >= 2 ? cells : nil
    }

    private static func parseDelimiterRow(_ line: String) -> Int? {
        let cells = splitTableRow(line)
        guard cells.count >= 2, cells.allSatisfy(isValidDelimiterCell) else { return nil }
        return cells.count
    }

    private static func isValidDelimiterCell(_ cell: String) -> Bool {
        var value = cell.trimmingCharacters(in: .whitespaces)
        if value.first == ":" {
            value.removeFirst()
        }
        if value.last == ":" {
            value.removeLast()
        }
        return value.count >= 3 && value.allSatisfy { $0 == "-" }
    }

    private static func normalizeTableRow(_ row: [String], width: Int) -> [String] {
        if row.count == width {
            return row
        }
        if row.count > width {
            return Array(row.prefix(width))
        }
        return row + Array(repeating: "", count: width - row.count)
    }

    private static func splitTableRow(_ line: String) -> [String] {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        var cells: [String] = []
        var cell = ""
        var backslashCount = 0

        for char in trimmed {
            if char == "\\" {
                backslashCount += 1
                cell.append(char)
                continue
            }

            if char == "|" {
                if backslashCount % 2 == 1 {
                    cell.removeLast()
                    cell.append("|")
                } else {
                    cells.append(cell.trimmingCharacters(in: .whitespaces))
                    cell = ""
                }
                backslashCount = 0
                continue
            }

            backslashCount = 0
            cell.append(char)
        }

        cells.append(cell.trimmingCharacters(in: .whitespaces))
        if trimmed.hasPrefix("|"), !cells.isEmpty {
            cells.removeFirst()
        }
        if trimmed.hasSuffix("|"), !cells.isEmpty {
            cells.removeLast()
        }
        return cells
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
    private static let markdownImageRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"!\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)"#, options: [])
    }()
    private static let srcRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"src\s*=\s*(?:"([^"]+)"|'([^']+)')"#, options: [.caseInsensitive])
    }()
    private static let altRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"alt\s*=\s*(?:"([^"]*)"|'([^']*)')"#, options: [.caseInsensitive])
    }()
}
