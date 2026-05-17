package com.sqldpass.app.data.remote

import com.sqldpass.app.data.BestScoreEntry
import com.sqldpass.app.data.BookmarkExistsResponse
import com.sqldpass.app.data.BookmarkListResponse
import com.sqldpass.app.data.CreateFeedbackRequest
import com.sqldpass.app.data.MemberMeResponse
import com.sqldpass.app.data.MockExamDetail
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.data.OAuthLoginRequest
import com.sqldpass.app.data.OAuthLoginResponse
import com.sqldpass.app.data.OverallAvgResponse
import com.sqldpass.app.data.PastExamDetail
import com.sqldpass.app.data.PastExamGradeRequest
import com.sqldpass.app.data.PastExamGradeResponse
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.data.QuestionResponse
import com.sqldpass.app.data.SnapshotResponse
import com.sqldpass.app.data.SolveRequest
import com.sqldpass.app.data.SolveResponse
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.data.SubjectResponse
import com.sqldpass.app.data.SubscriptionResponse
import com.sqldpass.app.data.UpdateNicknameRequest
import com.sqldpass.app.data.VerifyPaymentResponse
import com.sqldpass.app.data.VerifyPlayBillingRequest
import com.sqldpass.app.data.WrongAnswerStatsSummary
import com.sqldpass.app.data.WrongAnswerSummary
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SqldpassApi {
    @POST("api/auth/login/google/idtoken")
    suspend fun loginWithGoogleIdToken(@Body body: OAuthLoginRequest): OAuthLoginResponse

    @GET("api/mock-exams")
    suspend fun getMockExams(): List<MockExamSummary>

    @GET("api/mock-exams/{id}")
    suspend fun getMockExam(@Path("id") id: Long): MockExamDetail

    @GET("api/mobile/content/snapshot")
    suspend fun getMobileSnapshot(@Header("If-None-Match") etag: String?): Response<SnapshotResponse>

    @POST("api/solves")
    suspend fun submitSolve(@Body body: SolveRequest): SolveResponse

    @GET("api/solves/{id}")
    suspend fun getSolve(@Path("id") id: Long): SolveResponse

    @POST("api/payment/play-billing/verify")
    suspend fun verifyPlayBilling(@Body body: VerifyPlayBillingRequest): VerifyPaymentResponse

    @GET("api/payment/subscription")
    suspend fun getSubscription(): SubscriptionResponse

    @GET("api/public/past-exams")
    suspend fun getPastExams(@Query("cert") certSlug: String?): List<PastExamSummary>

    @GET("api/public/past-exams/{id}")
    suspend fun getPastExam(@Path("id") id: Long): PastExamDetail

    @POST("api/public/past-exams/{id}/grade")
    suspend fun gradePastExam(
        @Path("id") id: Long,
        @Body body: PastExamGradeRequest,
    ): PastExamGradeResponse

    @GET("api/subjects")
    suspend fun getSubjects(): List<SubjectResponse>

    @GET("api/questions")
    suspend fun getRandomQuestions(
        @Query("subjectId") subjectId: Long,
        @Query("size") size: Int,
    ): List<QuestionResponse>

    @GET("api/streak/me")
    suspend fun getMyStreak(): StreakResponse

    @GET("api/solves/stats/overall-avg")
    suspend fun getOverallAvg(): OverallAvgResponse

    @GET("api/solves/me/daily")
    suspend fun getMyDailyCounts(@Query("days") days: Int = 14): List<com.sqldpass.app.data.DailyCountResponse>

    @GET("api/mock-exams/best-scores")
    suspend fun getBestScores(): Response<Map<String, BestScoreEntry>>

    // Bookmarks
    @GET("api/bookmarks")
    suspend fun getBookmarks(): BookmarkListResponse

    @POST("api/bookmarks/{questionId}")
    suspend fun addBookmark(@Path("questionId") questionId: Long): Response<Unit>

    @DELETE("api/bookmarks/{questionId}")
    suspend fun removeBookmark(@Path("questionId") questionId: Long): Response<Unit>

    @GET("api/bookmarks/exists/{questionId}")
    suspend fun bookmarkExists(@Path("questionId") questionId: Long): BookmarkExistsResponse

    // Feedback (report)
    @POST("api/feedback")
    suspend fun createFeedback(@Body body: CreateFeedbackRequest): Response<Unit>

    // Wrong answers
    @GET("api/wrong-answers")
    suspend fun getWrongAnswers(@Query("subjectId") subjectId: Long?): List<WrongAnswerSummary>

    @GET("api/wrong-answers/stats")
    suspend fun getWrongAnswerStats(): List<WrongAnswerStatsSummary>

    // Member
    @PATCH("api/members/me/nickname")
    suspend fun updateNickname(@Body body: UpdateNicknameRequest): MemberMeResponse

    @GET("api/members/me")
    suspend fun getMe(): MemberMeResponse
}
