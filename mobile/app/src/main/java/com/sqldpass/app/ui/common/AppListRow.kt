package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 그룹 리스트 행. 기본은 라운드 없음 — caller 가 AppCard 로 감싸거나 LazyColumn 안에 묶음.
 *
 * - leading: 좌측 아이콘 슬롯 (옵션). tint 지정 가능.
 * - title + (subtitle): 가운데, weight 1.
 * - trailing text + chevron: 우측.
 * - destructive: title/icon 모두 danger 색.
 */
@Composable
fun AppListRow(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = LocalSqldpassPalette.current.textMuted,
    trailing: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val effectiveAlpha = if (enabled) 1f else 0.45f

    val titleColor = if (destructive) palette.danger else palette.textPrimary
    val resolvedLeadingTint = if (destructive) palette.danger else leadingIconTint
    val chevronTint = palette.textSubtle

    Row(
        modifier = modifier
            .alpha(effectiveAlpha)
            .fillMaxWidth()
            .background(palette.card)
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = SqldSpacing.base, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = resolvedLeadingTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textMuted,
            )
        }
        Icon(
            trailingIcon,
            contentDescription = null,
            tint = chevronTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "AppListRow — variants")
@Composable
private fun PreviewAppListRow() {
    SqldpassTheme(darkTheme = true) {
        val palette = LocalSqldpassPalette.current
        Box(Modifier.background(palette.page).padding(16.dp)) {
            Column {
                AppListRow(title = "기본 행", onClick = {})
                Box(Modifier.fillMaxWidth().background(palette.border).size(1.dp).width(1.dp))
                AppListRow(
                    title = "서브타이틀이 있는 행",
                    subtitle = "두 번째 줄에 보조 설명을 표시합니다",
                    leadingIcon = Icons.Outlined.Settings,
                    onClick = {},
                )
                AppListRow(
                    title = "알림",
                    leadingIcon = Icons.Outlined.Notifications,
                    trailing = "On",
                    onClick = {},
                )
                AppListRow(
                    title = "계정 삭제",
                    leadingIcon = Icons.Outlined.Delete,
                    destructive = true,
                    onClick = {},
                )
                AppListRow(
                    title = "비활성 행",
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}
