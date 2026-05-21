package com.sqldpass.app

import android.app.Application
import androidx.room.Room
import com.sqldpass.app.auth.AuthManager
import com.sqldpass.app.auth.TokenAuthenticator
import com.sqldpass.app.billing.BillingManager
import com.sqldpass.app.data.AppRepository
import com.sqldpass.app.data.QuotaInfo
import com.sqldpass.app.data.SettingsStore
import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.data.local.SqldpassDatabase
import com.sqldpass.app.data.remote.AuthRefreshApi
import com.sqldpass.app.data.remote.QuotaInterceptor
import com.sqldpass.app.data.remote.SqldpassApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SqldpassApplication : Application() {
    val tokenStore: TokenStore by lazy { TokenStore(this) }

    /**
     * 토큰 재발급이 실패해 강제 로그아웃됐음을 UI 에 알린다.
     * [com.sqldpass.app.MainActivity] 가 수집해 onAuthChanged 및 GoogleSignIn signOut 트리거.
     */
    private val _sessionLost = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionLost: SharedFlow<Unit> = _sessionLost.asSharedFlow()

    /**
     * 무료 일일 한도(HTTP 402) 감지 시 [QuotaInterceptor] 가 emit. ViewModel 이 collect 해
     * 전역 페이월 Bottom Sheet 상태로 변환한다.
     *
     * extraBufferCapacity = 1 — 단발성 이벤트라 버퍼링 불필요.
     */
    private val _quotaExceeded = MutableSharedFlow<QuotaInfo>(extraBufferCapacity = 1)
    val quotaExceeded: SharedFlow<QuotaInfo> = _quotaExceeded.asSharedFlow()

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    private val authHeaderInterceptor: Interceptor = Interceptor { chain ->
        val token = tokenStore.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private fun debugLogging(): HttpLoggingInterceptor? =
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        } else null

    // 재발급 호출 전용. Authenticator 미부착 — 부착하면 refresh 자체의 401 응답이
    // 다시 TokenAuthenticator 를 호출해 재귀 루프가 발생함.
    //
    // 명시 timeout: OS sleep 이후 좀비 socket 상에서 hang 되는 케이스 차단.
    // callTimeout 은 추가하지 않음 — 본 클라이언트는 refresh 자체라 retry 가 없어
    // connect/read/write 만으로 충분.
    private val refreshOkHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authHeaderInterceptor)
            .apply { debugLogging()?.let { addInterceptor(it) } }
            .build()
    }

    private val authRefreshApi: AuthRefreshApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(refreshOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthRefreshApi::class.java)
    }

    private val tokenAuthenticator: TokenAuthenticator by lazy {
        TokenAuthenticator(
            tokenStore = tokenStore,
            refreshApiProvider = { authRefreshApi },
            onSessionLost = { _sessionLost.tryEmit(Unit) },
        )
    }

    // 일일 한도(402) 인터셉터 — TokenAuthenticator(401) 동작은 건드리지 않는다.
    private val quotaInterceptor: QuotaInterceptor by lazy {
        QuotaInterceptor(
            moshi = moshi,
            onQuotaExceeded = { info -> _quotaExceeded.tryEmit(info) },
        )
    }

    // 명시 timeout: OS sleep 이후 좀비 socket hang 차단. callTimeout 은 일부러 미설정 —
    // TokenAuthenticator 의 401 재발급 후 retry 가 callTimeout 안에 끊겨버리는 회귀를 피함.
    //
    // QuotaInterceptor 는 application interceptor 로 — 401 재발급 retry 와 충돌하지 않는다
    // (401 응답이면 402 가 아니므로 quotaInterceptor 는 통과시키고, 402 응답이면 throw 라
    //  TokenAuthenticator 가 호출될 일이 없다).
    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(quotaInterceptor)
            .authenticator(tokenAuthenticator)
            .apply { debugLogging()?.let { addInterceptor(it) } }
            .build()
    }

    private val api: SqldpassApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SqldpassApi::class.java)
    }

    private val database: SqldpassDatabase by lazy {
        Room.databaseBuilder(this, SqldpassDatabase::class.java, "sqldpass.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: AppRepository by lazy {
        AppRepository(api, database, tokenStore)
    }

    val authManager: AuthManager by lazy {
        AuthManager(this, getString(R.string.google_web_client_id), api, tokenStore)
    }

    val billingManager: BillingManager by lazy {
        BillingManager(this, api)
    }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
}
