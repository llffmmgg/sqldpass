package com.sqldpass.app.data

data class OAuthLoginRequest(val idToken: String)

data class OAuthLoginResponse(
    val token: String,
    val nickname: String?,
    val isNew: Boolean = false,
)

data class SnapshotResponse(
    val version: String,
    val generatedAt: String?,
    val mockExamCount: Int,
    val questionCount: Int,
    val mockExams: List<MockExamSnapshot>,
)

data class MockExamSnapshot(
    val id: Long,
    val name: String,
    val examType: String?,
    val sequence: Int,
    val visibility: String?,
    val expertVerified: Boolean,
    val kind: String?,
    val examYear: Int?,
    val examRound: Int?,
    val examDate: String?,
    val template: String?,
    val questions: List<QuestionSnapshot>,
)

data class QuestionSnapshot(
    val id: Long,
    val displayOrder: Int?,
    val subjectId: Long?,
    val subjectName: String?,
    val subjectParentName: String?,
    val content: String,
    val questionType: String?,
    val correctOption: Int?,
    val answer: String?,
    val keywords: List<String> = emptyList(),
    val explanation: String?,
    val summary: String?,
    val topic: String?,
    val difficulty: Int?,
)

data class MockExamSummary(
    val id: Long,
    val name: String,
    val examType: String?,
    val sequence: Int,
    val totalQuestions: Int,
    val difficultyLabel: String?,
    val solved: Boolean,
    val bestCorrectCount: Int?,
    val bestTotalCount: Int?,
    val visibility: String?,
    val purchased: Boolean = false,
    val isPremium: Boolean = false,
)

data class MockExamDetail(
    val id: Long,
    val name: String,
    val examType: String?,
    val sequence: Int,
    val totalQuestions: Int,
    val questions: List<MockExamQuestion>,
)

data class MockExamQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
    val subjectId: Long?,
    val subjectName: String?,
)

data class SolveAnswerRequest(
    val questionId: Long,
    val selectedOption: Int? = null,
    val answerText: String? = null,
)

data class SolveRequest(
    val subjectId: Long? = null,
    val mockExamId: Long? = null,
    val source: String? = null,
    val answers: List<SolveAnswerRequest>,
    val clientSubmissionId: String? = null,
)

data class SolveResponse(
    val id: Long,
    val subjectId: Long?,
    val mockExamId: Long?,
    val totalCount: Int,
    val correctCount: Int,
    val score: Int,
    val solvedAt: String,
)

data class VerifyPlayBillingRequest(
    val productId: String,
    val purchaseToken: String,
)

data class VerifyPaymentResponse(
    val paymentId: String?,
    val amount: Int?,
    val productName: String?,
    val plan: String?,
    val expiresAt: String?,
)

data class PastExamSummary(
    val id: Long,
    val name: String,
    val examType: String?,
    val certSlug: String?,
    val totalQuestions: Int,
    val examYear: Int?,
    val examRound: Int?,
    val examDate: String?,
    val expertVerified: Boolean = false,
    val createdAt: String? = null,
    val solved: Boolean = false,
    val bestCorrectCount: Int? = null,
    val bestTotalCount: Int? = null,
)

data class PastExamDetail(
    val id: Long,
    val name: String,
    val examType: String?,
    val certSlug: String?,
    val totalQuestions: Int,
    val examYear: Int?,
    val examRound: Int?,
    val examDate: String?,
    val expertVerified: Boolean = false,
    val questions: List<PastExamQuestion>,
)

data class PastExamQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
    val subjectId: Long?,
    val subjectName: String?,
)

data class PastExamAnswer(
    val questionId: Long,
    val selectedOption: Int? = null,
    val answerText: String? = null,
)

data class PastExamGradeRequest(
    val answers: List<PastExamAnswer>,
)

data class PastExamGradeResponse(
    val totalCount: Int,
    val correctCount: Int,
    val score: Int,
    val items: List<PastExamGradedItem> = emptyList(),
    val solveId: Long? = null,
    val subjectScores: List<PastExamSubjectScore> = emptyList(),
    val passed: Boolean = false,
    val passReason: String? = null,
    val milestoneReached: Int? = null,
)

data class PastExamGradedItem(
    val questionId: Long,
    val correct: Boolean,
    val partialScore: Double? = null,
    val selectedOption: Int? = null,
    val submittedAnswerText: String? = null,
    val correctOption: Int? = null,
    val answer: String? = null,
    val keywords: List<String> = emptyList(),
    val explanation: String? = null,
)

data class PastExamSubjectScore(
    val subjectName: String,
    val total: Int,
    val correct: Int,
    val rate: Double,
    val weighted: Int,
    val failed: Boolean = false,
)

data class SubjectResponse(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
    val parentName: String? = null,
)

data class QuestionResponse(
    val id: Long,
    val subjectId: Long?,
    val content: String,
    val questionType: String?,
)

data class StreakResponse(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSolvedDate: String? = null,
    val milestoneDays: List<Int> = emptyList(),
)

data class OverallAvgResponse(
    val overallAvg: Double? = null,
    val myRecentAvg: Double? = null,
    val periodDays: Int? = null,
)

data class BestScoreEntry(
    val correctCount: Int,
    val totalCount: Int,
)
