package com.sqldpass.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.DailyCountResponse
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 일별 풀이 수 라인 차트 — Compose Canvas 직접 그리기, 의존성 추가 없음.
 * 빈 날은 0 으로 채워 14일 연속 라인 + 점.
 */
@Composable
fun DailyChartCard(
    counts: List<DailyCountResponse>,
    days: Int = 14,
) {
    val palette = LocalSqldpassPalette.current
    val today = LocalDate.now()
    val byDate = counts.associate { runCatching { LocalDate.parse(it.date) }.getOrNull() to it.count }
        .filterKeys { it != null }
    val series: List<Pair<LocalDate, Long>> = (0 until days).map { offset ->
        val d = today.minusDays((days - 1 - offset).toLong())
        d to (byDate[d] ?: 0L)
    }
    val max = (series.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val total = series.sumOf { it.second }
    val primary = palette.accent
    val grid = palette.border
    val dotInner = palette.elevated

    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.None) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "최근 ${days}일 풀이 추이",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Text(
                "총 $total",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            val w = size.width
            val h = size.height
            val padH = 8f
            val padV = 12f
            val plotW = w - padH * 2
            val plotH = h - padV * 2

            // 가로 그리드 4선
            repeat(4) { i ->
                val y = padV + plotH * i / 3f
                drawLine(
                    color = grid,
                    start = Offset(padH, y),
                    end = Offset(w - padH, y),
                    strokeWidth = 1f,
                )
            }

            if (series.isEmpty()) return@Canvas
            val stepX = if (series.size > 1) plotW / (series.size - 1) else 0f
            val points = series.mapIndexed { idx, (_, value) ->
                val x = padH + stepX * idx
                val y = padV + plotH * (1f - value.toFloat() / max.toFloat())
                Offset(x, y)
            }

            // 채움 영역 (primary 의 alpha 0.18)
            val fillPath = Path().apply {
                moveTo(points.first().x, padV + plotH)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, padV + plotH)
                close()
            }
            drawPath(path = fillPath, color = primary.copy(alpha = 0.18f))

            // 라인
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = primary,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
            // 점
            points.forEach { p ->
                drawCircle(color = primary, radius = 4f, center = p)
                drawCircle(color = dotInner, radius = 2f, center = p)
            }
        }
        // x축 라벨 — 첫·중간·마지막 3개
        val labelFormatter = DateTimeFormatter.ofPattern("M/d")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val first = series.first().first
            val mid = series[series.size / 2].first
            val last = series.last().first
            listOf(first, mid, last).forEach { d ->
                Text(
                    d.format(labelFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
            }
        }
    }
}
