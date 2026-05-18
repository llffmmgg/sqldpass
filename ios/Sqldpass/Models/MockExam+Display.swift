import SwiftUI

extension MockExamSummary {
    /// 시험 종류별 액센트 색상 (디자인 토큰의 cert.* 활용)
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

    /// 사용자에게 보이는 시험 종류명
    var typeLabel: String {
        switch examType {
        case "SQLD": return "SQLD"
        case "ENGINEER_PRACTICAL": return "정처기 실기"
        case "ENGINEER_WRITTEN": return "정처기 필기"
        case "COMPUTER_LITERACY_ONE": return "컴활 1급"
        case "COMPUTER_LITERACY_TWO": return "컴활 2급"
        case "ADSP": return "ADsP"
        default: return examType
        }
    }

    /// 풀이 진행 표시 ("12 / 50")
    var bestScoreLabel: String? {
        guard let correct = bestCorrectCount, let total = bestTotalCount else { return nil }
        return "\(correct) / \(total)"
    }

    var isPastExam: Bool { kind == "PAST_EXAM" }
}
