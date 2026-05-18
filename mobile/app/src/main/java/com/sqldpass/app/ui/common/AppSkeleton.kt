package com.sqldpass.app.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius

/**
 * Inked OMR 스켈레톤 placeholder.
 *
 * Supabase 톤 — sweeping gradient/shimmer 바 없음. `palette.elevated` 와
 * 그 alpha 0.5 변형 사이의 1100ms ease-in-out alpha fade.
 *
 * 사이즈는 호출자 modifier 가 결정한다 (예: `Modifier.fillMaxWidth().height(120.dp)`).
 */
@Composable
fun AppSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SqldRadius.md),
) {
    val palette = LocalSqldpassPalette.current
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "app-skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-fade",
    )
    // 두 톤 사이 선형 보간 — sweeping gradient/shimmer 없음.
    val base = palette.elevated
    val faded = palette.elevated.copy(alpha = 0.5f)
    val bg = lerpColor(base, faded, progress)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg),
    )
}

/**
 * Card 모양의 편의 스켈레톤 — AppCard 와 비슷한 surface/border 로 120dp 박스.
 */
@Composable
fun AppSkeletonCard(
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "app-skeleton-card")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-card-fade",
    )
    val base = palette.elevated
    val faded = palette.elevated.copy(alpha = 0.5f)
    val bg = lerpColor(base, faded, progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(SqldRadius.lg))
            .background(palette.card)
            .border(
                BorderStroke(1.dp, palette.border),
                RoundedCornerShape(SqldRadius.lg),
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(SqldRadius.md))
                .background(bg),
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t,
    )
}

@Preview(name = "AppSkeleton — variants")
@Composable
private fun PreviewAppSkeleton() {
    com.sqldpass.app.ui.theme.SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                )
                AppSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
                AppSkeletonCard()
            }
        }
    }
}
