package com.sqldpass.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

/**
 * 빠른 연속 탭 디바운스 + 일관된 햅틱 피드백.
 * runner 시작·결제 같은 무거운 액션을 두 번 호출 방지.
 * window 동안 다시 누르면 무시.
 */
@Composable
fun rememberDebouncedClick(
    windowMillis: Long = 800,
    haptic: Boolean = true,
    onClick: () -> Unit,
): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    var blocked by remember { mutableStateOf(false) }
    LaunchedEffect(blocked) {
        if (blocked) {
            delay(windowMillis)
            blocked = false
        }
    }
    return {
        if (!blocked) {
            blocked = true
            if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}
