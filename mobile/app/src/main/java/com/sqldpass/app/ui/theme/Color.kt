package com.sqldpass.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand — emerald primary (frontend globals.css 의 --primary 와 동기화)
internal val Emerald500Light = Color(0xFF24B47E)  // 라이트 --primary
internal val Emerald400Dark  = Color(0xFF3ECF8E)  // 다크 --primary
internal val EmeraldHover    = Color(0xFF1FA374)  // 라이트 --primary-hover
internal val EmeraldSoftLight = Color(0xFFE6F7F0) // primary-soft 와 유사 (rgba(36,180,126,0.10) 의 카드 배경 톤)
internal val EmeraldDarkCta  = Color(0xFF006239)  // 다크 --cta-bg (Forest Call to Action)
internal val EmeraldDarkText = Color(0xFFA7F3D0)  // 다크 onPrimaryContainer

// 자격증·streak 보조 액센트 (frontend cert-sqld = #f59e0b)
internal val Amber50  = Color(0xFFFFFBEB)
internal val Amber100 = Color(0xFFFEF3C7)
internal val Amber300 = Color(0xFFFCD34D)
internal val Amber500 = Color(0xFFF59E0B)
internal val Amber700 = Color(0xFFB45309)
internal val Amber900 = Color(0xFF78350F)

// 자격증 cert palette — frontend globals.css:60-65 그대로 import.
internal val CertSqld = Amber500                       // #F59E0B
internal val CertEngineerPractical = Color(0xFF2DBB7A)
internal val CertEngineerWritten   = Color(0xFFF43F5E)
internal val CertCl1 = Color(0xFF0EA5E9)
internal val CertCl2 = Color(0xFF6366F1)
internal val CertAdsp = Color(0xFF14B8A6)

// State palette — frontend globals.css:41-44.
internal val StateSuccess = Color(0xFF00997A)
internal val StateWarning = Color(0xFFD97706)
internal val StateDanger  = Color(0xFFEF4444)
internal val StateInfo    = Color(0xFF0A84FF)

// Neutral / Supabase tone
internal val Neutral50  = Color(0xFFFAFAFA)
internal val Zinc100  = Color(0xFFF4F4F5)
internal val Zinc200  = Color(0xFFE5E7EB)
internal val Zinc400  = Color(0xFFA1A1AA)
internal val Zinc500  = Color(0xFF737373)
internal val Zinc700  = Color(0xFF3F3F46)
internal val Zinc800  = Color(0xFF1F1F22)
internal val Zinc900  = Color(0xFF111113)
internal val Zinc950  = Color(0xFF0A0A0B)

// Semantic
internal val PassLightBg     = Color(0xFFDCFCE7)
internal val PassLightText   = Color(0xFF166534)
internal val PassDarkBg      = Color(0xFF14532D)
internal val PassDarkText    = Color(0xFFBBF7D0)
internal val FailLightBg     = Color(0xFFFFE4E6)
internal val FailLightText   = Color(0xFF9F1239)
internal val FailDarkBg      = Color(0xFF4C0519)
internal val FailDarkText    = Color(0xFFFECDD3)
internal val Red700 = Color(0xFFB91C1C)
internal val Red300 = Color(0xFFF87171)
