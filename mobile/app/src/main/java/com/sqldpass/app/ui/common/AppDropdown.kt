package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

@Composable
fun AppDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val palette = LocalSqldpassPalette.current
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, 44),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 176.dp, max = 260.dp)
                .clip(RoundedCornerShape(SqldRadius.md))
                .background(palette.card)
                .border(
                    BorderStroke(1.dp, palette.border),
                    RoundedCornerShape(SqldRadius.md),
                )
                .padding(vertical = SqldSpacing.xs),
            content = content,
        )
    }
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
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = SqldSpacing.md, vertical = SqldSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(name = "AppDropdown")
@Composable
private fun PreviewAppDropdown() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDropdown(expanded = true, onDismiss = {}) {
                AppDropdownItem(label = "Edit", leadingIcon = Icons.Outlined.Edit, onClick = {})
                AppDropdownItem(label = "Share", leadingIcon = Icons.Outlined.Share, onClick = {})
                AppDropdownItem(
                    label = "Delete",
                    leadingIcon = Icons.Outlined.Delete,
                    destructive = true,
                    onClick = {},
                )
            }
        }
    }
}
