package com.sqldpass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sqldpass.app.data.ThemeMode
import com.sqldpass.app.nav.BOTTOM_TABS
import com.sqldpass.app.nav.SqldpassRoute
import com.sqldpass.app.nav.isBottomTabRoute
import com.sqldpass.app.nav.pushSlideEnter
import com.sqldpass.app.nav.pushSlideExitForward
import com.sqldpass.app.nav.pushSlidePopExit
import com.sqldpass.app.nav.tabFadeEnter
import com.sqldpass.app.nav.tabFadeExit
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.AppViewModel
import com.sqldpass.app.ui.AppViewModelFactory
import com.sqldpass.app.ui.common.TabScaffold
import com.sqldpass.app.ui.dashboard.DashboardTab
import com.sqldpass.app.ui.home.HomeScreen
import com.sqldpass.app.ui.mockexam.MockExamTab
import com.sqldpass.app.ui.passplus.PassPlusCatalogScreen
import com.sqldpass.app.ui.pastexam.PastExamTab
import com.sqldpass.app.ui.runner.RunnerScreen
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
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Play Billing 연결은 IPC 대기가 100~500ms 메인 스레드 블록 가능 — 백그라운드로.
        lifecycleScope.launch { app.billingManager.connect() }
        setContent {
            val themeMode by app.settingsStore.themeMode.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            SqldpassTheme(darkTheme = darkTheme) {
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
                    themeMode = themeMode,
                    onThemeChange = app.settingsStore::setThemeMode,
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

@Composable
private fun SqldpassApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onPurchase: (String) -> Unit,
    productSnapshot: () -> List<com.sqldpass.app.billing.BillingProductSnapshot>,
    onLoadProducts: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showNavigationSuite = isBottomTabRoute(currentRoute)

    val appContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            NavHost(
                navController = navController,
                startDestination = SqldpassRoute.Home.route,
            ) {
                composable(
                    SqldpassRoute.Home.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(
                        title = "문어CBT",
                        topBar = false,
                    ) {
                        HomeScreen(
                            nickname = state.nickname,
                            message = state.message,
                            onQuickPractice = { navController.navigate(SqldpassRoute.Solve.route) },
                            onSync = viewModel::sync,
                            onPurchase = { navController.navigate(SqldpassRoute.PassPlus.route) },
                            heroActions = {
                                com.sqldpass.app.ui.home.HomeAccountMenu(
                                    nickname = state.nickname,
                                    onLogin = onLogin,
                                    onLogout = onLogout,
                                )
                            },
                        )
                    }
                }
                composable(
                    SqldpassRoute.MockExam.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "모의고사") {
                        MockExamTab(
                            state = state,
                            onRefresh = viewModel::refresh,
                            onStartExam = { id ->
                                viewModel.startMockExamRunner(id)
                                navController.navigate(SqldpassRoute.Runner.route)
                            },
                        )
                    }
                }
                composable(
                    SqldpassRoute.PastExam.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "기출복원") {
                        PastExamTab(
                            state = state,
                            onSelectCert = viewModel::selectCertSlug,
                            onStartExam = { id, slug ->
                                viewModel.startPastExamRunner(id, slug)
                                navController.navigate(SqldpassRoute.Runner.route)
                            },
                        )
                    }
                }
                composable(
                    SqldpassRoute.Solve.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "문제풀기") {
                        SolveTab(
                            state = state,
                            onLoadSubjects = viewModel::loadSubjects,
                            onStartPractice = { id ->
                                viewModel.startPracticeRunner(id)
                                navController.navigate(SqldpassRoute.Runner.route)
                            },
                        )
                    }
                }
                composable(
                    SqldpassRoute.Dashboard.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "대시보드", topBar = false) {
                        DashboardTab(
                            state = state,
                            onLoadDashboard = viewModel::loadDashboard,
                            onLogin = onLogin,
                            onLogout = onLogout,
                            onPurchase = { navController.navigate(SqldpassRoute.PassPlus.route) },
                            onLoadWrongStats = viewModel::loadWrongAnswerStats,
                            onStartWrongAnswers = { subjectId, subjectName ->
                                viewModel.startWrongAnswerRunner(subjectId, subjectName)
                                navController.navigate(SqldpassRoute.Runner.route)
                            },
                            onUpdateNickname = viewModel::updateNickname,
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                        )
                    }
                }
                composable(
                    SqldpassRoute.PassPlus.route,
                    enterTransition = { pushSlideEnter() },
                    exitTransition = { pushSlideExitForward() },
                    popExitTransition = { pushSlidePopExit() },
                ) {
                    PassPlusCatalogScreen(
                        subscription = state.subscription,
                        products = productSnapshot(),
                        onLoadProducts = onLoadProducts,
                        onLoadSubscription = viewModel::loadSubscription,
                        onPurchase = { id ->
                            onPurchase(id)
                            navController.popBackStack()
                        },
                        onClose = { navController.popBackStack() },
                    )
                }
                composable(
                    SqldpassRoute.Runner.route,
                    enterTransition = { pushSlideEnter() },
                    exitTransition = { pushSlideExitForward() },
                    popExitTransition = { pushSlidePopExit() },
                ) {
                    RunnerScreen(
                        state = state,
                        onSubmitAnswers = viewModel::submitRunner,
                        onCancelRunner = {
                            viewModel.cancelRunner()
                            navController.popBackStack()
                        },
                        onDismissResult = {
                            viewModel.dismissResult()
                            navController.popBackStack()
                        },
                        onToggleBookmark = viewModel::toggleBookmark,
                        onReport = viewModel::submitFeedback,
                        onRestart = viewModel::restartLastPractice,
                        onExitScreen = {
                            if (navController.previousBackStackEntry != null) navController.popBackStack()
                        },
                    )
                }
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (showNavigationSuite) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                BOTTOM_TABS.forEach { tab ->
                    val selected = currentRoute == tab.route.route
                    item(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            appContent()
        }
    } else {
        appContent()
    }
}
