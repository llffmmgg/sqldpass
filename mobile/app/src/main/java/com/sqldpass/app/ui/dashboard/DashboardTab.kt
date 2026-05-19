package com.sqldpass.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.OverallAvgResponse
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.data.ThemeMode
import com.sqldpass.app.data.WrongAnswerStatsSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.DashboardData
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

@Composable
fun DashboardTab(
    state: AppUiState,
    onLoadDashboard: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onPurchase: (String) -> Unit,
    onLoadWrongStats: () -> Unit = {},
    onStartWrongAnswers: (Long?, String?) -> Unit = { _, _ -> },
    onUpdateNickname: (String, (Boolean) -> Unit) -> Unit = { _, cb -> cb(false) },
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onThemeChange: (ThemeMode) -> Unit = {},
) {
    var nicknameDialogOpen by remember { mutableStateOf(false) }
    var nicknameSubmitting by remember { mutableStateOf(false) }
    if (nicknameDialogOpen) {
        NicknameEditDialog(
            currentNickname = state.nickname,
            submitting = nicknameSubmitting,
            onDismiss = { if (!nicknameSubmitting) nicknameDialogOpen = false },
            onSubmit = { newName ->
                nicknameSubmitting = true
                onUpdateNickname(newName) { ok ->
                    nicknameSubmitting = false
                    if (ok) nicknameDialogOpen = false
                }
            },
        )
    }
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) {
            onLoadDashboard()
            onLoadWrongStats()
        }
    }
    val examNameById = state.mockExams.associate { it.id to it.name }
    val palette = LocalSqldpassPalette.current
    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            title = "대시보드",
            subtitle = state.nickname?.let { "$it 님의 학습 기록" }
                ?: "Google 로 로그인하면 학습 기록이 쌓여요.",
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        if (state.nickname == null) {
            item {
                CtaCard(
                    title = "로그인이 필요합니다",
                    meta = "Google 로 로그인하면 연속 학습일과 회차별 최고 점수가 보입니다.",
                    ctaLabel = "Google 로 로그인",
                    onClick = onLogin,
                )
            }
        } else {
            item {
                StreakCard(currentStreak = state.dashboard?.streak?.currentStreak ?: 0)
            }
            item {
                AvgCard(
                    overallAvg = state.dashboard?.overallAvg?.let { it.overallAvg ?: it.avgDailyCount },
                    myRecentAvg = state.dashboard?.overallAvg?.myRecentAvg,
                )
            }
            item {
                DailyChartCard(counts = state.dashboard?.dailyCounts.orEmpty())
            }
            val best = state.dashboard?.bestScores.orEmpty()
            if (best.isNotEmpty()) {
                item {
                    Text(
                        "회차별 최고 점수",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }
                items(best, key = { it.mockExamId }) { score ->
                    BestScoreCard(
                        score = score,
                        examName = examNameById[score.mockExamId] ?: "#${score.mockExamId}",
                    )
                }
            }
            val weak = state.wrongAnswerStats
                .sortedByDescending { it.wrongRate }
                .take(5)
            if (weak.isNotEmpty()) {
                item {
                    Text(
                        "취약 과목",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }
                items(weak, key = { it.subjectId }) { s ->
                    WeakSubjectCard(
                        stat = s,
                        onStart = { onStartWrongAnswers(s.subjectId, s.subjectName) },
                    )
                }
                item {
                    AppButton(
                        text = "오답 전체 모아풀기",
                        onClick = { onStartWrongAnswers(null, null) },
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Regular,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            CtaCard(
                title = "PASS+ 모의고사",
                meta = "프리미엄 회차는 구매 후 앱에서도 오프라인 풀이가 가능합니다.",
                ctaLabel = "PASS+ 보기",
                onClick = { onPurchase("iap_one_month") },
            )
        }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeToggleCard(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Text(
            "화면 테마",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            listOf(
                ThemeMode.SYSTEM to "시스템",
                ThemeMode.LIGHT to "라이트",
                ThemeMode.DARK to "다크",
            ).forEach { (mode, label) ->
                AppChip(
                    label = label,
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                )
            }
        }
    }
}

@Composable
private fun StreakCard(currentStreak: Int) {
    val palette = LocalSqldpassPalette.current
    val cert = com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors.current.cert
    val amber = cert.sqld
    AppCard(
        surface = AppCardSurface.Card,
        accent = AppCardAccent.Sqld,
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Whatshot,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "연속 학습 일수",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
        }
        Text(
            "$currentStreak",
            style = MaterialTheme.typography.displayMedium,
            color = amber,
        )
        Text(
            if (currentStreak >= 1) "오늘도 한 회차, 꾸준함이 합격으로."
            else "오늘 풀이로 streak 을 시작해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun AvgCard(overallAvg: Double?, myRecentAvg: Double?) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Text(
            "최근 14일 평균",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        val overall = overallAvg?.let { "%.1f".format(it) } ?: "—"
        val mine = myRecentAvg?.let { "%.1f".format(it) }
        Text(
            "전체 평균 ${overall}문 / 일",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
        mine?.let {
            Text(
                "내 평균 ${it}문 / 일",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun WeakSubjectCard(stat: WrongAnswerStatsSummary, onStart: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stat.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Text(
                    "오답 ${stat.wrongCount}/${stat.totalSolved} · 오답률 ${stat.wrongRate}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
            AppButton(
                text = "다시 풀기",
                onClick = onStart,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
            )
        }
    }
}

@Composable
private fun BestScoreCard(score: BestScoreSummary, examName: String) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                examName,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${score.correctCount}/${score.totalCount}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
            )
        }
    }
}

private fun mockState(nickname: String? = "문어"): AppUiState = AppUiState(
    nickname = nickname,
    dashboard = DashboardData(
        streak = StreakResponse(currentStreak = 14, longestStreak = 21),
        overallAvg = OverallAvgResponse(overallAvg = 12.4, myRecentAvg = 18.7, periodDays = 14),
        bestScores = listOf(
            BestScoreSummary(mockExamId = 101, correctCount = 42, totalCount = 50),
            BestScoreSummary(mockExamId = 102, correctCount = 38, totalCount = 50),
        ),
    ),
)

@Preview(name = "Dashboard — Light", showBackground = true)
@Composable
private fun DashboardPreviewLight() {
    SqldpassTheme(darkTheme = false) {
        DashboardTab(state = mockState(), onLoadDashboard = {}, onLogin = {}, onLogout = {}, onPurchase = {})
    }
}

@Preview(name = "Dashboard — Dark", showBackground = true)
@Composable
private fun DashboardPreviewDark() {
    SqldpassTheme(darkTheme = true) {
        DashboardTab(state = mockState(nickname = null), onLoadDashboard = {}, onLogin = {}, onLogout = {}, onPurchase = {})
    }
}

@Preview(name = "Dashboard — Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun DashboardPreviewLargeFont() {
    SqldpassTheme(darkTheme = false) {
        DashboardTab(state = mockState(), onLoadDashboard = {}, onLogin = {}, onLogout = {}, onPurchase = {})
    }
}
