package com.sqldpass.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.R
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 문어 마스코트. 6 pose. 등장 시 1회 0.92→1.04→1.00 스케일 (medium bouncy spring, ~520ms).
 *
 * **금지**: 무한 반복 호흡, opacity pulse, floating bobble. 등장 한 번만 살짝 튀어오른다.
 */
enum class AppMascotPose { Greeting, Focus, Celebrate, Review, Guide, Onboarding }

@Composable
fun AppMascot(
    pose: AppMascotPose,
    sizeDp: Int = 64,
    modifier: Modifier = Modifier,
    animateOnAppear: Boolean = true,
) {
    val drawable = when (pose) {
        AppMascotPose.Greeting -> R.drawable.mascot_greeting
        AppMascotPose.Focus -> R.drawable.mascot_focus
        AppMascotPose.Celebrate -> R.drawable.mascot_focus
        AppMascotPose.Review -> R.drawable.mascot_review
        AppMascotPose.Guide -> R.drawable.mascot_guide
        AppMascotPose.Onboarding -> R.drawable.mascot_onboarding
    }

    val scaleAnim = remember { Animatable(if (animateOnAppear) 0.92f else 1f) }
    LaunchedEffect(pose, animateOnAppear) {
        if (animateOnAppear) {
            // 1회 살짝 오버슈트 후 안정. spring 의 mediumBouncy 가 0.92 → 1.04 → 1.00 형태로 마무리한다.
            scaleAnim.snapTo(0.92f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else {
            scaleAnim.snapTo(1f)
        }
    }

    Image(
        painter = painterResource(id = drawable),
        contentDescription = pose.name,
        modifier = modifier
            .size(sizeDp.dp)
            .scale(scaleAnim.value),
    )
}

@Preview(name = "AppMascot — all poses @96dp")
@Composable
private fun PreviewAppMascotAllPoses() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
                    AppMascot(pose = AppMascotPose.Greeting, sizeDp = 96)
                    AppMascot(pose = AppMascotPose.Focus, sizeDp = 96)
                    AppMascot(pose = AppMascotPose.Celebrate, sizeDp = 96)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
                    AppMascot(pose = AppMascotPose.Review, sizeDp = 96)
                    AppMascot(pose = AppMascotPose.Guide, sizeDp = 96)
                    AppMascot(pose = AppMascotPose.Onboarding, sizeDp = 96)
                }
            }
        }
    }
}
