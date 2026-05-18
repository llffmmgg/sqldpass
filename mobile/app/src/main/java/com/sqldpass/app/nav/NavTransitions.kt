package com.sqldpass.app.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * NavHost 의 enter/exit transition 모음.
 *
 * - 5탭 사이: fade through 180ms (위계 동등, 빠른 전환)
 * - 풀스크린(Runner / PassPlus) 진입: 슬라이드 인 오른쪽→왼쪽 + 페이드 280ms (iOS push 톤)
 * - 풀스크린 뒤로가기: 슬라이드 아웃 오른쪽으로 (대칭)
 *
 * Material 표준 280ms / 페이드 220ms.
 */

private const val FADE_DUR = 180
private const val SLIDE_DUR = 280
private const val SLIDE_FADE_DUR = 220

fun tabFadeEnter(): EnterTransition = fadeIn(animationSpec = tween(FADE_DUR))

fun tabFadeExit(): ExitTransition = fadeOut(animationSpec = tween(FADE_DUR))

/** 풀스크린 등장: 오른쪽에서 왼쪽으로 슬라이드 + 페이드. */
fun pushSlideEnter(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(SLIDE_DUR),
        initialOffsetX = { fullWidth -> fullWidth },
    ) + fadeIn(animationSpec = tween(SLIDE_FADE_DUR))

/** 풀스크린 위에서 다른 곳으로 갈 때(드문 케이스): 살짝 왼쪽으로 + 페이드. */
fun pushSlideExitForward(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(SLIDE_DUR),
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeOut(animationSpec = tween(SLIDE_FADE_DUR))

/** 풀스크린 뒤로가기(pop): 다시 오른쪽으로 슬라이드 아웃. */
fun pushSlidePopExit(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(SLIDE_DUR),
        targetOffsetX = { fullWidth -> fullWidth },
    ) + fadeOut(animationSpec = tween(SLIDE_FADE_DUR))
