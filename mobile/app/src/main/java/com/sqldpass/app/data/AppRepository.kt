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
import com.squareup.moshi.JsonClass
import java.util.UUID
import retrofit2.Response

/**
 * 단일 채점 풀이를 PendingSolveEntity 에 담을 때 mockExamId 컬럼에 들어가는 sentinel.
 * 실제 모의고사 id 는 양수만 사용되므로 -1L 충돌 없음. drain 시 이 sentinel 을 보고 분기.
 */
const val SOLO_PENDING_MOCK_EXAM_SENTINEL: Long = -1L

/**
 * Solo 모드 PendingSolve 의 answersJson 직렬화 wrapper.
 * 기존 모의고사 row 의 answersJson 은 List<SolveAnswerRequest> 직접 직렬화. drain 시
 * mockExamId 가 sentinel 이면 본 wrapper 로 디코드.
 */
@JsonClass(generateAdapter = true)
data class SoloPendingPayload(
    val subjectId: Long,
    val answers: List<SolveAnswerRequest>,
)

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
    private val soloPayloadAdapter = moshi.adapter(SoloPendingPayload::class.java)

    /** SoloSolveScreen 의 오프라인 인디케이터용 — 미동기화 큐 카운트 Flow. */
    fun pendingSolveCountFlow(): kotlinx.coroutines.flow.Flow<Int> = dao.unsyncedSolveCountFlow()

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
        response.requireSuccessful("콘텐츠 동기화")
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
            answers = answers,
            clientSubmissionId = "android-${UUID.randomUUID()}",
        )
        return api.submitSolve(request)
    }

    /**
     * 미동기화 PendingSolve 들을 createdAtMillis ASC 순으로 서버에 재전송.
     *
     * - mockExamId 가 [SOLO_PENDING_MOCK_EXAM_SENTINEL] 이면 SoloPendingPayload 로 디코드하여
     *   subjectId 기반 NORMAL 풀이로 제출.
     * - 그 외(양수) 는 기존 모의고사 row 로 처리.
     * - 개별 row 가 실패하면 다음 row 로 (네트워크 일시 오류일 수 있음).
     * - 멱등키 `clientSubmissionId` 로 서버는 같은 row 가 두 번 와도 중복 row 생성 X.
     */
    suspend fun drainPendingSolves(): Int {
        if (tokenStore.token.isNullOrBlank()) return 0
        var synced = 0
        for (pending in dao.unsyncedSolves()) {
            try {
                val response = if (pending.mockExamId == SOLO_PENDING_MOCK_EXAM_SENTINEL) {
                    val payload = soloPayloadAdapter.fromJson(pending.answersJson) ?: continue
                    api.submitSolve(
                        SolveRequest(
                            subjectId = payload.subjectId,
                            answers = payload.answers,
                            clientSubmissionId = pending.clientSubmissionId,
                        ),
                    )
                } else {
                    val answers = answersAdapter.fromJson(pending.answersJson).orEmpty()
                    api.submitSolve(
                        SolveRequest(
                            mockExamId = pending.mockExamId,
                            answers = answers,
                            clientSubmissionId = pending.clientSubmissionId,
                        ),
                    )
                }
                dao.markSolveSynced(pending.clientSubmissionId, response.id)
                synced += 1
            } catch (_: Exception) {
                // 다음 row 시도 — 영구 실패 처리는 별 phase.
                continue
            }
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

    suspend fun questionDetail(id: Long): QuestionDetailResponse =
        api.getQuestionDetail(id)

    /**
     * 단일 채점 풀이의 1문제 제출. subjectId 기반 NORMAL 풀이로 저장한다.
     *
     * 네트워크 실패 시 PendingSolveEntity 에 enqueue 한 뒤 throw — 호출부(ViewModel) 는
     * 실패를 정보로만 받고 다음 문제로 진행 가능. 복귀 시 [drainPendingSolves] 가 sentinel
     * 분기로 자동 sync. 멱등키 `clientSubmissionId` 가 서버 측 중복을 막는다.
     */
    suspend fun submitSoloAnswer(
        subjectId: Long,
        questionId: Long,
        selectedOption: Int?,
        answerText: String?,
    ): SolveResponse {
        val clientSubmissionId = "android-${UUID.randomUUID()}"
        val answers = listOf(SolveAnswerRequest(questionId, selectedOption, answerText))
        val request = SolveRequest(
            subjectId = subjectId,
            answers = answers,
            clientSubmissionId = clientSubmissionId,
        )
        return try {
            api.submitSolve(request)
        } catch (e: Exception) {
            dao.upsertPendingSolve(
                PendingSolveEntity(
                    clientSubmissionId = clientSubmissionId,
                    mockExamId = SOLO_PENDING_MOCK_EXAM_SENTINEL,
                    answersJson = soloPayloadAdapter.toJson(
                        SoloPendingPayload(subjectId = subjectId, answers = answers),
                    ),
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            throw e
        }
    }

    suspend fun submitCollectedAnswers(answers: List<SolveAnswerRequest>): SolveResponse {
        val request = SolveRequest(
            source = "BOOKMARK",
            answers = answers,
            clientSubmissionId = "android-${UUID.randomUUID()}",
        )
        return api.submitSolve(request)
    }

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

    /** GET /api/solves — HistoryScreen 풀이기록 목록. */
    suspend fun mySolves(): List<SolveSummary> = api.getMySolves()

    suspend fun subscription(): SubscriptionResponse = api.getSubscription()

    suspend fun paymentEligible(): Boolean = api.getPaymentEligibility().eligible

    suspend fun bookmarks(): BookmarkListResponse = api.getBookmarks()

    suspend fun addBookmark(questionId: Long) {
        api.addBookmark(questionId).requireSuccessful("즐겨찾기 추가")
    }

    suspend fun removeBookmark(questionId: Long) {
        api.removeBookmark(questionId).requireSuccessful("즐겨찾기 해제")
    }

    suspend fun isBookmarked(questionId: Long): Boolean =
        runCatching { api.bookmarkExists(questionId).exists }.getOrDefault(false)

    suspend fun reportFeedback(type: String, questionId: Long?, content: String, pageUrl: String? = null) {
        api.createFeedback(
            CreateFeedbackRequest(
                type = normalizeFeedbackType(type),
                questionId = questionId,
                content = content,
                pageUrl = pageUrl,
            ),
        ).requireSuccessful("피드백 전송")
    }

    suspend fun wrongAnswers(subjectId: Long? = null): List<WrongAnswerSummary> =
        api.getWrongAnswers(subjectId)

    suspend fun wrongAnswerStats(): List<WrongAnswerStatsSummary> = api.getWrongAnswerStats()

    suspend fun updateNickname(nickname: String): MemberMeResponse =
        api.updateNickname(UpdateNicknameRequest(nickname))

    suspend fun me(): MemberMeResponse = api.getMe()

    suspend fun deleteAccount() {
        api.deleteMe().requireSuccessful("계정 삭제")
    }

    suspend fun myDailyCounts(days: Int = 14): List<DailyCountResponse> =
        api.getMyDailyCounts(days)

    private fun normalizeFeedbackType(type: String): String = when (type) {
        "QUESTION_ERROR",
        "WRONG_ANSWER",
        "TYPO",
        "UNCLEAR",
        "OUT_OF_SCOPE",
        "QUESTION_REPORT"
        -> "QUESTION_ERROR"
        "BUG" -> "BUG"
        "FEATURE", "GENERAL" -> "FEATURE"
        else -> "OTHER"
    }

    private fun <T> Response<T>.requireSuccessful(actionLabel: String): T? {
        if (!isSuccessful) {
            val detail = errorBody()?.string()?.takeIf { it.isNotBlank() }
            error("$actionLabel 실패: HTTP ${code()}${detail?.let { " - $it" } ?: ""}")
        }
        return body()
    }
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
