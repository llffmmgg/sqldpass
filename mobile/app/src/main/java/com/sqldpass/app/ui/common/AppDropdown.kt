package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Inked OMR 드롭다운 메뉴 primitive.
 *
 * Material3 `DropdownMenu` 는 컨테이너 색을 직접 받지 않으므로 modifier 의 background +
 * clip 으로 `LocalSqldpassPalette.current.card` + `SqldRadius.md` 를 적용한다.
 *
 * `AppDropdownItem` 은 라벨 + 선택 아이콘 + destructive 변형을 제공한다 — `destructive=true`
 * 면 텍스트와 아이콘 모두 `LocalSqldpassPalette.current.danger` 로 칠한다.
 */
@Composable
fun AppDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.md))
            .background(palette.card),
        content = content,
    )
}

@Composable
fun AppDropdownItem(
    label: String,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val color = if (destructive) palette.danger else palette.textPrimary

    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = color,
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
}

@Preview(name = "AppDropdown — 3 items (2 normal + 1 destructive)")
@Composable
private fun PreviewAppDropdown() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDropdown(expanded = true, onDismiss = {}) {
                AppDropdownItem(
                    label = "수정",
                    leadingIcon = Icons.Outlined.Edit,
                    onClick = {},
                )
                AppDropdownItem(
                    label = "공유",
                    leadingIcon = Icons.Outlined.Share,
                    onClick = {},
                )
                AppDropdownItem(
                    label = "삭제",
                    leadingIcon = Icons.Outlined.Delete,
                    destructive = true,
                    onClick = {},
                )
            }
        }
    }
}
