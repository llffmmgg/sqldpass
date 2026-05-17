package com.sqldpass.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.OverallAvgResponse
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.DashboardData
import com.sqldpass.app.ui.home.ActionCard
import com.sqldpass.app.ui.theme.SqldpassTheme

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun DashboardTab(
    state: AppUiState,
    onLoadDashboard: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onPurchase: (String) -> Unit,
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) onLoadDashboard()
    }
    val examNameById = state.mockExams.associate { it.id to it.name }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("대시보드", style = MaterialTheme.typography.headlineSmall) }
        item {
            AccountCard(
                nickname = state.nickname,
                onLogin = onLogin,
                onLogout = onLogout,
            )
        }
        if (state.nickname != null) {
            item {
                StreakCard(currentStreak = state.dashboard?.streak?.currentStreak ?: 0)
            }
            item {
                AvgCard(
                    overallAvg = state.dashboard?.overallAvg?.overallAvg,
                    myRecentAvg = state.dashboard?.overallAvg?.myRecentAvg,
                )
            }
            val best = state.dashboard?.bestScores.orEmpty()
            if (best.isNotEmpty()) {
                item { Text("회차별 최고 점수", style = MaterialTheme.typography.titleMedium) }
                items(best, key = { it.mockExamId }) { score ->
                    BestScoreCard(
                        score = score,
                        examName = examNameById[score.mockExamId] ?: "#${score.mockExamId}",
                    )
                }
            }
        }
        item {
            ActionCard(
                title = "PASS+ 모의고사",
                body = "프리미엄 회차는 구매 후 앱에서도 오프라인 풀이가 가능합니다.",
                action = "PASS+ 보기",
                onClick = { onPurchase("iap_one_month") },
            )
        }
    }
}

@Composable
private fun AccountCard(
    nickname: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (nickname == null) {
                Text("로그인이 필요합니다", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Google 로 로그인해 학습 기록과 합격률을 확인하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onLogin,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) { Text("Google 로 로그인") }
            } else {
                Text("$nickname 님", style = MaterialTheme.typography.titleMedium)
                Text(
                    "문어CBT 와 함께 학습 중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        shape = RoundedCornerShape(ButtonCorner),
                        onClick = onLogout,
                        modifier = Modifier.sizeIn(minHeight = 48.dp),
                    ) { Text("로그아웃") }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(currentStreak: Int) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("연속 학습 일수", style = MaterialTheme.typography.titleMedium)
            Text(
                "$currentStreak",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (currentStreak >= 1) "오늘도 한 회차, 꾸준함이 합격으로."
                else "오늘 풀이로 streak 을 시작해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AvgCard(overallAvg: Double?, myRecentAvg: Double?) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("최근 14일 평균", style = MaterialTheme.typography.titleMedium)
            val overall = overallAvg?.let { "%.1f".format(it) } ?: "—"
            val mine = myRecentAvg?.let { "%.1f".format(it) }
            Text(
                "전체 평균 ${overall}문 / 일",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            mine?.let {
                Text(
                    "내 평균 ${it}문 / 일",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BestScoreCard(score: BestScoreSummary, examName: String) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                examName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${score.correctCount}/${score.totalCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
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
