package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 자격증 식별 pill 배지. 6종 cert × 3 size.
 *
 * - bg: cert color * alpha 0.14
 * - border: 1dp cert color
 * - text: cert color
 * - 솔리드 dot 옵션은 기본 OFF (호출자가 별도로 표시할 수 있도록 빠짐).
 */
enum class AppCert(val slug: String, val label: String) {
    Sqld("SQLD", "SQLD"),
    EngineerPractical("EngineerPractical", "정처기 실기"),
    EngineerWritten("EngineerWritten", "정처기 필기"),
    Cl1("Cl1", "컴활 1급"),
    Cl2("Cl2", "컴활 2급"),
    Adsp("Adsp", "ADsP"),
}

enum class AppCertBadgeSize { Small, Medium, Large }

@Composable
fun AppCertBadge(
    cert: AppCert,
    modifier: Modifier = Modifier,
    size: AppCertBadgeSize = AppCertBadgeSize.Medium,
) {
    val palette = LocalSqldpassPalette.current
    val color = certPaletteColor(palette, cert)
    val minHeight = when (size) {
        AppCertBadgeSize.Small -> 24.dp
        AppCertBadgeSize.Medium -> 32.dp
        AppCertBadgeSize.Large -> 44.dp
    }
    val horizontal: Dp = when (size) {
        AppCertBadgeSize.Small -> 8.dp
        AppCertBadgeSize.Medium -> 12.dp
        AppCertBadgeSize.Large -> 16.dp
    }
    val vertical: Dp = when (size) {
        AppCertBadgeSize.Small -> 2.dp
        AppCertBadgeSize.Medium -> 4.dp
        AppCertBadgeSize.Large -> 8.dp
    }
    val style: TextStyle = when (size) {
        AppCertBadgeSize.Small -> MaterialTheme.typography.labelSmall
        AppCertBadgeSize.Medium -> MaterialTheme.typography.labelLarge
        AppCertBadgeSize.Large -> MaterialTheme.typography.titleMedium
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.full))
            .background(color.copy(alpha = 0.14f))
            .border(
                BorderStroke(1.dp, color),
                RoundedCornerShape(SqldRadius.full),
            )
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = horizontal, vertical = vertical),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cert.label,
            style = style,
            color = color,
        )
    }
}

fun certPaletteColor(palette: SqldpassPalette, cert: AppCert): Color = when (cert) {
    AppCert.Sqld -> palette.certSqld
    AppCert.EngineerPractical -> palette.certEngineerPractical
    AppCert.EngineerWritten -> palette.certEngineerWritten
    AppCert.Cl1 -> palette.certCl1
    AppCert.Cl2 -> palette.certCl2
    AppCert.Adsp -> palette.certAdsp
}

@Preview(name = "AppCertBadge — all certs × sizes")
@Composable
private fun PreviewAppCertBadgeMatrix() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCert.values().forEach { cert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
                    ) {
                        AppCertBadge(cert = cert, size = AppCertBadgeSize.Small)
                        AppCertBadge(cert = cert, size = AppCertBadgeSize.Medium)
                        AppCertBadge(cert = cert, size = AppCertBadgeSize.Large)
                    }
                }
            }
        }
    }
}
