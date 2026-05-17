package com.sqldpass.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.sqldpass.app.data.OAuthLoginRequest
import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.data.remote.SqldpassApi
import kotlinx.coroutines.tasks.await

class AuthManager(
    context: Context,
    webClientId: String,
    private val api: SqldpassApi,
    private val tokenStore: TokenStore,
) {
    private val googleClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build(),
    )

    fun signInIntent(): Intent = googleClient.signInIntent

    suspend fun handleSignInResult(data: Intent?) {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val idToken = account.idToken ?: error("Google ID 토큰을 받을 수 없습니다.")
        val login = api.loginWithGoogleIdToken(OAuthLoginRequest(idToken))
        tokenStore.token = login.token
        tokenStore.nickname = login.nickname
    }

    fun signOut(activity: Activity) {
        tokenStore.clear()
        googleClient.signOut()
    }
}
