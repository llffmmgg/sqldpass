package com.sqldpass.app.ui.solve.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.SoloMarkdownContent
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import kotlin.math.roundToInt

/**
 * 단일 채점 풀이의 옵션 1개.
 *
 * 5가지 시각 상태:
 *  - Idle: 미답
 *  - Selected: 선택됨 (revealed 전) — press 0.97 + bounce 1.04
 *  - RevealedCorrect: 정답 공개 — 정답 옵션 (success border + ✓)
 *  - RevealedSelectedWrong: 선택한 오답 (danger border + ✗ + shake-x)
 *  - RevealedOther: 정답 공개 시 선택 안 한 옵션 (opacity 0.5)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SolveOptionRow(
    optionNumber: Int,
    optionText: String?,
    selected: Boolean,
    revealed: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val semantic = LocalSqldpassSemanticColors.current
    val accent = MaterialTheme.colorScheme.primary
    val success = semantic.state.success
    val danger = semantic.state.danger
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface

    val visual = when {
        revealed && isCorrectOption -> Visual.RevealedCorrect
        revealed && selected && !isCorrectOption -> Visual.RevealedSelectedWrong
        revealed -> Visual.RevealedOther
        selected -> Visual.Selected
        else -> Visual.Idle
    }

    val borderColor by animateColorAsState(
        targetValue = when (visual) {
            Visual.Idle, Visual.RevealedOther -> outline
            Visual.Selected -> accent
            Visual.RevealedCorrect -> success
            Visual.RevealedSelectedWrong -> danger
        },
        animationSpec = tween(durationMillis = 200),
        label = "option-border",
    )
    val containerColor by animateColorAsState(
        targetValue = when (visual) {
            Visual.RevealedCorrect -> success.copy(alpha = 0.12f)
            Visual.RevealedSelectedWrong -> danger.copy(alpha = 0.12f)
            Visual.Selected -> accent.copy(alpha = 0.10f)
            else -> surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "option-bg",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visual == Visual.RevealedOther) 0.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "option-alpha",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !revealed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "option-press",
    )

    var bounceTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected, revealed) {
        if (selected && !revealed) bounceTrigger += 1
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
    LaunchedEffect(visual) {
        if (visual == Visual.RevealedSelectedWrong) {
            for (delta in listOf(4f, -4f, 3f, -3f, 0f)) {
                shakeOffset.animateTo(delta, tween(60))
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    val combinedScale = pressScale * bounceScale

    Card(
        modifier = Modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = contentAlpha
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { if (!revealed) onClick() },
                onDoubleClick = { if (!revealed) onDoubleClick() },
            ),
        shape = RoundedCornerShape(SqldRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = when (visual) {
                Visual.Selected,
                Visual.RevealedCorrect,
                Visual.RevealedSelectedWrong -> 2.dp
                else -> 1.dp
            },
            color = borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val barColor: Color = when (visual) {
                Visual.Selected -> accent
                Visual.RevealedCorrect -> success
                Visual.RevealedSelectedWrong -> danger
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(
                        if (visual != Visual.Idle && visual != Visual.RevealedOther) 6.dp else 4.dp,
                    )
                    .background(barColor),
            )
            Spacer(Modifier.size(SqldSpacing.sm))
            val circleBg = when (visual) {
                Visual.Selected -> accent
                Visual.RevealedCorrect -> success
                Visual.RevealedSelectedWrong -> danger
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val circleFg = when (visual) {
                Visual.Selected -> MaterialTheme.colorScheme.onPrimary
                Visual.RevealedCorrect -> Color.White
                Visual.RevealedSelectedWrong -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(circleBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$optionNumber",
                    style = MaterialTheme.typography.labelLarge,
                    color = circleFg,
                )
            }
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = SqldSpacing.sm),
            ) {
                if (optionText != null) {
                    SoloMarkdownContent(text = optionText, textSizeSp = 14f)
                } else {
                    Text("${optionNumber}번", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.size(SqldSpacing.sm))
            val trailingIcon = when (visual) {
                Visual.RevealedCorrect -> Icons.Outlined.CheckCircle
                Visual.RevealedSelectedWrong -> Icons.Outlined.Cancel
                Visual.Selected -> Icons.Outlined.CheckCircle
                else -> Icons.Outlined.RadioButtonUnchecked
            }
            val iconTint = when (visual) {
                Visual.Selected -> accent
                Visual.RevealedCorrect -> success
                Visual.RevealedSelectedWrong -> danger
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = trailingIcon,
                contentDescription = when (visual) {
                    Visual.Selected -> "선택됨"
                    Visual.RevealedCorrect -> "정답"
                    Visual.RevealedSelectedWrong -> "오답"
                    else -> "미선택"
                },
                tint = iconTint,
            )
            Spacer(Modifier.size(SqldSpacing.md))
        }
    }
}

private enum class Visual {
    Idle,
    Selected,
    RevealedCorrect,
    RevealedSelectedWrong,
    RevealedOther,
}
