package com.sqldpass.app.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OAuthLoginRequest(val idToken: String)

@JsonClass(generateAdapter = true)
data class OAuthLoginResponse(
    val token: String,
    val nickname: String?,
    val isNew: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SnapshotResponse(
    val version: String,
    val generatedAt: String?,
    val mockExamCount: Int,
    val questionCount: Int,
    val mockExams: List<MockExamSnapshot>,
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class MockExamDetail(
    val id: Long,
    val name: String,
    val examType: String?,
    val sequence: Int,
    val totalQuestions: Int,
    val questions: List<MockExamQuestion>,
)

@JsonClass(generateAdapter = true)
data class MockExamQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
    val subjectId: Long?,
    val subjectName: String?,
)

@JsonClass(generateAdapter = true)
data class SolveAnswerRequest(
    val questionId: Long,
    val selectedOption: Int? = null,
    val answerText: String? = null,
)

@JsonClass(generateAdapter = true)
data class SolveRequest(
    val subjectId: Long? = null,
    val mockExamId: Long? = null,
    val source: String? = null,
    val answers: List<SolveAnswerRequest>,
    val clientSubmissionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class SolveResponse(
    val id: Long,
    val subjectId: Long?,
    val mockExamId: Long?,
    val totalCount: Int,
    val correctCount: Int,
    val score: Int,
    val solvedAt: String,
    val answers: List<SolveAnswerResponse> = emptyList(),
    val currentStreak: Int? = null,
    val milestoneReached: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SolveAnswerResponse(
    val questionId: Long,
    val selectedOption: Int,
    val correctOption: Int,
    val correct: Boolean,
)

@JsonClass(generateAdapter = true)
data class VerifyPlayBillingRequest(
    val productId: String,
    val purchaseToken: String,
)

@JsonClass(generateAdapter = true)
data class VerifyPaymentResponse(
    val paymentId: String?,
    val amount: Int?,
    val productName: String?,
    val plan: String?,
    val expiresAt: String?,
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class PastExamQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
    val subjectId: Long?,
    val subjectName: String?,
)

@JsonClass(generateAdapter = true)
data class PastExamAnswer(
    val questionId: Long,
    val selectedOption: Int? = null,
    val answerText: String? = null,
)

@JsonClass(generateAdapter = true)
data class PastExamGradeRequest(
    val answers: List<PastExamAnswer>,
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class PastExamSubjectScore(
    val subjectName: String,
    val total: Int,
    val correct: Int,
    val rate: Double,
    val weighted: Int,
    val failed: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SubjectResponse(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
    val parentName: String? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionResponse(
    val id: Long,
    val subjectId: Long?,
    val content: String,
    val questionType: String?,
)

@JsonClass(generateAdapter = true)
data class StreakResponse(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSolvedDate: String? = null,
    val milestoneDays: List<Int> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OverallAvgResponse(
    val overallAvg: Double? = null,
    val myRecentAvg: Double? = null,
    val periodDays: Int? = null,
)

@JsonClass(generateAdapter = true)
data class BestScoreEntry(
    val correctCount: Int,
    val totalCount: Int,
)

@JsonClass(generateAdapter = true)
data class BookmarkSummary(
    val questionId: Long,
    val questionContent: String,
    val questionType: String?,
    val subjectId: Long?,
    val subjectName: String?,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class BookmarkListResponse(
    val items: List<BookmarkSummary> = emptyList(),
    val totalCount: Long = 0,
    val limited: Boolean = false,
    val freeLimit: Int = 30,
)

@JsonClass(generateAdapter = true)
data class BookmarkExistsResponse(val exists: Boolean)

@JsonClass(generateAdapter = true)
data class CreateFeedbackRequest(
    val type: String,
    val questionId: Long? = null,
    val content: String,
    val pageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    val active: Boolean,
    val plan: String? = null,
    val expiresAt: String? = null,
    val removesAds: Boolean = false,
    val allowsPdf: Boolean = false,
    val hasLibraryAccess: Boolean = false,
    val allowsPremium: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class WrongAnswerSummary(
    val questionId: Long,
    val questionContent: String,
    val subjectId: Long?,
    val subjectName: String?,
    val wrongCount: Int,
    val lastWrongAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class WrongAnswerStatsSummary(
    val subjectId: Long,
    val subjectName: String,
    val totalSolved: Int,
    val wrongCount: Int,
    val wrongRate: Int,
)

@JsonClass(generateAdapter = true)
data class UpdateNicknameRequest(val nickname: String)

@JsonClass(generateAdapter = true)
data class MemberMeResponse(
    val id: Long,
    val nickname: String?,
    val provider: String? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class DailyCountResponse(
    val date: String, // ISO yyyy-MM-dd
    val count: Long,
)
