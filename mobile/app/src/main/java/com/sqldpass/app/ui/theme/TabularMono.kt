package com.sqldpass.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 점수, 타이머, 카운터 같은 숫자 전용 TextStyle.
 *
 * 시스템 monospace + `tnum` (tabular numbers) feature 강제. 글자가 흔들리지 않고
 * 점수가 바뀔 때 옆 글자가 튀지 않는다. 모든 숫자(N/10, MM:SS, % 등)는 이 스타일
 * 또는 변종을 사용한다 — 본문 한국어 텍스트는 Pretendard 유지.
 */
object SqldpassMonoText {
    private val mono: FontFamily = FontFamily.Monospace
    private const val TNUM = "tnum"

    val display: TextStyle = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontFeatureSettings = TNUM,
    )

    val large: TextStyle = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = TNUM,
    )

    val title: TextStyle = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = TNUM,
    )

    val body: TextStyle = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TNUM,
    )

    val small: TextStyle = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TNUM,
    )
}
