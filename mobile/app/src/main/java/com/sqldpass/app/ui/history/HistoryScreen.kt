package com.sqldpass.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.SolveSummary
import com.sqldpass.app.text.formatKstDateTime
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppMascotPose
import com.sqldpass.app.ui.common.AppStateView
import com.sqldpass.app.ui.common.AppViewState
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 마이 > 풀이 기록. iOS HistoryView 의 정보 구조 따라 점수 + 일자 + 정답수 카드를
 * LazyColumn 으로 표시.
 *
 * 카드를 탭하면 기존 채점 결과 화면으로 진입해 GET /api/solves/{id} 결과를 보여준다.
 */
@Composable
fun HistoryScreen(
    state: AppUiState,
    onLoadHistory: () -> Unit,
    onOpenHistoryDetail: (Long) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) onLoadHistory()
    }

    val palette = LocalSqldpassPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        HistoryTopBar(onBack = onBack)

        when {
            state.historyLoading && state.history.isEmpty() -> {
                AppStateView(
                    state = AppViewState.Loading,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.historyError != null && state.history.isEmpty() -> {
                AppStateView(
                    state = AppViewState.ErrorState(
                        title = "풀이 기록을 불러오지 못했어요",
                        message = state.historyError,
                        onRetry = onLoadHistory,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.history.isEmpty() -> {
                AppStateView(
                    state = AppViewState.Empty(
                        title = "아직 푼 기록이 없어요",
                        message = "모의고사·랜덤 풀이를 마치면 여기에 기록이 쌓여요.",
                        mascot = AppMascotPose.Guide,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SqldSpacing.lg - SqldSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                ) {
                    items(state.history, key = { it.id }) { item ->
                        HistoryRow(
                            solve = item,
                            onClick = { onOpenHistoryDetail(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(onBack: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.page)
            .statusBarsPadding()
            .padding(
                start = SqldSpacing.sm,
                end = SqldSpacing.lg,
                top = SqldSpacing.sm,
                bottom = SqldSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "뒤로가기",
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "풀이 기록",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HistoryRow(solve: SolveSummary, onClick: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    solve.score.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scoreColor(solve.score, palette.accent, palette.info, palette.danger),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "점",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Box(modifier = Modifier.weight(1f))
                formatKstDateTime(solve.solvedAt)?.let { date ->
                    Text(
                        date,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSubtle,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            ) {
                Text(
                    "${solve.correctCount} / ${solve.totalCount} 정답",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textMuted,
                )
                Text(
                    historyKindLabel(solve),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSubtle,
                )
            }
        }
    }
}

private fun historyKindLabel(solve: SolveSummary): String = when {
    solve.mockExamId != null -> "모의고사 #${solve.mockExamId}"
    solve.subjectId != null -> "랜덤 풀이"
    else -> "풀이"
}

private fun scoreColor(score: Int, accent: Color, info: Color, danger: Color): Color = when {
    score >= 80 -> accent
    score >= 60 -> info
    else -> danger
}
