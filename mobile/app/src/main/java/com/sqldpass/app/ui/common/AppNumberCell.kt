package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassMonoText
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 단일 KPI 셀 — 라벨 + 숫자 + 선택적 단위. 2×2 그리드로 묶기 좋게 weight(1f) 친화.
 *
 * - Compact: mono body
 * - Regular: mono large (28sp)
 * - Display: mono display (32sp)
 *
 * unit 은 숫자 오른쪽에 baseline 정렬 mono small / textMuted.
 */
enum class AppNumberCellSize { Compact, Regular, Display }

@Composable
fun AppNumberCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    accent: Color? = null,
    size: AppNumberCellSize = AppNumberCellSize.Regular,
) {
    val palette = LocalSqldpassPalette.current
    val numberStyle: TextStyle = when (size) {
        AppNumberCellSize.Compact -> SqldpassMonoText.body
        AppNumberCellSize.Regular -> SqldpassMonoText.large
        AppNumberCellSize.Display -> SqldpassMonoText.display
    }
    val numberColor = accent ?: palette.accent

    AppCard(
        surface = AppCardSurface.Card,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = value,
                        style = numberStyle,
                        color = numberColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (unit != null) {
                        Text(
                            text = unit,
                            style = SqldpassMonoText.small,
                            color = palette.textMuted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "AppNumberCell — 2x2 KPI grid")
@Composable
private fun PreviewAppNumberCellGrid() {
    SqldpassTheme(darkTheme = true) {
        val palette = LocalSqldpassPalette.current
        Box(Modifier.background(palette.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
                    AppNumberCell(
                        value = "87",
                        label = "정답률",
                        unit = "%",
                        modifier = Modifier.weight(1f),
                    )
                    AppNumberCell(
                        value = "12",
                        label = "연속 학습일",
                        unit = "일",
                        accent = palette.warning,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
                    AppNumberCell(
                        value = "1,248",
                        label = "총 풀이",
                        size = AppNumberCellSize.Compact,
                        modifier = Modifier.weight(1f),
                    )
                    AppNumberCell(
                        value = "34",
                        label = "오답 노트",
                        accent = palette.danger,
                        size = AppNumberCellSize.Compact,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppNumberCell(
                    value = "92.4",
                    label = "예상 점수",
                    unit = "점",
                    size = AppNumberCellSize.Display,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
