package com.sqldpass.app.data.remote

import com.sqldpass.app.data.TokenRefreshResponse
import retrofit2.http.POST

/**
 * 401 자동 재발급용 전용 인터페이스.
 *
 * 별도 Retrofit/OkHttp 클라이언트(Authenticator 미부착) 위에서만 호출해야 한다.
 * 그렇지 않으면 refresh 자체가 401 을 반환했을 때 [com.sqldpass.app.auth.TokenAuthenticator] 가
 * 무한 루프로 진입한다.
 */
interface AuthRefreshApi {
    @POST("api/auth/refresh")
    suspend fun refresh(): TokenRefreshResponse
}
