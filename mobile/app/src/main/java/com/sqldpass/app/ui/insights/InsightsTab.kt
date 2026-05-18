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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.WrongAnswerStatsSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.dashboard.DailyChartCard
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

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
            Column(modifier = Modifier.padding(SqldSpacing.lg - 4.dp)) {
                CtaCard(
                    title = "로그인이 필요합니다",
                    meta = "Google 로 로그인하면 과목별 정답률, 풀이 추이, 회차별 최고 점수가 보입니다.",
                    ctaLabel = "Google 로 로그인",
                    onClick = onLogin,
                )
            }
            return
        }

        val palette = LocalSqldpassPalette.current
        val stats = state.wrongAnswerStats.sortedByDescending { it.wrongRate }
        val daily = state.dashboard?.dailyCounts.orEmpty()
        val bests = state.dashboard?.bestScores.orEmpty()
        val examNameById = state.mockExams.associate { it.id to it.name }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SqldSpacing.lg - 4.dp),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.md + 2.dp),
        ) {
            item {
                SubjectAccuracyCard(stats = stats)
            }
            item {
                DailyChartCard(counts = daily)
            }
            if (bests.isNotEmpty()) {
                item {
                    Text(
                        "회차별 최고 점수",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
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
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.None) {
        Text(
            "과목별 정답률",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        if (stats.isEmpty()) {
            Text(
                "아직 집계할 풀이 데이터가 없어요. 한 회차 풀어보면 여기 채워집니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
        } else {
            stats.take(8).forEach { stat ->
                AccuracyBar(stat)
            }
        }
    }
}

@Composable
private fun AccuracyBar(stat: WrongAnswerStatsSummary) {
    val palette = LocalSqldpassPalette.current
    val accuracy = (100 - stat.wrongRate).coerceIn(0, 100)
    val isWeak = accuracy < 70
    val barColor = when {
        accuracy >= 90 -> palette.accent
        isWeak -> palette.danger
        else -> palette.warning
    }
    val trackColor = palette.elevated
    val fraction = accuracy / 100f

    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                Text(
                    stat.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                )
                if (isWeak) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = "취약 과목",
                        tint = palette.danger,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                "${accuracy}% · 오답 ${stat.wrongCount}/${stat.totalSolved}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SqldSpacing.sm)
                .clip(RoundedCornerShape(SqldRadius.sm - 2.dp))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(SqldSpacing.sm)
                    .clip(RoundedCornerShape(SqldRadius.sm - 2.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun BestScoreRow(score: BestScoreSummary, examName: String) {
    val palette = LocalSqldpassPalette.current
    val rate = if (score.totalCount > 0) score.correctCount * 100 / score.totalCount else 0
    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.None) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                examName,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${score.correctCount}/${score.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                )
                Text(
                    "${rate}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
        }
    }
}
