package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldpassMonoText
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 작은 라벨 배지. SqldpassBadge (기존) 와 병행 — 신규 콜러는 AppBadge 사용.
 *
 * - Soft: base color 0.14 alpha bg + base text.
 * - Solid: base color bg + white/accentFg text.
 * - 숫자 전용 라벨이면 SqldpassMonoText.small, 한글 라벨이면 labelSmall.
 */
enum class AppBadgeTone {
    Accent,
    Sqld,
    EngineerPractical,
    EngineerWritten,
    Cl1,
    Cl2,
    Adsp,
    Success,
    Warning,
    Danger,
    Info,
    Neutral,
}

enum class AppBadgeVariant { Soft, Solid }

@Composable
fun AppBadge(
    label: String,
    tone: AppBadgeTone,
    variant: AppBadgeVariant = AppBadgeVariant.Soft,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val base = appBadgeBase(tone, palette)
    val bg = when (variant) {
        AppBadgeVariant.Soft -> base.copy(alpha = 0.14f)
        AppBadgeVariant.Solid -> base
    }
    val fg = when (variant) {
        AppBadgeVariant.Soft -> base
        AppBadgeVariant.Solid -> if (tone == AppBadgeTone.Accent) palette.accentFg else Color.White
    }
    val isPureNumeric = label.isNotEmpty() && label.all {
        it.isDigit() || it == '%' || it == '/' || it == '.' || it == ':'
    }
    val style = if (isPureNumeric) {
        SqldpassMonoText.small
    } else {
        MaterialTheme.typography.labelSmall
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.sm))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = style, color = fg)
    }
}

private fun appBadgeBase(tone: AppBadgeTone, palette: SqldpassPalette): Color = when (tone) {
    AppBadgeTone.Accent -> palette.accent
    AppBadgeTone.Sqld -> palette.certSqld
    AppBadgeTone.EngineerPractical -> palette.certEngineerPractical
    AppBadgeTone.EngineerWritten -> palette.certEngineerWritten
    AppBadgeTone.Cl1 -> palette.certCl1
    AppBadgeTone.Cl2 -> palette.certCl2
    AppBadgeTone.Adsp -> palette.certAdsp
    AppBadgeTone.Success -> palette.success
    AppBadgeTone.Warning -> palette.warning
    AppBadgeTone.Danger -> palette.danger
    AppBadgeTone.Info -> palette.info
    AppBadgeTone.Neutral -> palette.textSubtle
}

@Preview(name = "AppBadge — Tones (Soft)")
@Composable
private fun PreviewAppBadgeSoft() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBadge("기출", AppBadgeTone.Accent)
                    AppBadge("SQLD", AppBadgeTone.Sqld)
                    AppBadge("실기", AppBadgeTone.EngineerPractical)
                    AppBadge("필기", AppBadgeTone.EngineerWritten)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBadge("정답", AppBadgeTone.Success)
                    AppBadge("주의", AppBadgeTone.Warning)
                    AppBadge("오답", AppBadgeTone.Danger)
                    AppBadge("정보", AppBadgeTone.Info)
                    AppBadge("기본", AppBadgeTone.Neutral)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBadge("12/30", AppBadgeTone.Accent)
                    AppBadge("87%", AppBadgeTone.Success)
                }
            }
        }
    }
}

@Preview(name = "AppBadge — Tones (Solid)")
@Composable
private fun PreviewAppBadgeSolid() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBadge("기출", AppBadgeTone.Accent, AppBadgeVariant.Solid)
                    AppBadge("SQLD", AppBadgeTone.Sqld, AppBadgeVariant.Solid)
                    AppBadge("Cl1", AppBadgeTone.Cl1, AppBadgeVariant.Solid)
                    AppBadge("Cl2", AppBadgeTone.Cl2, AppBadgeVariant.Solid)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBadge("ADsP", AppBadgeTone.Adsp, AppBadgeVariant.Solid)
                    AppBadge("PASS+", AppBadgeTone.Success, AppBadgeVariant.Solid)
                    AppBadge("FAIL", AppBadgeTone.Danger, AppBadgeVariant.Solid)
                }
            }
        }
    }
}
