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
        text = autoFencePlainCode(text)
        text = normalizeOptionMarkers(text)
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
                .trimmingCharacters(in: CharacterSet(charactersIn: "\n"))
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

    // MARK: - 평문 코드 자동 펜싱 (웹 ensureCodeFences 1:1 동치)

    private enum AutoFenceLang: String {
        case sql, c, java, python
    }

    private static let hasKoreanRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"[가-힣]"#)
    }()

    private static let sqlStartRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT)\b"#,
            options: [.caseInsensitive]
        )
    }()

    private static let sqlContRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"^\s*(FROM|WHERE|AND|OR|GROUP\s+BY|ORDER\s+BY|HAVING|JOIN|INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|ON|UNION|UNION\s+ALL|INTERSECT|MINUS|EXCEPT|LIMIT|OFFSET|FETCH|VALUES|SET|RETURNING|WHEN|THEN|ELSE|END|CASE|INTO|USING|PARTITION\s+BY|WINDOW|RANGE|ROWS|TO|PUBLIC)\b"#,
            options: [.caseInsensitive]
        )
    }()

    private static let cStartRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"^\s*(#include\b|int\s+main\b|int\s+\w+\s*[=;(\[]|void\s+\w+\s*\(|char\s+\*?\w+|float\s+\w+|double\s+\w+|long\s+\w+|short\s+\w+|unsigned\s+\w+|struct\s+\w+|typedef\s+\b|return\s+\d|printf\s*\(|scanf\s*\(|while\s*\(|for\s*\(|if\s*\(|do\s*\{|switch\s*\()"#
        )
    }()

    private static let javaStartRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"^\s*(public\s+(static\s+)?(class|void|int|String|boolean)\b|private\s+\w+|protected\s+\w+|class\s+\w+\s*\{?|System\.out\.print|void\s+\w+\s*\(|try\s*\{|catch\s*\()"#
        )
    }()

    private static let pyStartRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: #"^\s*(def\s+\w+\s*\(|class\s+\w+\s*[:(]|import\s+\w+|from\s+\w+\s+import\b|print\s*\(|for\s+\w+\s+in\s+|while\s+.+:|if\s+.+:)"#
        )
    }()

    private static let contIndentedRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"^\s{2,}\S"#)
    }()

    private static let contSymbolsRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"^\s*([{}();,]|\/\/|\/\*|\*\/|else\b|return\b)"#)
    }()

    private static func regexMatches(_ regex: NSRegularExpression, _ string: String) -> Bool {
        let ns = string as NSString
        return regex.firstMatch(in: string, options: [], range: NSRange(location: 0, length: ns.length)) != nil
    }

    private static func detectAutoFenceLang(_ line: String) -> AutoFenceLang? {
        if regexMatches(hasKoreanRegex, line) { return nil }
        if regexMatches(sqlStartRegex, line) { return .sql }
        if regexMatches(cStartRegex, line) { return .c }
        if regexMatches(javaStartRegex, line) { return .java }
        if regexMatches(pyStartRegex, line) { return .python }
        return nil
    }

    private static func isAutoFenceContinuation(_ line: String, lang: AutoFenceLang) -> Bool {
        if line.trimmingCharacters(in: .whitespaces).isEmpty { return false }
        if regexMatches(hasKoreanRegex, line) { return false }
        if regexMatches(contIndentedRegex, line) || regexMatches(contSymbolsRegex, line) { return true }
        switch lang {
        case .sql:
            return regexMatches(sqlContRegex, line) || regexMatches(sqlStartRegex, line)
        case .c:
            return regexMatches(cStartRegex, line)
        case .java:
            return regexMatches(javaStartRegex, line)
        case .python:
            return regexMatches(pyStartRegex, line)
        }
    }

    private static func isFenceLine(_ line: String) -> Bool {
        // 웹과 동일: 라인 시작이 (공백 포함) ``` 로 시작.
        let trimmedLeading = line.drop(while: { $0 == " " || $0 == "\t" })
        return trimmedLeading.hasPrefix("```")
    }

    static func autoFencePlainCode(_ input: String) -> String {
        if input.isEmpty { return input }
        let lines = input.components(separatedBy: "\n")
        var out: [String] = []
        var i = 0
        while i < lines.count {
            let line = lines[i]

            // 1) 이미 펜스 안이면 닫는 펜스까지 통과
            if isFenceLine(line) {
                out.append(line)
                i += 1
                while i < lines.count && !isFenceLine(lines[i]) {
                    out.append(lines[i])
                    i += 1
                }
                if i < lines.count {
                    out.append(lines[i])
                    i += 1
                }
                continue
            }

            // 2) 코드 시작 키워드 감지 → 같은 블록으로 묶어 펜싱
            if let lang = detectAutoFenceLang(line) {
                var codeLines: [String] = [line]
                i += 1
                while i < lines.count {
                    let next = lines[i]
                    if next.trimmingCharacters(in: .whitespaces).isEmpty { break }
                    if !isAutoFenceContinuation(next, lang: lang) { break }
                    codeLines.append(next)
                    i += 1
                }
                // 1줄짜리 짧은 토큰은 펜싱 보류 (인라인 처리에 맡김)
                if codeLines.count == 1 && codeLines[0].count < 40 {
                    out.append(codeLines[0])
                    continue
                }
                out.append("```" + lang.rawValue)
                out.append(contentsOf: codeLines)
                out.append("```")
                continue
            }

            out.append(line)
            i += 1
        }
        return out.joined(separator: "\n")
    }

    // MARK: - 보기 마커 줄바꿈 정규화 (보수적)

    private static let circledMarkers: [Character] = ["\u{2460}", "\u{2461}", "\u{2462}", "\u{2463}"] // ① ② ③ ④

    // `1.` `2.` `3.` `4.` 위치 (단어 경계 뒤). 정규식 `(?<![\d.])\d\.` 로 `12.`/`1.2.` 등 오매치 제외.
    private static let dotMarkerRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"(?<![\d.])(\d)\."#)
    }()

    // `1)` `2)` `3)` `4)` 위치 (단어 경계 뒤).
    private static let parenMarkerRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: #"(?<![\d.])(\d)\)"#)
    }()

    /// 한 라인에서 (digit, NSRange) 매치를 모은다. digit 는 1~4 만 필터.
    private static func markerPositions(in line: String, regex: NSRegularExpression) -> [(digit: Int, range: NSRange)] {
        let ns = line as NSString
        let matches = regex.matches(in: line, options: [], range: NSRange(location: 0, length: ns.length))
        var result: [(Int, NSRange)] = []
        for m in matches {
            let digitRange = m.range(at: 1)
            guard digitRange.location != NSNotFound else { continue }
            let digitStr = ns.substring(with: digitRange)
            guard let d = Int(digitStr), (1...4).contains(d) else { continue }
            result.append((d, m.range))
        }
        return result
    }

    /// 원 숫자 마커 위치 (문자별 인덱스).
    private static func circledPositions(in line: String) -> [(digit: Int, index: String.Index)] {
        var result: [(Int, String.Index)] = []
        var idx = line.startIndex
        while idx < line.endIndex {
            let ch = line[idx]
            if let pos = circledMarkers.firstIndex(of: ch) {
                result.append((pos + 1, idx))
            }
            idx = line.index(after: idx)
        }
        return result
    }

    private static func processOptionLine(_ line: String) -> String {
        // 1) 원 숫자: ① ② ③ ④ 모두 등장 또는 라인 시작이 마커가 아닌데 ① 등장
        let circled = circledPositions(in: line)
        let hasAllCircled = Set(circled.map { $0.digit }).isSuperset(of: [1, 2, 3, 4])

        // 단일 ① 가운데 등장: 라인 시작(선행 공백 제외)이 ① 가 아닌데 라인 안에 ① 가 존재할 때 그 앞에 \n 삽입.
        let leadingTrimmed = line.drop(while: { $0 == " " || $0 == "\t" })
        let startsWithCircled1 = leadingTrimmed.first.map { circledMarkers.contains($0) } ?? false

        if hasAllCircled {
            // 첫 마커 제외, 각 ①②③④ 직전에 \n 삽입.
            var result = ""
            var first = true
            for ch in line {
                if circledMarkers.contains(ch) {
                    if !first {
                        result.append("\n")
                    }
                    first = false
                }
                result.append(ch)
            }
            return result
        }

        if !startsWithCircled1, let firstCircled = circled.first {
            // 본문 끝에 보기가 바로 이어 붙은 경우 — ① 단 하나라도 라인 중간에 있으면 그 앞에 \n 삽입.
            // (false-positive 거의 없음: 본문에 ①②③④ 가 단독 등장하는 일은 드묾)
            var result = String(line[line.startIndex..<firstCircled.index])
            result.append("\n")
            result.append(String(line[firstCircled.index..<line.endIndex]))
            // 처리 후 나머지 본문에서 추가 원 숫자 처리: ②③④ 가 있으면 동일하게 그 앞에 \n.
            // 위 hasAllCircled 가 false 인 경우라도 ②③④ 중 일부 등장 가능 — 보수적으로 단일 ① 케이스만 처리하고 그대로 둔다.
            return result
        }

        // 2) `1.` `2.` `3.` `4.` 모두 등장 (단어 경계)
        let dotMatches = markerPositions(in: line, regex: dotMarkerRegex)
        let dotDigits = Set(dotMatches.map { $0.digit })
        if dotDigits.isSuperset(of: [1, 2, 3, 4]) {
            return insertNewlinesBeforeMarkers(line: line, ranges: dotMatches.map { $0.range })
        }

        // 3) `1)` `2)` `3)` `4)` 모두 등장 (단어 경계)
        let parenMatches = markerPositions(in: line, regex: parenMarkerRegex)
        let parenDigits = Set(parenMatches.map { $0.digit })
        if parenDigits.isSuperset(of: [1, 2, 3, 4]) {
            return insertNewlinesBeforeMarkers(line: line, ranges: parenMatches.map { $0.range })
        }

        return line
    }

    /// `ranges` 시작 위치 기준 정렬 후 첫 마커 제외 각 마커 직전에 \n 삽입.
    private static func insertNewlinesBeforeMarkers(line: String, ranges: [NSRange]) -> String {
        let sorted = ranges.sorted { $0.location < $1.location }
        guard sorted.count >= 2 else { return line }
        let ns = line as NSString
        var result = ""
        var cursor = 0
        for (idx, range) in sorted.enumerated() {
            let chunk = ns.substring(with: NSRange(location: cursor, length: range.location - cursor))
            result.append(chunk)
            if idx > 0 {
                result.append("\n")
            }
            cursor = range.location
        }
        if cursor < ns.length {
            result.append(ns.substring(from: cursor))
        }
        return result
    }

    static func normalizeOptionMarkers(_ input: String) -> String {
        if input.isEmpty { return input }
        let lines = input.components(separatedBy: "\n")
        var out: [String] = []
        var inFence = false
        for line in lines {
            if isFenceLine(line) {
                inFence.toggle()
                out.append(line)
                continue
            }
            if inFence {
                out.append(line)
                continue
            }
            out.append(processOptionLine(line))
        }
        return out.joined(separator: "\n")
    }
}
