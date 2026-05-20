import SwiftUI

/// 백엔드 `examType` enum raw string → 사용자 표시 한글 라벨.
/// CertCatalog 와 동일한 풀네임 톤(줄임말 사용 X).
func displayLabel(forExamType examType: String) -> String {
    switch examType {
    case "SQLD":                  return "SQLD"
    case "ENGINEER_PRACTICAL":    return "정보처리기사 실기"
    case "ENGINEER_WRITTEN":      return "정보처리기사 필기"
    case "COMPUTER_LITERACY_1": return "컴퓨터활용능력 1급"
    case "COMPUTER_LITERACY_2": return "컴퓨터활용능력 2급"
    case "ADSP":                  return "ADsP"
    default:                      return examType
    }
}

func accentColor(forExamType examType: String) -> Color {
    switch examType {
    case "SQLD":                  return .certSQLD
    case "ENGINEER_PRACTICAL":    return .certEngineerPractical
    case "ENGINEER_WRITTEN":      return .certEngineerWritten
    case "COMPUTER_LITERACY_1": return .certComputerL1
    case "COMPUTER_LITERACY_2": return .certComputerL2
    case "ADSP":                  return .certADSP
    default:                      return .brandPrimary
    }
}

extension MockExamSummary {
    var typeAccentColor: Color { accentColor(forExamType: examType) }
    var typeLabel: String { displayLabel(forExamType: examType) }

    var bestScoreLabel: String? {
        guard let correct = bestCorrectCount, let total = bestTotalCount else { return nil }
        return "\(correct) / \(total)"
    }

    var isPastExam: Bool { kind == "PAST_EXAM" }
}

extension MockExamDetail {
    var typeAccentColor: Color { accentColor(forExamType: examType) }
    var typeLabel: String { displayLabel(forExamType: examType) }
}
