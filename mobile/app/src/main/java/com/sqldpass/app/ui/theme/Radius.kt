package com.sqldpass.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 공통 corner radius 토큰 — iOS Radius.swift 와 1:1 동일 값.
 *
 * 단일 진실 원천: ios/Sqldpass/Core/DesignSystem/Radius.swift
 * 본 파일은 미러. 값 변경 시 양 플랫폼을 함께 바꾼다.
 *
 * 사용 원칙:
 *  - 버튼/인풋: sm
 *  - 카드: lg
 *  - 칩/뱃지: full
 */
object SqldRadius {
    val sm = 6.dp    // 버튼, 인풋 (Supabase 사양)
    val md = 8.dp
    val lg = 16.dp   // 카드 (Supabase 사양)
    val xl = 16.dp
    val xxl = 20.dp
    val full = 9999.dp
}
