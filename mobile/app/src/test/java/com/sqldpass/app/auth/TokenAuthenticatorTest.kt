package com.sqldpass.app.auth

import com.sqldpass.app.data.TokenRefreshResponse
import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.data.remote.AuthRefreshApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * MockWebServer 를 통해 실제 OkHttp Authenticator 와 통합 동작을 검증한다.
 * TokenStore / AuthRefreshApi 는 MockK 로 모킹.
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer

    @MockK
    private lateinit var tokenStore: TokenStore

    @MockK
    private lateinit var refreshApi: AuthRefreshApi

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /**
     * Authorization 헤더를 자동으로 붙여주는 인터셉터. TokenStore.token 의 현재 값을 매 요청마다 읽음.
     * (실제 앱의 AuthInterceptor 와 동치)
     */
    private fun bearerInterceptor(): Interceptor = Interceptor { chain ->
        val token = tokenStore.token
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private fun buildClient(
        onSessionLost: () -> Unit,
    ): OkHttpClient {
        val authenticator = TokenAuthenticator(
            tokenStore = tokenStore,
            refreshApiProvider = { refreshApi },
            onSessionLost = onSessionLost,
        )
        return OkHttpClient.Builder()
            .addInterceptor(bearerInterceptor())
            .authenticator(authenticator)
            .build()
    }

    @Test
    fun firstUnauthorizedRefreshesAndRetriesWithNewToken() {
        // tokenStore.token 은 처음 "old" → refresh 후 "new"
        val tokenSlot = AtomicInteger(0)
        every { tokenStore.token } answers {
            if (tokenSlot.get() == 0) "old" else "new"
        }
        every { tokenStore.token = "new" } answers {
            tokenSlot.set(1)
        }
        every { tokenStore.nickname = any() } just Runs
        coEvery { refreshApi.refresh() } returns TokenRefreshResponse(token = "new", nickname = "nick")

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val client = buildClient(onSessionLost = { fail("onSessionLost should not be called") })
        val response = client.newCall(Request.Builder().url(server.url("/me")).get().build()).execute()

        assertEquals(200, response.code)
        response.close()
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals("Bearer old", first.getHeader("Authorization"))
        assertEquals("Bearer new", second.getHeader("Authorization"))
        verify(exactly = 1) { tokenStore.token = "new" }
    }

    @Test
    fun secondUnauthorizedAfterRefreshClearsTokenAndCallsSessionLost() {
        every { tokenStore.token } returns "old"
        every { tokenStore.token = any() } just Runs
        every { tokenStore.nickname = any() } just Runs
        every { tokenStore.clear() } just Runs
        coEvery { refreshApi.refresh() } returns TokenRefreshResponse(token = "new", nickname = null)

        // 첫 401 → refresh → retry 401 → 여기서 priorResponseCount>=1 분기로 진입.
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        var sessionLost = 0
        val client = buildClient(onSessionLost = { sessionLost++ })
        val response = client.newCall(Request.Builder().url(server.url("/me")).get().build()).execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(1, sessionLost)
        verify(atLeast = 1) { tokenStore.clear() }
    }

    @Test
    fun missingAuthorizationHeaderReturnsNullAndDoesNotRefresh() {
        // Authenticator.authenticate() 를 직접 호출 (Authorization 헤더 없는 Response 로).
        every { tokenStore.token } returns null
        val authenticator = TokenAuthenticator(
            tokenStore = tokenStore,
            refreshApiProvider = { refreshApi },
            onSessionLost = { fail("session lost should not fire") },
        )

        val request = Request.Builder().url("http://localhost/").get().build()
        val response = Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val result = authenticator.authenticate(null, response)
        assertNull(result)
        verify(exactly = 0) { tokenStore.clear() }
    }

    @Test
    fun refreshThrowingClearsTokenAndFiresSessionLost() {
        every { tokenStore.token } returns "old"
        every { tokenStore.token = any() } just Runs
        every { tokenStore.clear() } just Runs
        coEvery { refreshApi.refresh() } throws IOException("network")

        server.enqueue(MockResponse().setResponseCode(401))

        var sessionLost = 0
        val client = buildClient(onSessionLost = { sessionLost++ })
        val response = client.newCall(Request.Builder().url(server.url("/me")).get().build()).execute()

        // refresh 실패 → null 반환 → OkHttp 가 원 401 그대로 반환.
        assertEquals(401, response.code)
        response.close()
        assertEquals(1, sessionLost)
        verify(atLeast = 1) { tokenStore.clear() }
    }

    @Test
    fun singleFlightRefreshOnConcurrentUnauthorized() = runBlocking {
        val refreshCount = AtomicInteger(0)
        val tokenState = AtomicInteger(0) // 0=old, 1=new
        every { tokenStore.token } answers {
            if (tokenState.get() == 0) "old" else "new"
        }
        every { tokenStore.token = "new" } answers {
            tokenState.set(1)
        }
        every { tokenStore.nickname = any() } just Runs
        coEvery { refreshApi.refresh() } answers {
            refreshCount.incrementAndGet()
            // 약간의 지연으로 두 번째 코루틴이 mutex 에서 대기하도록 유도.
            Thread.sleep(80)
            TokenRefreshResponse(token = "new", nickname = null)
        }

        // 두 요청 모두 첫 응답은 401, 재시도는 200.
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("a"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("b"))

        val client = buildClient(onSessionLost = { fail("not expected") })

        val a = async { client.newCall(Request.Builder().url(server.url("/a")).get().build()).execute() }
        val b = async { client.newCall(Request.Builder().url(server.url("/b")).get().build()).execute() }
        val ra = a.await()
        val rb = b.await()

        assertEquals(200, ra.code)
        assertEquals(200, rb.code)
        ra.close()
        rb.close()

        // single-flight: refreshApi 는 정확히 1번만 호출돼야 함.
        assertEquals(1, refreshCount.get())
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
