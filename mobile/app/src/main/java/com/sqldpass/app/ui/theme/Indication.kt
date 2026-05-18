package com.sqldpass.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * Inked OMR 의 누름 인디케이션. Material3 의 radial ripple 을 대체.
 *
 * 디자인 의도: ripple 의 wave 가 Google Material 색을 진하게 풍기고
 * "flat + hairline" 정체성을 깬다. 대신 0.97 스케일 + 4% white inner wash 로
 * 단단하고 즉각적인 누름감만 표현한다. 접근성 포커스 링은 별도 컴포넌트가 관리.
 *
 * 사용:
 *  - 전역 주입: `CompositionLocalProvider(LocalIndication provides SqldpassIndication)`
 *  - 직접 지정: `Modifier.clickable(interactionSource, indication = SqldpassIndication, ...)`
 */
object SqldpassIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        SqldpassIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

private class SqldpassIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val animatedScale = Animatable(1f)
    private val animatedAlpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        launch {
                            animatedScale.animateTo(
                                targetValue = 0.97f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                        launch {
                            animatedAlpha.animateTo(
                                targetValue = 0.04f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                            )
                        }
                    }
                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> {
                        launch {
                            animatedScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                        launch {
                            animatedAlpha.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = animatedScale.value
        scale(s, s, pivot = center) {
            this@draw.drawContent()
        }
        val a = animatedAlpha.value
        if (a > 0f) {
            drawRect(Color.White.copy(alpha = a))
        }
    }
}
