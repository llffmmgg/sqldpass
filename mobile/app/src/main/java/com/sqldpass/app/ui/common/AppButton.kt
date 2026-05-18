package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Inked OMR primary CTA primitive.
 *
 * - Press feedback: SqldpassIndication (전역 LocalIndication) — 0.97 scale + 4% wash.
 * - Variants: Primary/Secondary/Tertiary/Destructive.
 * - Sizes: Compact 40dp / Regular 48dp / Large 56dp.
 * - Loading: text/icon swap → CircularProgressIndicator. Width 유지.
 * - Disabled: alpha 0.45, click 차단.
 *
 * 절대 Material3 `Button(...)` 위에 얹지 않는다 — ripple 이 살아남기 때문.
 */
enum class AppButtonVariant { Primary, Secondary, Tertiary, Destructive }

enum class AppButtonSize { Compact, Regular, Large }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val haptic = LocalHapticFeedback.current

    val bg = appButtonBg(variant, palette)
    val fg = appButtonFg(variant, palette)
    val border = appButtonBorder(variant, palette)

    val minHeight = when (size) {
        AppButtonSize.Compact -> 40.dp
        AppButtonSize.Regular -> 48.dp
        AppButtonSize.Large -> 56.dp
    }
    val horizontalPadding = when (size) {
        AppButtonSize.Compact -> 12.dp
        AppButtonSize.Regular -> 16.dp
        AppButtonSize.Large -> 20.dp
    }
    val textStyle = when (size) {
        AppButtonSize.Compact -> MaterialTheme.typography.labelLarge
        AppButtonSize.Regular -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        AppButtonSize.Large -> MaterialTheme.typography.titleMedium
    }
    val iconSize = when (size) {
        AppButtonSize.Compact -> 16.dp
        AppButtonSize.Regular -> 18.dp
        AppButtonSize.Large -> 20.dp
    }

    val effectiveAlpha = if (enabled && !loading) 1f else 0.45f
    val clickable = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }

    val shouldHaptic = variant == AppButtonVariant.Primary ||
        variant == AppButtonVariant.Destructive

    Box(
        modifier = modifier
            .alpha(effectiveAlpha)
            .clip(RoundedCornerShape(SqldRadius.sm))
            .then(
                if (border != null) {
                    Modifier.border(
                        BorderStroke(1.dp, border),
                        RoundedCornerShape(SqldRadius.sm),
                    )
                } else Modifier,
            )
            .background(bg)
            .clickable(
                enabled = clickable,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = {
                    if (shouldHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .sizeIn(minHeight = minHeight)
            .padding(PaddingValues(horizontal = horizontalPadding)),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = fg,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(iconSize),
                    )
                }
                Text(
                    text = text,
                    style = textStyle,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun appButtonBg(variant: AppButtonVariant, palette: SqldpassPalette): Color =
    when (variant) {
        AppButtonVariant.Primary -> palette.accent
        AppButtonVariant.Secondary -> palette.card
        AppButtonVariant.Tertiary -> Color.Transparent
        AppButtonVariant.Destructive -> palette.danger
    }

private fun appButtonFg(variant: AppButtonVariant, palette: SqldpassPalette): Color =
    when (variant) {
        AppButtonVariant.Primary -> palette.accentFg
        AppButtonVariant.Secondary -> palette.textPrimary
        AppButtonVariant.Tertiary -> palette.accent
        AppButtonVariant.Destructive -> Color.White
    }

private fun appButtonBorder(variant: AppButtonVariant, palette: SqldpassPalette): Color? =
    when (variant) {
        AppButtonVariant.Secondary -> palette.border
        else -> null
    }

@Preview(name = "AppButton — Primary 3 sizes")
@Composable
private fun PreviewAppButtonPrimary() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton("시작하기", onClick = {}, size = AppButtonSize.Compact)
                AppButton("시작하기", onClick = {}, size = AppButtonSize.Regular)
                AppButton("시작하기", onClick = {}, size = AppButtonSize.Large)
                AppButton("진행 중", onClick = {}, loading = true)
                AppButton("비활성", onClick = {}, enabled = false)
                AppButton("아이콘", onClick = {}, leadingIcon = Icons.AutoMirrored.Outlined.ArrowForward)
            }
        }
    }
}

@Preview(name = "AppButton — Variants")
@Composable
private fun PreviewAppButtonVariants() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton("Primary", onClick = {}, variant = AppButtonVariant.Primary)
                AppButton("Secondary", onClick = {}, variant = AppButtonVariant.Secondary)
                AppButton("Tertiary", onClick = {}, variant = AppButtonVariant.Tertiary)
                AppButton(
                    "삭제",
                    onClick = {},
                    variant = AppButtonVariant.Destructive,
                    leadingIcon = Icons.Outlined.Delete,
                )
            }
        }
    }
}
