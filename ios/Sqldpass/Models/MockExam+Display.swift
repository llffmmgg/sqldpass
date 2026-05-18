import SwiftUI

extension MockExamSummary {
    var typeAccentColor: Color {
        switch examType {
        case "SQLD": return .certSQLD
        case "ENGINEER_PRACTICAL": return .certEngineerPractical
        case "ENGINEER_WRITTEN": return .certEngineerWritten
        case "COMPUTER_LITERACY_ONE": return .certComputerL1
        case "COMPUTER_LITERACY_TWO": return .certComputerL2
        case "ADSP": return .certADSP
        default: return .brandPrimary
        }
    }

    var typeLabel: String {
        switch examType {
        case "SQLD": return "SQLD"
        case "ENGINEER_PRACTICAL": return "정보처리 실기"
        case "ENGINEER_WRITTEN": return "정보처리 필기"
        case "COMPUTER_LITERACY_ONE": return "컴활 1급"
        case "COMPUTER_LITERACY_TWO": return "컴활 2급"
        case "ADSP": return "ADsP"
        default: return examType
        }
    }

    var bestScoreLabel: String? {
        guard let correct = bestCorrectCount, let total = bestTotalCount else { return nil }
        return "\(correct) / \(total)"
    }

    var isPastExam: Bool { kind == "PAST_EXAM" }
}
