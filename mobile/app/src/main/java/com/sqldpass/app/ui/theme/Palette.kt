package com.sqldpass.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Inked OMR 디자인 시스템의 단일 색 진실 원천.
 *
 * 모든 feature 코드는 `MaterialTheme.colorScheme.surface/onSurface*` 대신
 * `LocalSqldpassPalette.current.*` 만 읽는다. Material3 가 다음 버전에서 토큰을
 * 바꿔도 우리 디자인은 변하지 않게 하기 위한 격리층.
 *
 * Theme.kt 가 light/dark 분기로 인스턴스를 만들어 제공한다.
 */
data class SqldpassPalette(
    // 3-tier surface (Supabase flat — 색 대비만으로 elevation 표현)
    val page: Color,
    val card: Color,
    val elevated: Color,
    // borders
    val border: Color,
    val borderStrong: Color,
    // text
    val textPrimary: Color,
    val textMuted: Color,
    val textSubtle: Color,
    // brand
    val accent: Color,
    val accentHover: Color,
    val accentFg: Color,
    val accentSoftBg: Color, // 선택된 옵션/칩 배경. 0.08~0.12 alpha 캡. AI 흐릿 효과 아님 — 선택 시그널.
    // semantic
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val successSoftBg: Color,
    val dangerSoftBg: Color,
    // cert 6
    val certSqld: Color,
    val certEngineerPractical: Color,
    val certEngineerWritten: Color,
    val certCl1: Color,
    val certCl2: Color,
    val certAdsp: Color,
)

internal fun lightPalette(): SqldpassPalette = SqldpassPalette(
    page = Color(0xFFFAFAFA),
    card = Color(0xFFFFFFFF),
    elevated = Color(0xFFF4F4F4),
    border = Color(0xFFE5E5E5),
    borderStrong = Color(0xFFC4C4C4),
    textPrimary = Color(0xFF181818),
    textMuted = Color(0xFF666666),
    textSubtle = Color(0xFF8A8A8A),
    accent = Emerald500Light,
    accentHover = EmeraldHover,
    accentFg = Color(0xFFFFFFFF),
    accentSoftBg = Color(0x1424B47E), // ~0.08
    success = StateSuccess,
    warning = StateWarning,
    danger = StateDanger,
    info = StateInfo,
    successSoftBg = Color(0x1F00997A), // ~0.12
    dangerSoftBg = Color(0x1FEF4444),
    certSqld = CertSqld,
    certEngineerPractical = CertEngineerPractical,
    certEngineerWritten = CertEngineerWritten,
    certCl1 = CertCl1,
    certCl2 = CertCl2,
    certAdsp = CertAdsp,
)

internal fun darkPalette(): SqldpassPalette = SqldpassPalette(
    page = Color(0xFF121212),
    card = Color(0xFF1A1A1A),
    elevated = Color(0xFF242424),
    border = Color(0xFF393939),
    borderStrong = Color(0xFF4D4D4D),
    textPrimary = Color(0xFFFAFAFA),
    textMuted = Color(0xFFB4B4B4),
    textSubtle = Color(0xFF898989),
    accent = Emerald400Dark,
    accentHover = Color(0xFF00C573),
    accentFg = Color(0xFFFAFAFA),
    accentSoftBg = Color(0x1A3ECF8E), // ~0.10
    success = Color(0xFF00B8A3),
    warning = Color(0xFFFFB800),
    danger = Color(0xFFF63737),
    info = StateInfo,
    successSoftBg = Color(0x1F00B8A3),
    dangerSoftBg = Color(0x1FF63737),
    certSqld = CertSqld,
    certEngineerPractical = CertEngineerPractical,
    certEngineerWritten = CertEngineerWritten,
    certCl1 = CertCl1,
    certCl2 = CertCl2,
    certAdsp = CertAdsp,
)

val LocalSqldpassPalette = staticCompositionLocalOf { darkPalette() }
