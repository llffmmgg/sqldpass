package com.sqldpass.app.data.remote

import com.sqldpass.app.data.QuotaExceededException
import com.sqldpass.app.data.QuotaInfo
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HTTP 402 + body `{ error, used, limit, resetAt }` 응답을 감지해 전역 콜백으로 전달.
 *
 * 백엔드 `DailyQuotaPolicy` 가 무료 회원의 일일 한도 초과 시 402 로 리턴한다.
 * 이 인터셉터는:
 *
 * 1. 본문을 [okhttp3.Response.peekBody] 로 비파괴적으로 읽어 [QuotaInfo] 로 파싱.
 * 2. [onQuotaExceeded] 콜백으로 [com.sqldpass.app.ui.AppViewModel] 에 전달해
 *    전역 Bottom Sheet 가 떠오르도록 한다.
 * 3. 파싱이 성공했을 때만 [QuotaExceededException] 으로 throw — Retrofit 호출부가
 *    원래 받아야 할 비즈니스 응답(예: List<MockExamSummary>)이 아니므로 평소 catch 흐름과 호환.
 *
 * **금지**: 자체 카운팅. 본 클라이언트는 표시만, 한도 판정은 서버.
 * **비변경**: 기존 TokenAuthenticator(401) 동작을 건드리지 않는다.
 */
class QuotaInterceptor(
    moshi: Moshi,
    private val onQuotaExceeded: (QuotaInfo) -> Unit,
) : Interceptor {
    private val adapter = moshi.adapter(QuotaInfo::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != 402) return response

        val body = runCatching { response.peekBody(MAX_PEEK_BYTES).string() }
            .getOrNull()
            ?: return response

        val info = runCatching { adapter.fromJson(body) }.getOrNull() ?: return response
        // 콜백 (UI 전역 상태 mutate) 은 main dispatcher 가 아니라 OkHttp dispatcher 에서 호출되지만,
        // AppViewModel 의 MutableStateFlow 는 thread-safe 라 문제 없다.
        onQuotaExceeded(info)
        // 호출부 suspend 함수에 RuntimeException 으로 전파 — runCatching 등 표준 흐름에서 잡힘.
        throw QuotaExceededException(info)
    }

    private companion object {
        // 응답 본문은 작아야 함 (수십 바이트). 안전 상한 4KB.
        const val MAX_PEEK_BYTES = 4 * 1024L
    }
}
