package com.sqldpass.app.data

import com.squareup.moshi.JsonClass

/**
 * 무료 회원 일일 한도 응답 body — 백엔드 `DailyQuotaPolicy` 가 HTTP 402 와 함께 내려준다.
 *
 * - [code]: `DAILY_QUESTION_LIMIT` | `DAILY_MOCK_LIMIT`
 * - [used]/[limit]: 오늘 사용량 / 일일 상한
 * - [resetAt]: KST naive ISO ("2026-05-22T00:00:00")
 *
 * **카운팅 로직 없음 — 본 클라이언트는 표시만.** 서버 단일 진실 소스.
 */
@JsonClass(generateAdapter = true)
data class QuotaInfo(
    val error: String,
    val used: Int,
    val limit: Int,
    val resetAt: String,
) {
    val code: String get() = error
}

/**
 * QuotaInterceptor 가 HTTP 402 응답에서 파싱한 [QuotaInfo] 를 들고 던지는 예외.
 *
 * - Retrofit suspend 호출 측엔 코틀린 예외 형태로 전파되어 호출부 try/catch 로 잡힐 수 있지만,
 *   본 phase 의 정책상 호출부에 try/catch 를 분산시키지 않는다. 대신 인터셉터가 동시에
 *   `AppViewModel.showQuotaPaywall` 콜백을 호출해 전역 Bottom Sheet 가 떠오른다.
 * - 회귀 방지: 401 의 TokenAuthenticator 흐름과는 별개 — 본 예외는 재시도를 유발하지 않는다.
 */
class QuotaExceededException(val info: QuotaInfo) : RuntimeException(info.code)
