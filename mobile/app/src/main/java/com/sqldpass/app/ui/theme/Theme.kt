package com.sqldpass.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors: ColorScheme = lightColorScheme(
    primary = Amber500,
    onPrimary = Zinc950,
    primaryContainer = Amber50,
    onPrimaryContainer = Amber900,
    secondary = Zinc700,
    onSecondary = Color.White,
    background = Neutral50,
    onBackground = Zinc900,
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc500,
    outline = Zinc200,
    outlineVariant = Zinc200,
    error = Red700,
    onError = Color.White,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Amber300,
    onPrimary = Zinc950,
    primaryContainer = Color(0xFF3F2C0A),
    onPrimaryContainer = Amber300,
    secondary = Zinc400,
    onSecondary = Zinc950,
    background = Color.Black,
    onBackground = Color(0xFFFAFAFA),
    surface = Zinc950,
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Zinc900,
    onSurfaceVariant = Zinc400,
    outline = Zinc800,
    outlineVariant = Zinc800,
    error = Red300,
    onError = Zinc950,
)

/**
 * 합격 배너처럼 의미가 있는 색은 colorScheme 에 매핑이 어색해 별도 토큰으로 노출.
 * darkTheme 에 따라 톤이 갈리므로 컴포지션 단계에서 결정해 내려준다.
 */
data class SqldpassSemanticColors(
    val passBg: Color,
    val passText: Color,
    val failBg: Color,
    val failText: Color,
    val highlightBg: Color,
)

val LocalSqldpassSemanticColors = staticCompositionLocalOf {
    SqldpassSemanticColors(
        passBg = PassLightBg,
        passText = PassLightText,
        failBg = FailLightBg,
        failText = FailLightText,
        highlightBg = Amber50,
    )
}

@Composable
fun SqldpassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val semantic = if (darkTheme) {
        SqldpassSemanticColors(
            passBg = PassDarkBg,
            passText = PassDarkText,
            failBg = FailDarkBg,
            failText = FailDarkText,
            highlightBg = Color(0xFF3F2C0A),
        )
    } else {
        SqldpassSemanticColors(
            passBg = PassLightBg,
            passText = PassLightText,
            failBg = FailLightBg,
            failText = FailLightText,
            highlightBg = Amber50,
        )
    }
    CompositionLocalProvider(LocalSqldpassSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SqldpassTypography,
            content = content,
        )
    }
}
