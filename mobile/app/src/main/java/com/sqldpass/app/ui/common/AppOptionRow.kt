package com.sqldpass.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassMonoText
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme
import kotlin.math.roundToInt

/**
 * OMR 옵션 행 — Inked OMR 의 시그니처 컴포넌트.
 *
 * 5가지 시각 상태:
 *  - Idle: 미답
 *  - Selected: 선택됨 (revealed 전) — bounce 1.04 + press 0.97
 *  - Revealed(isCorrect = true, wasSelected = *): 정답 옵션 (success border + ✓)
 *  - Revealed(isCorrect = false, wasSelected = true): 선택한 오답 (danger border + ✗ + shake-x)
 *  - Revealed(isCorrect = false, wasSelected = false): 정답 공개 시 선택 안 한 다른 옵션 (alpha 0.5)
 *
 * 디자인 원칙:
 *  - 옵션 텍스트는 maxLines 없음. 줄 바꿈 무한 허용. (문제 가독성 최우선)
 *  - 본문 16sp, line-height 22sp.
 *  - 색은 LocalSqldpassPalette 만 사용 — Material3.colorScheme.* 사용 금지.
 *  - 모든 transition 은 state-change trigger 가 있을 때만. idle pulse / breathing 금지.
 */
sealed class AppOptionState {
    object Idle : AppOptionState()
    object Selected : AppOptionState()
    data class Revealed(val isCorrect: Boolean, val wasSelected: Boolean) : AppOptionState()
}

/**
 * boolean 3개로부터 AppOptionState 도출. 기존 SolveOptionRow API 호환을 돕는 헬퍼.
 */
fun appOptionStateOf(
    selected: Boolean,
    revealed: Boolean,
    isCorrectOption: Boolean,
): AppOptionState = when {
    revealed -> AppOptionState.Revealed(isCorrect = isCorrectOption, wasSelected = selected)
    selected -> AppOptionState.Selected
    else -> AppOptionState.Idle
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppOptionRow(
    optionNumber: Int,
    optionText: String?,
    state: AppOptionState,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current

    val borderColor by animateColorAsState(
        targetValue = optionBorderColor(state, palette),
        animationSpec = tween(durationMillis = 200),
        label = "option-border",
    )
    val containerColor by animateColorAsState(
        targetValue = optionContainerColor(state, palette),
        animationSpec = tween(durationMillis = 200),
        label = "option-bg",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (state is AppOptionState.Revealed && !state.isCorrect && !state.wasSelected) 0.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "option-alpha",
    )
    val borderWidth = when (state) {
        AppOptionState.Idle -> 1.dp
        AppOptionState.Selected -> 2.dp
        is AppOptionState.Revealed -> if (state.isCorrect || state.wasSelected) 2.dp else 1.dp
    }

    val isRevealed = state is AppOptionState.Revealed
    val isClickable = !isRevealed

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isClickable) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "option-press",
    )

    var bounceTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        if (state is AppOptionState.Selected) bounceTrigger += 1
    }
    val bounceScale by animateFloatAsState(
        targetValue = if (bounceTrigger > 0) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        finishedListener = { bounceTrigger = 0 },
        label = "option-bounce",
    )

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state is AppOptionState.Revealed && !state.isCorrect && state.wasSelected) {
            for (delta in listOf(4f, -4f, 3f, -3f, 0f)) {
                shakeOffset.animateTo(delta, tween(60))
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    val combinedScale = pressScale * bounceScale

    val railWidth by animateDpAsState(
        targetValue = when (state) {
            AppOptionState.Idle -> 4.dp
            AppOptionState.Selected -> 6.dp
            is AppOptionState.Revealed -> if (state.isCorrect || state.wasSelected) 6.dp else 4.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "option-rail-width",
    )

    val railColor = optionRailColor(state, palette)
    val stampBg = optionStampBg(state, palette)
    val stampFg = optionStampFg(state, palette)
    val (trailingIcon, trailingTint) = optionTrailing(state, palette)

    Box(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = contentAlpha
            }
            .clip(RoundedCornerShape(SqldRadius.lg))
            .background(containerColor)
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(SqldRadius.lg))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                enabled = isClickable,
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            )
            .defaultMinSize(minHeight = 56.dp)
            .height(IntrinsicSize.Min),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(railWidth)
                    .background(railColor),
            )
            Spacer(Modifier.size(SqldSpacing.sm))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(stampBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    optionNumber.toString(),
                    style = SqldpassMonoText.body,
                    color = stampFg,
                )
            }
            Spacer(Modifier.size(SqldSpacing.sm + 2.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = SqldSpacing.sm),
            ) {
                if (optionText != null) {
                    AppQuestionContent(
                        text = optionText,
                        textSizeSp = 16f,
                        codeBlockSurface = AppCodeBlockSurface.Bare,
                    )
                } else {
                    Text(
                        "${optionNumber}번",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                    )
                }
            }
            Spacer(Modifier.size(SqldSpacing.sm))
            Icon(
                imageVector = trailingIcon,
                contentDescription = optionTrailingDescription(state),
                tint = trailingTint,
            )
            Spacer(Modifier.size(SqldSpacing.md))
        }
    }
}

