import Foundation

/// 백엔드 응답: GET /api/wrong-answers
struct WrongAnswer: Codable, Equatable, Hashable, Identifiable {
    let questionId: Int64
    let questionContent: String
    let questionType: String
    let subjectId: Int64
    let subjectName: String
    let wrongCount: Int
    let lastWrongAt: String

    var id: Int64 { questionId }
}

/// 백엔드 응답: GET /api/wrong-answers/stats
struct WrongAnswerStats: Codable, Equatable, Identifiable {
    let subjectId: Int64
    let subjectName: String
    let totalSolved: Int
    let wrongCount: Int
    /// 0~100 정수 (백엔드 계산)
    let wrongRate: Int

    var id: Int64 { subjectId }
}

/// 백엔드 응답: GET /api/wrong-answers/preview — 잠금 화면용 미리보기
struct WrongAnswerPreview: Codable, Equatable, Identifiable {
    let questionId: Int64
    let questionContent: String
    let questionType: String
    let subjectName: String

    var id: Int64 { questionId }
}

/// 백엔드 응답: POST /api/wrong-answers/{questionId}/retry
struct WrongAnswerRetryResult: Codable, Equatable {
    let correct: Bool
    /// MCQ 정답 번호 (1~4). 단답형은 nil
    let correctOption: Int?
    /// 단답/약술형 모범답안. MCQ는 nil
    let correctAnswer: String?
    let explanation: String?
}

extension WrongAnswer {
    var isTextAnswerType: Bool {
        questionType.isTextAnswerType
    }
}

extension WrongAnswerPreview {
    var isTextAnswerType: Bool {
        questionType.isTextAnswerType
    }
}

private extension String {
    var isTextAnswerType: Bool {
        let normalized = uppercased()
        return normalized.contains("SHORT") || normalized.contains("DESCRIPTIVE") || normalized.contains("TEXT")
    }
}
