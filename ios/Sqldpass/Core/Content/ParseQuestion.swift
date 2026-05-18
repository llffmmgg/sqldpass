import Foundation

/// 백엔드 응답의 `content` 한 덩어리를 본문(body) / 보기(options) 로 분리.
///
/// Android `mobile/.../text/QuestionParser.kt` (`parseQuestion`) 와 1:1 동치.
/// ①②③④ 마커로 시작하는 줄을 option, 그 이전은 body, option 시작 후 마커 없는 줄은 직전 option 에 이어붙임.
struct ParsedQuestion {
    let body: String
    let options: [String]
}

enum QuestionParser {
    private static let optionMarkers: [Character] = ["①", "②", "③", "④"]
    private static let leadingMarkerRegex: NSRegularExpression = {
        try! NSRegularExpression(pattern: "^[①②③④]\\s*")
    }()

    static func parse(_ content: String) -> ParsedQuestion {
        let lines = content.components(separatedBy: "\n")
        var bodyLines: [String] = []
        var options: [String] = []
        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if let first = trimmed.first, optionMarkers.contains(first) {
                let ns = trimmed as NSString
                let stripped = leadingMarkerRegex.stringByReplacingMatches(
                    in: trimmed,
                    options: [],
                    range: NSRange(location: 0, length: ns.length),
                    withTemplate: ""
                )
                options.append(stripped)
            } else if options.isEmpty {
                bodyLines.append(line)
            } else {
                let last = options.removeLast()
                options.append(last + "\n" + line)
            }
        }
        return ParsedQuestion(
            body: bodyLines.joined(separator: "\n"),
            options: options
        )
    }
}
