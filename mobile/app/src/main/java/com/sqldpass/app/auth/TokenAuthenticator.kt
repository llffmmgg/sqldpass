package com.sqldpass.app.auth

import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.data.remote.AuthRefreshApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401 응답에 대해 한 번만 토큰 재발급을 시도하는 OkHttp Authenticator.
 *
 * iOS [APIClient + AuthStore.refresh] 의 single-flight 패턴과 동치.
 *
 * 동작:
 * - 원 요청에 Authorization 헤더가 없었으면 (예: 로그인 요청) 재시도하지 않는다.
 * - 동시에 여러 요청이 401 을 만나도 [mutex] 로 단일 비행(single-flight) 보장.
 *   다른 스레드가 이미 새 토큰을 받아 [TokenStore] 에 저장했다면 그 토큰으로 재시도.
 * - 재발급 호출 자체가 실패(예외 또는 빈 토큰)하면 [tokenStore] 를 비우고
 *   [onSessionLost] 를 호출해 UI 에 세션 만료를 알린다.
 * - 무한 루프 방지: 이미 한 번 재발급한 요청이 또 401 을 받으면 더 시도하지 않는다.
 *
 * **주의:** [refreshApiProvider] 가 가리키는 Retrofit 인터페이스는 본 Authenticator 가 붙지 않은
 * 별도 OkHttpClient 위에서 동작해야 한다. 그렇지 않으면 refresh 자체의 401 응답이 다시
 * authenticate() 를 호출하는 재귀가 발생한다.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApiProvider: () -> AuthRefreshApi,
    private val onSessionLost: () -> Unit,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val originalToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        if (priorResponseCount(response) >= 1) {
            tokenStore.clear()
            onSessionLost()
            return null
        }

        return runBlocking {
            mutex.withLock {
                val current = tokenStore.token
                if (!current.isNullOrBlank() && current != originalToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }

                val newToken = try {
                    val refreshed = refreshApiProvider().refresh()
                    refreshed.nickname?.let { tokenStore.nickname = it }
                    refreshed.token
                } catch (_: Exception) {
                    null
                }

                if (newToken.isNullOrBlank()) {
                    tokenStore.clear()
                    onSessionLost()
                    null
                } else {
                    tokenStore.token = newToken
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                }
            }
        }
    }

    private fun priorResponseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 0
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
