package com.sqldpass.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 공통 spacing 토큰 — iOS Spacing.swift 와 1:1 동일 값.
 *
 * 단일 진실 원천: ios/Sqldpass/Core/DesignSystem/Spacing.swift
 * 본 파일은 미러. 값 변경 시 양 플랫폼을 함께 바꾼다.
 *
 * 사용 원칙:
 *  - 모든 padding/spacing/spacedBy 매직 넘버는 본 객체 참조로 치환.
 *  - 새 값이 필요해 보이면 먼저 기존 토큰 안에서 해결 가능한지 검토.
 */
object SqldSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}
