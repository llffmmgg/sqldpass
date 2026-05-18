package com.sqldpass.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.WrongAnswerStatsSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.dashboard.DailyChartCard

private val CardCorner = 14.dp

@Composable
fun InsightsTab(
    state: AppUiState,
    onLoad: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) onLoad()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            title = "인사이트",
            subtitle = state.nickname?.let { "$it 님의 데이터로 약점을 좁혀요." }
                ?: "로그인하면 학습 데이터가 시각화돼요.",
        )

        if (state.nickname == null) {
            Column(modifier = Modifier.padding(20.dp)) {
                CtaCard(
                    title = "로그인이 필요합니다",
                    meta = "Google 로 로그인하면 과목별 정답률, 풀이 추이, 회차별 최고 점수가 보입니다.",
                    ctaLabel = "Google 로 로그인",
                    onClick = onLogin,
                )
            }
            return
        }

        val stats = state.wrongAnswerStats.sortedByDescending { it.wrongRate }
        val daily = state.dashboard?.dailyCounts.orEmpty()
        val bests = state.dashboard?.bestScores.orEmpty()
        val examNameById = state.mockExams.associate { it.id to it.name }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SubjectAccuracyCard(stats = stats)
            }
            item {
                DailyChartCard(counts = daily)
            }
            if (bests.isNotEmpty()) {
                item {
                    Text("회차별 최고 점수", style = MaterialTheme.typography.titleMedium)
                }
                items(bests.take(10), key = { it.mockExamId }) { best ->
                    BestScoreRow(
                        score = best,
                        examName = examNameById[best.mockExamId] ?: "회차 #${best.mockExamId}",
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectAccuracyCard(stats: List<WrongAnswerStatsSummary>) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("과목별 정답률", style = MaterialTheme.typography.titleMedium)
            if (stats.isEmpty()) {
                Text(
                    "아직 집계할 풀이 데이터가 없어요. 한 회차 풀어보면 여기 채워집니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                stats.take(8).forEach { stat ->
                    AccuracyBar(stat)
                }
            }
        }
    }
}

@Composable
private fun AccuracyBar(stat: WrongAnswerStatsSummary) {
    val accuracy = (100 - stat.wrongRate).coerceIn(0, 100)
    val isWeak = accuracy < 70
    val barColor = when {
        accuracy >= 90 -> MaterialTheme.colorScheme.primary
        isWeak -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fraction = accuracy / 100f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stat.subjectName, style = MaterialTheme.typography.bodyMedium)
                if (isWeak) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = "취약 과목",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                "${accuracy}% · 오답 ${stat.wrongCount}/${stat.totalSolved}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun BestScoreRow(score: BestScoreSummary, examName: String) {
    val rate = if (score.totalCount > 0) score.correctCount * 100 / score.totalCount else 0
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                examName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${score.correctCount}/${score.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${rate}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("unused")
private val Unused: Color = Color.Transparent
