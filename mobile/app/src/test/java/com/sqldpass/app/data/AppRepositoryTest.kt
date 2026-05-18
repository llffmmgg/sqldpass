package com.sqldpass.app.data

import com.sqldpass.app.data.local.OfflineDao
import com.sqldpass.app.data.local.PendingSolveEntity
import com.sqldpass.app.data.local.SqldpassDatabase
import com.sqldpass.app.data.remote.SqldpassApi
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AppRepositoryTest {

    private lateinit var api: SqldpassApi
    private lateinit var database: SqldpassDatabase
    private lateinit var dao: OfflineDao
    private lateinit var tokenStore: TokenStore
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        api = mockk()
        database = mockk()
        dao = mockk()
        tokenStore = mockk()
        every { database.offlineDao() } returns dao
        repository = AppRepository(api, database, tokenStore)
    }

    private fun mockExamSolveResponse(id: Long, mockExamId: Long?): SolveResponse =
        SolveResponse(
            id = id,
            subjectId = null,
            mockExamId = mockExamId,
            totalCount = 1,
            correctCount = 1,
            score = 100,
            solvedAt = "2026-01-01T00:00:00+09:00",
            answers = emptyList(),
        )

    private fun pendingMockExamRow(
        clientSubmissionId: String,
        mockExamId: Long,
        createdAtMillis: Long,
        answersJson: String = """[{"questionId":1,"selectedOption":2,"answerText":null}]""",
    ) = PendingSolveEntity(
        clientSubmissionId = clientSubmissionId,
        mockExamId = mockExamId,
        answersJson = answersJson,
        createdAtMillis = createdAtMillis,
    )

    private fun pendingSoloRow(
        clientSubmissionId: String,
        subjectId: Long,
        createdAtMillis: Long,
    ): PendingSolveEntity {
        val json = """{"subjectId":$subjectId,"answers":[{"questionId":11,"selectedOption":1,"answerText":null}]}"""
        return PendingSolveEntity(
            clientSubmissionId = clientSubmissionId,
            mockExamId = SOLO_PENDING_MOCK_EXAM_SENTINEL,
            answersJson = json,
            createdAtMillis = createdAtMillis,
        )
    }

    @Test
    fun loggedOut_returnsZero_andDoesNotCallApi() = runBlocking {
        every { tokenStore.token } returns null
        // dao.unsyncedSolves 가 호출돼도 결과는 0 이어야 함. spec 상 호출 안 됨 — 짧은 경로.
        val result = repository.drainPendingSolves()
        assertEquals(0, result)
        coVerify(exactly = 0) { api.submitSolve(any()) }
    }

    @Test
    fun allThreeSucceed_callsApiThreeTimes_marksAllSynced_returnsThree() = runBlocking {
        every { tokenStore.token } returns "tok"
        val rows = listOf(
            pendingMockExamRow("cs-1", mockExamId = 1L, createdAtMillis = 100L),
            pendingMockExamRow("cs-2", mockExamId = 2L, createdAtMillis = 200L),
            pendingMockExamRow("cs-3", mockExamId = 3L, createdAtMillis = 300L),
        )
        coEvery { dao.unsyncedSolves() } returns rows
        coEvery { api.submitSolve(any()) } answers {
            val req = firstArg<SolveRequest>()
            mockExamSolveResponse(id = (req.mockExamId ?: 0L) * 10, mockExamId = req.mockExamId)
        }
        coEvery { dao.markSolveSynced(any(), any()) } just Runs

        val result = repository.drainPendingSolves()
        assertEquals(3, result)
        coVerify(exactly = 3) { api.submitSolve(any()) }
        coVerifyOrder {
            dao.markSolveSynced("cs-1", 10L)
            dao.markSolveSynced("cs-2", 20L)
            dao.markSolveSynced("cs-3", 30L)
        }
    }

    @Test
    fun middleRowFails_othersContinue_returnsSuccessCount() = runBlocking {
        every { tokenStore.token } returns "tok"
        val rows = listOf(
            pendingMockExamRow("cs-1", mockExamId = 1L, createdAtMillis = 100L),
            pendingMockExamRow("cs-2", mockExamId = 2L, createdAtMillis = 200L),
            pendingMockExamRow("cs-3", mockExamId = 3L, createdAtMillis = 300L),
        )
        coEvery { dao.unsyncedSolves() } returns rows
        coEvery { api.submitSolve(any()) } answers {
            val req = firstArg<SolveRequest>()
            if (req.mockExamId == 2L) throw IOException("blip")
            mockExamSolveResponse(id = (req.mockExamId ?: 0L) * 10, mockExamId = req.mockExamId)
        }
        coEvery { dao.markSolveSynced(any(), any()) } just Runs

        val result = repository.drainPendingSolves()
        assertEquals(2, result)
        coVerify(exactly = 1) { dao.markSolveSynced("cs-1", 10L) }
        coVerify(exactly = 0) { dao.markSolveSynced("cs-2", any()) }
        coVerify(exactly = 1) { dao.markSolveSynced("cs-3", 30L) }
    }

    @Test
    fun soloSentinel_decodesPayload_andSubmitsAsPractice() = runBlocking {
        every { tokenStore.token } returns "tok"
        val rows = listOf(
            pendingSoloRow("cs-solo", subjectId = 42L, createdAtMillis = 100L),
        )
        coEvery { dao.unsyncedSolves() } returns rows
        val requestSlot = slot<SolveRequest>()
        coEvery { api.submitSolve(capture(requestSlot)) } returns mockExamSolveResponse(id = 999L, mockExamId = null)
        coEvery { dao.markSolveSynced(any(), any()) } just Runs

        val result = repository.drainPendingSolves()
        assertEquals(1, result)
        val req = requestSlot.captured
        assertEquals(42L, req.subjectId)
        assertEquals(null, req.mockExamId)
        assertEquals("MOBILE_PRACTICE", req.source)
        assertEquals("cs-solo", req.clientSubmissionId)
        coVerify(exactly = 1) { dao.markSolveSynced("cs-solo", 999L) }
    }

    @Test
    fun soloPayloadInvalidJson_isSkipped_othersStillProcessed() = runBlocking {
        every { tokenStore.token } returns "tok"
        val invalid = PendingSolveEntity(
            clientSubmissionId = "cs-bad",
            mockExamId = SOLO_PENDING_MOCK_EXAM_SENTINEL,
            answersJson = "not-valid-json-at-all",
            createdAtMillis = 50L,
        )
        val rows = listOf(
            pendingMockExamRow("cs-1", mockExamId = 1L, createdAtMillis = 100L),
            invalid,
            pendingMockExamRow("cs-3", mockExamId = 3L, createdAtMillis = 300L),
        )
        coEvery { dao.unsyncedSolves() } returns rows
        coEvery { api.submitSolve(any()) } answers {
            val req = firstArg<SolveRequest>()
            mockExamSolveResponse(id = (req.mockExamId ?: 0L) * 10, mockExamId = req.mockExamId)
        }
        coEvery { dao.markSolveSynced(any(), any()) } just Runs

        val result = repository.drainPendingSolves()
        assertEquals(2, result)
        coVerify(exactly = 0) { dao.markSolveSynced("cs-bad", any()) }
        coVerify(exactly = 1) { dao.markSolveSynced("cs-1", 10L) }
        coVerify(exactly = 1) { dao.markSolveSynced("cs-3", 30L) }
    }
}

