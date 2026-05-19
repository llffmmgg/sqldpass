package com.sqldpass.app

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import com.sqldpass.app.ui.common.AppLoadingIndicator
import com.sqldpass.app.ui.common.TabScaffold
import com.sqldpass.app.ui.dashboard.DashboardTab
import com.sqldpass.app.ui.home.HomeScreen
import com.sqldpass.app.ui.insights.InsightsTab
import com.sqldpass.app.ui.mockexam.MockExamTab
import com.sqldpass.app.ui.passplus.PassPlusCatalogScreen
import com.sqldpass.app.ui.pastexam.PastExamTab
import com.sqldpass.app.ui.profile.ProfileTab
import com.sqldpass.app.ui.runner.RunnerScreen
import com.sqldpass.app.ui.solve.SoloSolveScreen
import com.sqldpass.app.ui.solve.SolveTab
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme
import com.sqldpass.app.ui.bookmarks.BookmarksScreen
import com.sqldpass.app.ui.history.HistoryScreen
import com.sqldpass.app.ui.wronganswer.WrongAnswerTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app: SqldpassApplication
        get() = application as SqldpassApplication

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(app.repository, app.tokenStore)
    }

    /**
     * 네트워크 복귀 시 오프라인 큐 drain.
     * onAvailable 은 worker 스레드에서 호출 — ViewModel 의 viewModelScope.launch 가 자동 main thread 처리.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModel.tryDrainPendingSolves()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Play Billing 연결은 IPC 대기가 100~500ms 메인 스레드 블록 가능 — 백그라운드로.
        lifecycleScope.launch { app.billingManager.connect() }
        // 네트워크 복귀 감지 — 콜백 등록.
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
        // 401 재발급 실패로 세션이 끊겼을 때 ─ TokenAuthenticator 가 토큰을 이미 비웠으므로
        // 여기선 GoogleSignIn 세션과 ViewModel 상태만 정리하고 사용자에게 안내한다.
        lifecycleScope.launch {
            app.sessionLost.collect {
                app.authManager.signOut(this@MainActivity)
                viewModel.onAuthChanged()
                viewModel.setMessage("세션이 만료되어 로그아웃되었습니다. 다시 로그인해 주세요.")
            }
        }
        // Play Billing 결과 메시지 + 구독 상태 동기화.
        lifecycleScope.launch {
            app.billingManager.events.collect { event ->
                when (event) {
                    is com.sqldpass.app.billing.BillingEvent.Processing ->
                        viewModel.setMessage("결제 정보를 확인하는 중이에요…")
                    is com.sqldpass.app.billing.BillingEvent.Pending ->
                        viewModel.setMessage("결제가 대기 중이에요. 승인이 완료되면 자동으로 활성화돼요.")
                    is com.sqldpass.app.billing.BillingEvent.Canceled ->
                        viewModel.setMessage("결제가 취소되었어요.")
                    is com.sqldpass.app.billing.BillingEvent.Failed ->
                        viewModel.setMessage(event.message)
                    is com.sqldpass.app.billing.BillingEvent.Success -> {
                        viewModel.setMessage(event.warning ?: "구매가 완료됐어요. PASS+ 혜택이 활성화됐어요.")
                        // 백엔드가 entitlement 를 갱신했으므로 즉시 구독 상태 재조회.
                        viewModel.loadSubscription()
                    }
                }
            }
        }
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
                var billingProducts by remember { mutableStateOf(app.billingManager.productSnapshot()) }
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
                    onAccountDeleted = {
                        app.authManager.signOut(this)
                        viewModel.onAuthChanged()
                        viewModel.setMessage("계정이 삭제되었습니다.")
                    },
                    onPurchase = { productId ->
                        viewModel.checkPaymentEligibility { eligible ->
                            if (eligible) app.billingManager.launch(this, productId)
                        }
                    },
                    products = billingProducts,
                    onLoadProducts = {
                        scope.launch {
                            app.billingManager.loadProducts()
                            billingProducts = app.billingManager.productSnapshot()
                        }
                    },
                    onRestorePurchases = {
                        viewModel.setMessage("구매 내역을 확인하는 중입니다.")
                        app.billingManager.restorePurchases()
                        viewModel.loadSubscription()
                    },
                    themeMode = themeMode,
                    onThemeChange = app.settingsStore::setThemeMode,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSubscription()
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
    onAccountDeleted: () -> Unit,
    onPurchase: (String) -> Unit,
    products: List<com.sqldpass.app.billing.BillingProductSnapshot>,
    onLoadProducts: () -> Unit,
    onRestorePurchases: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showNavigationSuite = isBottomTabRoute(currentRoute)

    val appContent: @Composable () -> Unit = {
        val palette = LocalSqldpassPalette.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.page),
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
                        androidx.compose.runtime.LaunchedEffect(state.nickname) {
                            if (state.nickname != null && state.dashboard == null) viewModel.loadDashboard()
                        }
                        HomeScreen(
                            nickname = state.nickname,
                            message = state.message,
                            streak = state.dashboard?.streak,
                            onOpenPassPlus = { navController.navigate(SqldpassRoute.PassPlus.route) },
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
                                val name = state.subjects.firstOrNull { it.id == id }?.name
                                    ?: "랜덤 풀이"
                                viewModel.startSoloSolve(id, name)
                                navController.navigate(SqldpassRoute.SoloSolve.route)
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
                    SqldpassRoute.WrongAnswers.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "오답노트", topBar = false) {
                        WrongAnswerTab(
                            state = state,
                            onLoad = { viewModel.loadWrongAnswers() },
                            onStartSession = { items, title ->
                                viewModel.startWrongAnswersFromQuestions(items, title)
                                navController.navigate(SqldpassRoute.Runner.route)
                            },
                            onLogin = onLogin,
                        )
                    }
                }
                composable(
                    SqldpassRoute.Insights.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "인사이트", topBar = false) {
                        InsightsTab(
                            state = state,
                            onLoad = {
                                viewModel.loadDashboard()
                                viewModel.loadWrongAnswerStats()
                            },
                            onLogin = onLogin,
                        )
                    }
                }
                composable(
                    SqldpassRoute.Profile.route,
                    enterTransition = { tabFadeEnter() },
                    exitTransition = { tabFadeExit() },
                ) {
                    TabScaffold(title = "마이", topBar = false) {
                        ProfileTab(
                            state = state,
                            onLogin = onLogin,
                            onLogout = onLogout,
                            onSync = viewModel::sync,
                            onUpdateNickname = viewModel::updateNickname,
                            onSubmitFeedback = viewModel::submitFeedback,
                            onDeleteAccount = { done ->
                                viewModel.deleteAccount { ok ->
                                    if (ok) onAccountDeleted()
                                    done(ok)
                                }
                            },
                            onOpenPassPlus = { navController.navigate(SqldpassRoute.PassPlus.route) },
                            onOpenWrongAnswers = { navController.navigate(SqldpassRoute.WrongAnswers.route) },
                            onOpenBookmarks = { navController.navigate(SqldpassRoute.Bookmarks.route) },
                            onOpenHistory = { navController.navigate(SqldpassRoute.History.route) },
                            onOpenDashboard = { navController.navigate(SqldpassRoute.Dashboard.route) },
                            onOpenInsights = { navController.navigate(SqldpassRoute.Insights.route) },
                            onLoadMe = viewModel::loadMe,
                            onLoadSubscription = viewModel::loadSubscription,
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
                        products = products,
                        onLoadProducts = onLoadProducts,
                        onLoadSubscription = viewModel::loadSubscription,
                        onRestorePurchases = onRestorePurchases,
                        onPurchase = { id ->
                            onPurchase(id)
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
                composable(
                    SqldpassRoute.SoloSolve.route,
                    enterTransition = { pushSlideEnter() },
                    exitTransition = { pushSlideExitForward() },
                    popExitTransition = { pushSlidePopExit() },
                ) {
                    val pendingCount by viewModel.pendingSolveCount.collectAsState()
                    SoloSolveScreen(
                        state = state,
                        pendingSolveCount = pendingCount,
                        onSelectOption = viewModel::soloSelectOption,
                        onSetAnswerText = viewModel::soloSetAnswerText,
                        onSubmit = viewModel::soloSubmit,
                        onNext = viewModel::soloNext,
                        onExit = {
                            viewModel.soloExit()
                            navController.popBackStack()
                        },
                        onReplaySame = viewModel::soloReplaySame,
                        onNewRandom = viewModel::soloNewRandom,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onReport = { questionId ->
                            viewModel.submitFeedback(
                                type = "QUESTION_ERROR",
                                questionId = questionId,
                                content = "단일 채점 풀이 화면에서 신고",
                            ) { /* ViewModel message displays the result. */ }
                        },
                        bookmarkedIds = state.runnerBookmarks,
                    )
                }
                composable(
                    SqldpassRoute.Bookmarks.route,
                    enterTransition = { pushSlideEnter() },
                    exitTransition = { pushSlideExitForward() },
                    popExitTransition = { pushSlidePopExit() },
                ) {
                    BookmarksScreen(
                        state = state,
                        onLoadBookmarks = viewModel::loadBookmarks,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onBack = {
                            if (navController.previousBackStackEntry != null) navController.popBackStack()
                        },
                    )
                }
                composable(
                    SqldpassRoute.History.route,
                    enterTransition = { pushSlideEnter() },
                    exitTransition = { pushSlideExitForward() },
                    popExitTransition = { pushSlidePopExit() },
                ) {
                    HistoryScreen(
                        state = state,
                        onLoadHistory = viewModel::loadHistory,
                        onOpenHistoryDetail = { id ->
                            viewModel.openHistoryDetail(id) { ok ->
                                if (ok) navController.navigate(SqldpassRoute.Runner.route)
                            }
                        },
                        onBack = {
                            if (navController.previousBackStackEntry != null) navController.popBackStack()
                        },
                    )
                }
            }
            if (state.loading) {
                AppLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    if (showNavigationSuite) {
        val palette = com.sqldpass.app.ui.theme.LocalSqldpassPalette.current
        AppNavigationSuiteScaffold(
            navigationSuiteItems = {
                BOTTOM_TABS.forEach { tab ->
                    val selected = currentRoute == tab.route.route
                    item(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                // 같은 탭 재탭 — 해당 탭의 루트로 popBackStack (Play 표준 UX)
                                navController.popBackStack(tab.route.route, inclusive = false)
                            } else {
                                navController.navigate(tab.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) palette.accent else palette.textMuted,
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                color = if (selected) palette.accent else palette.textMuted,
                            )
                        },
                    )
                }
            },
            containerColor = palette.page,
        ) {
            appContent()
        }
    } else {
        appContent()
    }
}

private class AppNavigationSuiteScope {
    val items = mutableListOf<AppNavigationSuiteItem>()

    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
    ) {
        items += AppNavigationSuiteItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
        )
    }
}

private data class AppNavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
)

@Composable
private fun AppNavigationSuiteScaffold(
    navigationSuiteItems: AppNavigationSuiteScope.() -> Unit,
    containerColor: Color,
    content: @Composable () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = AppNavigationSuiteScope().apply(navigationSuiteItems)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.border),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                scope.items.forEach { item ->
                    val interactionSource = remember(item) { MutableInteractionSource() }
                    val bg = if (item.selected) palette.accentSoftBg else Color.Transparent
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(SqldRadius.md))
                            .background(bg)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.foundation.LocalIndication.current,
                                role = Role.Tab,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    item.onClick()
                                },
                            )
                            .padding(horizontal = SqldSpacing.xs, vertical = SqldSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SqldSpacing.xxs),
                    ) {
                        item.icon()
                        item.label()
                    }
                }
            }
        }
    }
}
