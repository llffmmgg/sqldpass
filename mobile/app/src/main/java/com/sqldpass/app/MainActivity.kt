package com.sqldpass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.AppViewModel
import com.sqldpass.app.ui.AppViewModelFactory
import com.sqldpass.app.ui.dashboard.DashboardTab
import com.sqldpass.app.ui.home.HomeScreen
import com.sqldpass.app.ui.mockexam.MockExamTab
import com.sqldpass.app.ui.passplus.PassPlusCatalogScreen
import com.sqldpass.app.ui.pastexam.PastExamTab
import com.sqldpass.app.ui.solve.SolveTab
import com.sqldpass.app.ui.theme.SqldpassTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app: SqldpassApplication
        get() = application as SqldpassApplication

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(app.repository, app.tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // AndroidX SplashScreen 폴리필 — API 31+ 는 시스템 처리, 그 이하는 라이브러리.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        app.billingManager.connect()
        setContent {
            SqldpassTheme {
                val state by viewModel.state.collectAsState()
                val scope = rememberCoroutineScope()
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    scope.launch {
                        try {
                            app.authManager.handleSignInResult(result.data)
                            viewModel.onAuthChanged()
                        } catch (e: com.google.android.gms.common.api.ApiException) {
                            viewModel.setMessage(authErrorMessage(e.statusCode))
                        } catch (e: Exception) {
                            viewModel.setMessage("Google 로그인 실패: ${e.message ?: "알 수 없는 오류"}")
                        }
                    }
                }
                SqldpassApp(
                    state = state,
                    viewModel = viewModel,
                    onLogin = { launcher.launch(app.authManager.signInIntent()) },
                    onLogout = {
                        app.authManager.signOut(this)
                        viewModel.onAuthChanged()
                    },
                    onPurchase = { productId -> app.billingManager.launch(this, productId) },
                    productSnapshot = { app.billingManager.productSnapshot() },
                    onLoadProducts = { scope.launch { app.billingManager.loadProducts() } },
                )
            }
        }
    }
}

private fun authErrorMessage(statusCode: Int): String = when (statusCode) {
    12501 -> "로그인이 취소됐습니다."
    7 -> "네트워크 연결을 확인해주세요."
    10 -> "Google 로그인 설정 오류 (DEVELOPER_ERROR). 키 등록을 확인하세요."
    12500 -> "Google 로그인에 실패했습니다 (SIGN_IN_FAILED)."
    12502 -> "이전 로그인이 아직 진행 중입니다."
    else -> "Google 로그인 실패 (code $statusCode)"
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("홈", Icons.Outlined.Home),
    Tab("모의고사", Icons.Outlined.Quiz),
    Tab("기출복원", Icons.Outlined.History),
    Tab("문제풀기", Icons.Outlined.EditNote),
    Tab("대시보드", Icons.Outlined.BarChart),
)

@Composable
private fun SqldpassApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onPurchase: (String) -> Unit,
    productSnapshot: () -> List<com.sqldpass.app.billing.BillingProductSnapshot>,
    onLoadProducts: () -> Unit,
) {
    if (state.passplusOpen) {
        PassPlusCatalogScreen(
            subscription = state.subscription,
            products = productSnapshot(),
            onLoadProducts = onLoadProducts,
            onLoadSubscription = viewModel::loadSubscription,
            onPurchase = { id ->
                onPurchase(id)
                viewModel.closePassPlus()
            },
            onClose = viewModel::closePassPlus,
        )
        return
    }
    // 오답 모아풀기는 탭 외부 풀스크린으로 동작
    val isWrongAnswers = state.runner?.mode == com.sqldpass.app.ui.runner.RunnerMode.WRONG_ANSWERS ||
        (state.runnerResult is com.sqldpass.app.ui.runner.RunnerResult.Solve &&
            (state.runnerResult as com.sqldpass.app.ui.runner.RunnerResult.Solve).mode ==
            com.sqldpass.app.ui.runner.RunnerMode.WRONG_ANSWERS)
    if (isWrongAnswers) {
        com.sqldpass.app.ui.runner.RunnerHost(
            state = state,
            mode = com.sqldpass.app.ui.runner.RunnerMode.WRONG_ANSWERS,
            onSubmitAnswers = viewModel::submitRunner,
            onCancelRunner = viewModel::cancelRunner,
            onDismissResult = viewModel::dismissResult,
            onToggleBookmark = viewModel::toggleBookmark,
            onReport = viewModel::submitFeedback,
        ) { /* fallback 없음 — 진입 시 항상 runner 또는 result */ }
        return
    }
    var selected by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            when (selected) {
                0 -> HomeScreen(
                    nickname = state.nickname,
                    message = state.message,
                    onLogin = onLogin,
                    onLogout = onLogout,
                    onSync = viewModel::sync,
                    onPurchase = { viewModel.openPassPlus() },
                )
                1 -> MockExamTab(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onStartExam = viewModel::startMockExamRunner,
                    onSubmitAnswers = viewModel::submitRunner,
                    onCancelRunner = viewModel::cancelRunner,
                    onDismissResult = viewModel::dismissResult,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onReport = viewModel::submitFeedback,
                )
                2 -> PastExamTab(
                    state = state,
                    onSelectCert = viewModel::selectCertSlug,
                    onStartExam = viewModel::startPastExamRunner,
                    onSubmitAnswers = viewModel::submitRunner,
                    onCancelRunner = viewModel::cancelRunner,
                    onDismissResult = viewModel::dismissResult,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onReport = viewModel::submitFeedback,
                )
                3 -> SolveTab(
                    state = state,
                    onLoadSubjects = viewModel::loadSubjects,
                    onStartPractice = viewModel::startPracticeRunner,
                    onSubmitAnswers = viewModel::submitRunner,
                    onCancelRunner = viewModel::cancelRunner,
                    onDismissResult = viewModel::dismissResult,
                    onNextSet = viewModel::restartLastPractice,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onReport = viewModel::submitFeedback,
                )
                else -> DashboardTab(
                    state = state,
                    onLoadDashboard = viewModel::loadDashboard,
                    onLogin = onLogin,
                    onLogout = onLogout,
                    onPurchase = { viewModel.openPassPlus() },
                    onLoadWrongStats = viewModel::loadWrongAnswerStats,
                    onStartWrongAnswers = viewModel::startWrongAnswerRunner,
                    onUpdateNickname = viewModel::updateNickname,
                )
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
