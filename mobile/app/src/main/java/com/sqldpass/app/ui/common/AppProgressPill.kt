package com.sqldpass.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassMonoText
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 진행 상태/타이머 표시용 pill primitive.
 *
 * - 좌: 선택적 label (예: "솔로 풀이") in labelMedium / textMuted.
 * - 중: "current/total" or "MM:SS" — `SqldpassMonoText.body`, color = accent.
 * - 우: 가는 progress bar (W64dp × H4dp, full radius). track = elevated, fill = accent.
 *   fill 너비는 ratio 변경 시 280ms easeOut 로 애니메이션.
 *
 * 깜빡임/blur/glow 일절 없음. accent 색만 60s/10s 임계에서 전환.
 */
enum class AppProgressAccent { Default, Warning, Danger }

@Composable
fun AppProgressPill(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    accent: AppProgressAccent = AppProgressAccent.Default,
) {
    val palette = LocalSqldpassPalette.current
    val accentColor = appProgressAccentColor(accent, palette)
    val safeTotal = if (total <= 0) 1 else total
    val ratio = (current.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 280),
        label = "progress-fill",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.full))
            .background(palette.card)
            .border(
                BorderStroke(1.dp, palette.border),
                RoundedCornerShape(SqldRadius.full),
            )
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
        }
        Text(
            text = "$current/$total",
            style = SqldpassMonoText.body,
            color = accentColor,
        )
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(SqldRadius.full))
                .background(palette.elevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRatio)
                    .background(accentColor),
            )
        }
    }
}

/**
 * 타이머 변형. 남은 초를 MM:SS 로 포맷하고 60s/10s 임계에서 accent 색 자동 전환.
 *
 * - >= 60s: Default
 * - <  60s: Warning
 * - <= 10s: Danger
 */
@Composable
fun AppProgressPillTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val palette = LocalSqldpassPalette.current
    val accent = when {
        remainingSeconds <= 10 -> AppProgressAccent.Danger
        remainingSeconds < 60 -> AppProgressAccent.Warning
        else -> AppProgressAccent.Default
    }
    val accentColor = appProgressAccentColor(accent, palette)
    val safeTotal = if (totalSeconds <= 0) 1 else totalSeconds
    val rawRatio = remainingSeconds.toFloat() / safeTotal.toFloat()
    val ratio = rawRatio.coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 280),
        label = "timer-fill",
    )

    val clampedRemaining = if (remainingSeconds < 0) 0 else remainingSeconds
    val minutes = clampedRemaining / 60
    val seconds = clampedRemaining % 60
    val mmss = "%02d:%02d".format(minutes, seconds)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.full))
            .background(palette.card)
            .border(
                BorderStroke(1.dp, palette.border),
                RoundedCornerShape(SqldRadius.full),
            )
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
        }
        Text(
            text = mmss,
            style = SqldpassMonoText.body,
            color = accentColor,
        )
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(SqldRadius.full))
                .background(palette.elevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRatio)
                    .background(accentColor),
            )
        }
    }
}

private fun appProgressAccentColor(
    accent: AppProgressAccent,
    palette: SqldpassPalette,
): Color = when (accent) {
    AppProgressAccent.Default -> palette.accent
    AppProgressAccent.Warning -> palette.warning
    AppProgressAccent.Danger -> palette.danger
}

@Preview(name = "AppProgressPill — variants")
@Composable
private fun PreviewAppProgressPill() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppProgressPill(current = 7, total = 30)
                AppProgressPill(
                    current = 7,
                    total = 30,
                    label = "솔로 풀이",
                )
                AppProgressPill(
                    current = 22,
                    total = 30,
                    label = "타이머",
                    accent = AppProgressAccent.Warning,
                )
                AppProgressPill(
                    current = 29,
                    total = 30,
                    label = "마감 임박",
                    accent = AppProgressAccent.Danger,
                )
                AppProgressPillTimer(
                    remainingSeconds = 180,
                    totalSeconds = 600,
                    label = "남은 시간",
                )
                AppProgressPillTimer(
                    remainingSeconds = 45,
                    totalSeconds = 600,
                    label = "남은 시간",
                )
                AppProgressPillTimer(
                    remainingSeconds = 8,
                    totalSeconds = 600,
                    label = "남은 시간",
                )
            }
        }
    }
}
