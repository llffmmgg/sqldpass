package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldpassMonoText
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Pill 형태 토글 칩.
 * - 기본: elevated bg + border + textPrimary.
 * - 선택: accentSoftBg (~0.08–0.12 alpha — 흐릿 효과 아님, 선택 시그널) + accent border 1.5dp + accent text.
 * - count 배지: 우측에 작은 mono 숫자 카운트. (선택 시 accent fill, 기본 시 card fill.)
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    count: Int? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val bg = if (selected) palette.accentSoftBg else palette.elevated
    val borderColor = if (selected) palette.accent else palette.border
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val textColor = if (selected) palette.accent else palette.textPrimary
    val effectiveAlpha = if (enabled) 1f else 0.45f

    Box(
        modifier = modifier
            .alpha(effectiveAlpha)
            .clip(RoundedCornerShape(SqldRadius.full))
            .background(bg)
            .border(
                BorderStroke(borderWidth, borderColor),
                RoundedCornerShape(SqldRadius.full),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            if (count != null) {
                val badgeBg = if (selected) palette.accent else palette.card
                val badgeFg = if (selected) palette.accentFg else palette.textMuted
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SqldRadius.full))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = count.toString(),
                        style = SqldpassMonoText.small,
                        color = badgeFg,
                    )
                }
            }
        }
    }
}

@Preview(name = "AppChip — States")
@Composable
private fun PreviewAppChipStates() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(label = "전체", selected = true, onClick = {})
                    AppChip(label = "기출", selected = false, onClick = {})
                    AppChip(label = "오답", selected = false, onClick = {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(label = "북마크", selected = true, onClick = {}, count = 12)
                    AppChip(label = "미해결", selected = false, onClick = {}, count = 34)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(
                        label = "필터",
                        selected = false,
                        onClick = {},
                        leadingIcon = Icons.Outlined.FilterList,
                    )
                    AppChip(label = "비활성", selected = false, onClick = {}, enabled = false)
                }
            }
        }
    }
}
