package com.sqldpass.app

import android.app.Application
import androidx.room.Room
import com.sqldpass.app.auth.AuthManager
import com.sqldpass.app.billing.BillingManager
import com.sqldpass.app.data.AppRepository
import com.sqldpass.app.data.SettingsStore
import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.data.local.SqldpassDatabase
import com.sqldpass.app.data.remote.SqldpassApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class SqldpassApplication : Application() {
    val tokenStore: TokenStore by lazy { TokenStore(this) }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
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
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val api: SqldpassApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
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
