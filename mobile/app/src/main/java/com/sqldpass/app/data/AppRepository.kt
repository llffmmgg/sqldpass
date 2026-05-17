package com.sqldpass.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sqldpass.app.data.local.MetadataEntity
import com.sqldpass.app.data.local.MockExamEntity
import com.sqldpass.app.data.local.PendingSolveEntity
import com.sqldpass.app.data.local.QuestionEntity
import com.sqldpass.app.data.local.SqldpassDatabase
import com.sqldpass.app.data.remote.SqldpassApi
import java.util.UUID

class AppRepository(
    private val api: SqldpassApi,
    private val database: SqldpassDatabase,
    private val tokenStore: TokenStore,
) {
    private val dao = database.offlineDao()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val answersAdapter = moshi.adapter<List<SolveAnswerRequest>>(
        Types.newParameterizedType(List::class.java, SolveAnswerRequest::class.java),
    )

    suspend fun mockExams(): List<MockExamSummary> {
        return try {
            api.getMockExams()
        } catch (_: Exception) {
            dao.mockExams().map {
                MockExamSummary(
                    id = it.id,
                    name = it.name,
                    examType = it.examType,
                    sequence = it.sequence,
                    totalQuestions = it.totalQuestions,
                    difficultyLabel = null,
                    solved = false,
                    bestCorrectCount = null,
                    bestTotalCount = null,
                    visibility = it.visibility,
                    isPremium = it.visibility == "PREMIUM",
                )
            }
        }
    }

    suspend fun mockExam(id: Long): MockExamDetail {
        return try {
            api.getMockExam(id)
        } catch (_: Exception) {
            val exam = dao.mockExam(id) ?: throw IllegalStateException("다운로드된 모의고사가 없습니다.")
            val questions = dao.questionsForExam(id).map {
                MockExamQuestion(
                    id = it.id,
                    displayOrder = it.displayOrder,
                    content = it.content,
                    questionType = it.questionType,
                    subjectId = null,
                    subjectName = it.subjectName,
                )
            }
            MockExamDetail(
                id = exam.id,
                name = exam.name,
                examType = exam.examType,
                sequence = exam.sequence,
                totalQuestions = questions.size,
                questions = questions,
            )
        }
    }

    suspend fun syncContent(): SyncResult {
        val cachedEtag = dao.metadata("etag")
        val response = api.getMobileSnapshot(cachedEtag)
        if (response.code() == 304) {
            return SyncResult(false, dao.mockExams().size, 0)
        }
        val body = response.body() ?: error("콘텐츠 동기화 응답이 비어 있습니다.")
        dao.clearMockExams()
        dao.clearQuestions()
        dao.upsertMockExams(body.mockExams.map {
            MockExamEntity(
                id = it.id,
                name = it.name,
                examType = it.examType,
                sequence = it.sequence,
                visibility = it.visibility,
                kind = it.kind,
                examYear = it.examYear,
                examRound = it.examRound,
                examDate = it.examDate,
                totalQuestions = it.questions.size,
            )
        })
        dao.upsertQuestions(body.mockExams.flatMap { exam ->
            exam.questions.map {
                QuestionEntity(
                    id = it.id,
                    mockExamId = exam.id,
                    displayOrder = it.displayOrder ?: 0,
                    subjectName = it.subjectName,
                    content = it.content,
                    questionType = it.questionType,
                    correctOption = it.correctOption,
                    answer = it.answer,
                    explanation = it.explanation,
                )
            }
        })
        response.headers()["ETag"]?.let { dao.upsertMetadata(MetadataEntity("etag", it)) }
        dao.upsertMetadata(MetadataEntity("version", body.version))
        return SyncResult(true, body.mockExamCount, body.questionCount)
    }

    suspend fun submitMockExam(mockExamId: Long, answers: List<SolveAnswerRequest>): SolveResponse {
        val clientSubmissionId = "android-${UUID.randomUUID()}"
        val request = SolveRequest(
            mockExamId = mockExamId,
            answers = answers,
            clientSubmissionId = clientSubmissionId,
        )
        return try {
            api.submitSolve(request)
        } catch (e: Exception) {
            dao.upsertPendingSolve(
                PendingSolveEntity(
                    clientSubmissionId = clientSubmissionId,
                    mockExamId = mockExamId,
                    answersJson = answersAdapter.toJson(answers),
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            throw e
        }
    }

    suspend fun submitPractice(subjectId: Long, answers: List<SolveAnswerRequest>): SolveResponse {
        val request = SolveRequest(
            subjectId = subjectId,
            source = "MOBILE_PRACTICE",
            answers = answers,
            clientSubmissionId = "android-${UUID.randomUUID()}",
        )
        return api.submitSolve(request)
    }

    suspend fun drainPendingSolves(): Int {
        if (tokenStore.token.isNullOrBlank()) return 0
        var synced = 0
        for (pending in dao.unsyncedSolves()) {
            val answers = answersAdapter.fromJson(pending.answersJson).orEmpty()
            val response = api.submitSolve(
                SolveRequest(
                    mockExamId = pending.mockExamId,
                    answers = answers,
                    clientSubmissionId = pending.clientSubmissionId,
                ),
            )
            dao.markSolveSynced(pending.clientSubmissionId, response.id)
            synced += 1
        }
        return synced
    }

    suspend fun pastExams(certSlug: String?): List<PastExamSummary> =
        api.getPastExams(certSlug)

    suspend fun pastExam(id: Long): PastExamDetail =
        api.getPastExam(id)

    suspend fun gradePastExam(id: Long, answers: List<PastExamAnswer>): PastExamGradeResponse =
        api.gradePastExam(id, PastExamGradeRequest(answers))

    suspend fun subjects(): List<SubjectResponse> =
        api.getSubjects()

    suspend fun randomQuestions(subjectId: Long, size: Int): List<QuestionResponse> =
        api.getRandomQuestions(subjectId, size)

    suspend fun streak(): StreakResponse =
        api.getMyStreak()

    suspend fun overallAvg(): OverallAvgResponse =
        api.getOverallAvg()

    suspend fun bestScores(): List<BestScoreSummary> {
        val response = api.getBestScores()
        if (!response.isSuccessful) return emptyList()
        val body = response.body() ?: return emptyList()
        return body.mapNotNull { (key, value) ->
            val mockExamId = key.toLongOrNull() ?: return@mapNotNull null
            BestScoreSummary(
                mockExamId = mockExamId,
                correctCount = value.correctCount,
                totalCount = value.totalCount,
            )
        }
    }

    suspend fun solveDetail(id: Long): SolveResponse = api.getSolve(id)

    suspend fun subscription(): SubscriptionResponse = api.getSubscription()

    suspend fun bookmarks(): BookmarkListResponse = api.getBookmarks()

    suspend fun addBookmark(questionId: Long) {
        api.addBookmark(questionId)
    }

    suspend fun removeBookmark(questionId: Long) {
        api.removeBookmark(questionId)
    }

    suspend fun isBookmarked(questionId: Long): Boolean =
        runCatching { api.bookmarkExists(questionId).exists }.getOrDefault(false)

    suspend fun reportFeedback(type: String, questionId: Long?, content: String, pageUrl: String? = null) {
        api.createFeedback(
            CreateFeedbackRequest(
                type = type,
                questionId = questionId,
                content = content,
                pageUrl = pageUrl,
            ),
        )
    }

    suspend fun wrongAnswers(subjectId: Long? = null): List<WrongAnswerSummary> =
        api.getWrongAnswers(subjectId)

    suspend fun wrongAnswerStats(): List<WrongAnswerStatsSummary> = api.getWrongAnswerStats()

    suspend fun updateNickname(nickname: String): MemberMeResponse =
        api.updateNickname(UpdateNicknameRequest(nickname))

    suspend fun me(): MemberMeResponse = api.getMe()

    suspend fun myDailyCounts(days: Int = 14): List<DailyCountResponse> =
        api.getMyDailyCounts(days)
}

data class SyncResult(
    val updated: Boolean,
    val mockExamCount: Int,
    val questionCount: Int,
)

data class BestScoreSummary(
    val mockExamId: Long,
    val correctCount: Int,
    val totalCount: Int,
)
