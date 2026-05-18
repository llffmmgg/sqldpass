package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * BasicTextField 기반 인풋 — Material3 OutlinedTextField 의 라벨 floating /
 * placeholder 애니메이션 노이즈를 제거하고, 라벨은 항상 ABOVE 고정.
 *
 * - border: default border → 포커스 시 accent → error 시 danger.
 * - cursor: accent.
 * - helper / error: input 아래.
 * - leadingIcon: 좌측 18dp 슬롯.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        error != null -> palette.danger
        isFocused -> palette.accent
        else -> palette.border
    }
    val effectiveAlpha = if (enabled) 1f else 0.45f

    Column(modifier = modifier.alpha(effectiveAlpha)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
            modifier = Modifier.padding(bottom = SqldSpacing.xs),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SqldRadius.md))
                .background(palette.elevated)
                .border(
                    BorderStroke(1.dp, borderColor),
                    RoundedCornerShape(SqldRadius.md),
                )
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(SqldSpacing.sm))
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = interactionSource,
                        decorationBox = { innerTextField ->
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textSubtle,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
        }
        val helperText = error ?: helper
        val helperColor = if (error != null) palette.danger else palette.textSubtle
        if (helperText != null) {
            Text(
                helperText,
                style = MaterialTheme.typography.labelSmall,
                color = helperColor,
                modifier = Modifier.padding(top = SqldSpacing.xs, start = SqldSpacing.xxs),
            )
        }
    }
}

@Preview(name = "AppTextField — States")
@Composable
private fun PreviewAppTextField() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppTextField(
                    value = "",
                    onValueChange = {},
                    label = "이메일",
                    placeholder = "you@example.com",
                    helper = "로그인에 사용한 이메일을 입력하세요",
                )
                AppTextField(
                    value = "heehun3658@gmail.com",
                    onValueChange = {},
                    label = "이메일",
                )
                AppTextField(
                    value = "abc",
                    onValueChange = {},
                    label = "비밀번호",
                    error = "8자 이상 입력해주세요",
                )
                AppTextField(
                    value = "",
                    onValueChange = {},
                    label = "검색",
                    placeholder = "문제를 검색하세요",
                    leadingIcon = Icons.Outlined.Search,
                )
                AppTextField(
                    value = "disabled",
                    onValueChange = {},
                    label = "비활성",
                    enabled = false,
                )
            }
        }
    }
}
