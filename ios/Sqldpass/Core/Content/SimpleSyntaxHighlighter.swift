import Foundation
import SwiftUI

/// 순수 Swift `NSRegularExpression` 기반 가벼운 신택스 하이라이터.
///
/// 외부 SPM 의존 없이 SQL/Java/Python/C/JS/TS/Shell 6 언어의 주석/문자열/숫자/키워드를
/// 디자인 토큰 색으로 칠한 `AttributedString` 을 반환한다. 알 수 없는 언어 / nil 입력은
/// 폰트와 기본 색만 적용한 단색 `AttributedString` 을 반환 (fallback).
///
/// 패스 충돌 회피 전략: 매치 순서는 주석 → 문자열 → 숫자 → 키워드. 앞 패스가 차지한
/// `NSRange` 와 겹치는 매치는 `NSMutableIndexSet` 마스킹으로 skip 한다. 이로써 주석
/// 안의 키워드가 emerald 로 잘못 칠해지지 않는다.
enum SimpleSyntaxHighlighter {

    // MARK: - Public API

    static func highlight(_ code: String, language: String?) -> AttributedString {
        var attributed = AttributedString(code)
        attributed.font = .system(.footnote, design: .monospaced)
        attributed.foregroundColor = Color.appCodeText

        let normalized = normalize(language: language)
        guard normalized != .plain else { return attributed }

        let patterns = patterns(for: normalized)
        let occupied = NSMutableIndexSet()
        let ns = code as NSString
        let fullRange = NSRange(location: 0, length: ns.length)

        for (regex, color) in patterns {
            let matches = regex.matches(in: code, options: [], range: fullRange)
            for match in matches {
                let range = match.range
                if range.length == 0 { continue }
                if intersects(occupied, range: range) { continue }
                occupied.add(in: range)
                guard let attrRange = attributedRange(from: range, in: code, attributed: attributed) else {
                    continue
                }
                attributed[attrRange].foregroundColor = color
            }
        }

        return attributed
    }

    // MARK: - Language

    private enum Language {
        case sql
        case java
        case python
        case c
        case jsTs
        case shell
        case plain
    }

    private static func normalize(language: String?) -> Language {
        guard let raw = language?.lowercased(), !raw.isEmpty else { return .plain }
        switch raw {
        case "sql":
            return .sql
        case "python", "py":
            return .python
        case "java":
            return .java
        case "c", "cpp", "c++":
            return .c
        case "javascript", "js", "typescript", "ts":
            return .jsTs
        case "bash", "sh", "shell":
            return .shell
        default:
            return .plain
        }
    }

    // MARK: - Patterns

    private static func patterns(for language: Language) -> [(NSRegularExpression, Color)] {
        var result: [(NSRegularExpression, Color)] = []

        // 1. 주석
        for pattern in commentPatterns(for: language) {
            if let regex = try? NSRegularExpression(pattern: pattern, options: []) {
                result.append((regex, Color.appTextMuted))
            }
        }

        // 2. 문자열 (공통)
        for pattern in [#"'(?:\\.|[^'\\])*'"#, #""(?:\\.|[^"\\])*""#] {
            if let regex = try? NSRegularExpression(pattern: pattern, options: []) {
                result.append((regex, Color.appCodeInlineFG))
            }
        }

        // 3. 숫자 (공통)
        if let regex = try? NSRegularExpression(pattern: #"\b\d+(?:\.\d+)?\b"#, options: []) {
            result.append((regex, Color.semanticInfo))
        }

        // 4. 키워드 — 언어별
        if let keywordPattern = keywordPattern(for: language) {
            var options: NSRegularExpression.Options = []
            if language == .sql {
                options.insert(.caseInsensitive)
            }
            if let regex = try? NSRegularExpression(pattern: keywordPattern, options: options) {
                result.append((regex, Color.brandPrimary))
            }
        }

        return result
    }

    private static func commentPatterns(for language: Language) -> [String] {
        switch language {
        case .sql:
            return [#"--[^\n]*"#, #"/\*[\s\S]*?\*/"#]
        case .java, .c, .jsTs:
            return [#"//[^\n]*"#, #"/\*[\s\S]*?\*/"#]
        case .python, .shell:
            return [#"#[^\n]*"#]
        case .plain:
            return []
        }
    }

    private static func keywordPattern(for language: Language) -> String? {
        switch language {
        case .sql:
            return #"\b(SELECT|FROM|WHERE|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|GROUP|ORDER|BY|HAVING|JOIN|INNER|LEFT|RIGHT|FULL|CROSS|OUTER|ON|UNION|ALL|INTERSECT|MINUS|EXCEPT|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|ALTER|DROP|TABLE|INDEX|VIEW|TRIGGER|PROCEDURE|FUNCTION|AS|DISTINCT|CASE|WHEN|THEN|ELSE|END|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT|BEGIN|TRANSACTION|LIMIT|OFFSET|FETCH|RETURNING|PARTITION|WINDOW|RANGE|ROWS|ASC|DESC|EXISTS|ANY|SOME|COUNT|SUM|AVG|MIN|MAX)\b"#
        case .java:
            return #"\b(public|private|protected|static|final|abstract|class|interface|extends|implements|new|this|super|return|void|int|long|short|byte|char|boolean|float|double|String|if|else|switch|case|default|for|while|do|break|continue|try|catch|finally|throw|throws|import|package|null|true|false)\b"#
        case .python:
            return #"\b(def|class|return|if|elif|else|for|while|in|not|and|or|is|None|True|False|import|from|as|pass|break|continue|try|except|finally|raise|with|lambda|yield|global|nonlocal|self)\b"#
        case .c:
            return #"\b(int|long|short|char|float|double|void|unsigned|signed|struct|union|enum|typedef|const|static|extern|return|if|else|switch|case|default|for|while|do|break|continue|sizeof|include|define|true|false|NULL)\b"#
        case .jsTs:
            return #"\b(var|let|const|function|return|if|else|switch|case|default|for|while|do|break|continue|try|catch|finally|throw|new|this|class|extends|export|import|from|as|of|in|typeof|instanceof|null|undefined|true|false|async|await|yield)\b"#
        case .shell:
            return #"\b(if|then|else|elif|fi|for|while|do|done|case|esac|in|function|return|export|local|readonly|echo|cd|exit)\b"#
        case .plain:
            return nil
        }
    }

    // MARK: - Helpers

    /// `NSMutableIndexSet` 가 주어진 `NSRange` 와 한 인덱스라도 겹치는지 확인.
    private static func intersects(_ set: NSMutableIndexSet, range: NSRange) -> Bool {
        guard range.length > 0 else { return false }
        return set.intersects(in: range)
    }

    /// `NSRange` → `Range<AttributedString.Index>` 변환.
    ///
    /// `NSRange` 는 UTF-16 단위라 직접 `AttributedString.Index` 로 갈 수 없다.
    /// 1) `NSRange` → `Range<String.Index>` (source `String` 기준)
    /// 2) `Range<String.Index>` → `Range<AttributedString.Index>` (iOS 15+ 이니셜라이저)
    private static func attributedRange(
        from nsRange: NSRange,
        in source: String,
        attributed: AttributedString
    ) -> Range<AttributedString.Index>? {
        guard let stringRange = Range(nsRange, in: source) else { return nil }
        return Range<AttributedString.Index>(stringRange, in: attributed)
    }
}
