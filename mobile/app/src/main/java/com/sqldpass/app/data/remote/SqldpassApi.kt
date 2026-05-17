package com.sqldpass.app.data.remote

import com.sqldpass.app.data.BestScoreEntry
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
import com.sqldpass.app.data.VerifyPaymentResponse
import com.sqldpass.app.data.VerifyPlayBillingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
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

    @POST("api/payment/play-billing/verify")
    suspend fun verifyPlayBilling(@Body body: VerifyPlayBillingRequest): VerifyPaymentResponse

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

    @GET("api/mock-exams/best-scores")
    suspend fun getBestScores(): Response<Map<String, BestScoreEntry>>
}