private fun optionBorderColor(state: AppOptionState, palette: SqldpassPalette): Color = when (state) {
    AppOptionState.Idle -> palette.border
    AppOptionState.Selected -> palette.accent
    is AppOptionState.Revealed -> when {
        state.isCorrect -> palette.success
        state.wasSelected -> palette.danger
        else -> palette.border
    }
}

private fun optionContainerColor(state: AppOptionState, palette: SqldpassPalette): Color = when (state) {
    AppOptionState.Idle -> palette.card
    AppOptionState.Selected -> palette.accentSoftBg
    is AppOptionState.Revealed -> when {
        state.isCorrect -> palette.successSoftBg
        state.wasSelected -> palette.dangerSoftBg
        else -> palette.card
    }
}

private fun optionRailColor(state: AppOptionState, palette: SqldpassPalette): Color = when (state) {
    AppOptionState.Idle -> Color.Transparent
    AppOptionState.Selected -> palette.accent
    is AppOptionState.Revealed -> when {
        state.isCorrect -> palette.success
        state.wasSelected -> palette.danger
        else -> Color.Transparent
    }
}

private fun optionStampBg(state: AppOptionState, palette: SqldpassPalette): Color = when (state) {
    AppOptionState.Idle -> palette.elevated
    AppOptionState.Selected -> palette.accent
    is AppOptionState.Revealed -> when {
        state.isCorrect -> palette.success
        state.wasSelected -> palette.danger
        else -> palette.elevated
    }
}

private fun optionStampFg(state: AppOptionState, palette: SqldpassPalette): Color = when (state) {
    AppOptionState.Idle -> palette.textMuted
    AppOptionState.Selected -> palette.accentFg
    is AppOptionState.Revealed -> when {
        state.isCorrect -> Color.White
        state.wasSelected -> Color.White
        else -> palette.textMuted
    }
}

private fun optionTrailing(
    state: AppOptionState,
    palette: SqldpassPalette,
): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> = when (state) {
    AppOptionState.Idle -> Icons.Outlined.RadioButtonUnchecked to palette.textMuted
    AppOptionState.Selected -> Icons.Outlined.CheckCircle to palette.accent
    is AppOptionState.Revealed -> when {
        state.isCorrect -> Icons.Outlined.CheckCircle to palette.success
        state.wasSelected -> Icons.Outlined.Cancel to palette.danger
        else -> Icons.Outlined.RadioButtonUnchecked to palette.textMuted
    }
}

private fun optionTrailingDescription(state: AppOptionState): String = when (state) {
    AppOptionState.Idle -> "미선택"
    AppOptionState.Selected -> "선택됨"
    is AppOptionState.Revealed -> when {
        state.isCorrect -> "정답"
        state.wasSelected -> "오답"
        else -> "미선택"
    }
}

@Preview(name = "AppOptionRow — 5 states")
@Composable
private fun PreviewAppOptionRow() {
    SqldpassTheme(darkTheme = true) {
        val palette = LocalSqldpassPalette.current
        Box(Modifier.background(palette.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                AppOptionRow(
                    optionNumber = 1,
                    optionText = "트랜잭션의 격리수준이 READ COMMITTED 일 때 발생할 수 있는 현상을 모두 고르시오.",
                    state = AppOptionState.Idle,
                    onClick = {},
                )
                AppOptionRow(
                    optionNumber = 2,
                    optionText = "Dirty Read 는 발생하지 않지만 Non-Repeatable Read 는 발생할 수 있다.",
                    state = AppOptionState.Selected,
                    onClick = {},
                )
                AppOptionRow(
                    optionNumber = 3,
                    optionText = "이 옵션은 정답입니다. 정답 공개 시 success 컬러로 강조됩니다.",
                    state = AppOptionState.Revealed(isCorrect = true, wasSelected = false),
                    onClick = {},
                )
                AppOptionRow(
                    optionNumber = 4,
                    optionText = "이 옵션은 선택했지만 오답이었습니다. shake-x 애니메이션이 한 번 재생됩니다.",
                    state = AppOptionState.Revealed(isCorrect = false, wasSelected = true),
                    onClick = {},
                )
                AppOptionRow(
                    optionNumber = 5,
                    optionText = "이 옵션은 선택하지 않은 다른 옵션이라 alpha 0.5 로 흐려집니다.",
                    state = AppOptionState.Revealed(isCorrect = false, wasSelected = false),
                    onClick = {},
                )
            }
        }
    }
}
