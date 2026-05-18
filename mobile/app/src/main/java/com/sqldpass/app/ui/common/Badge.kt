package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * soft tone Badge — 배경 = base alpha 0.14, 라벨 = base color.
 * frontend 의 `--primary-soft` / `--cta-bg-hover` 패턴.
 * label 은 짧게 (최대 8자) — Korean: "가장 인기", "PASS+", "기출"
 */
@Composable
fun SqldpassBadge(
    label: String,
    base: Color,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
) {
    val bg = if (solid) base else base.copy(alpha = 0.14f)
    val fg = if (solid) Color.White else base
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}
