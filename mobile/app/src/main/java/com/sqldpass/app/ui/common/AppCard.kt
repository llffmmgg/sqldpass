package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassPalette
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Inked OMR 카드 컨테이너 — 16dp 라운드, 1px hairline border, 엘리베이션 없음.
 * accent != None 일 때 좌측 4dp 컬러 레일을 카드 내부에 그린다.
 * onClick 제공 시 클릭 인디케이션 (SqldpassIndication) 자동 적용.
 */
enum class AppCardSurface { Card, Elevated }

enum class AppCardAccent {
    None,
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
}

@Composable
fun AppCard(
    surface: AppCardSurface = AppCardSurface.Card,
    accent: AppCardAccent = AppCardAccent.None,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val bg = when (surface) {
        AppCardSurface.Card -> palette.card
        AppCardSurface.Elevated -> palette.elevated
    }
    val accentColor = appCardAccentColor(accent, palette)

    Box(
        modifier = modifier
            .shadow(
                elevation = when (surface) {
                    AppCardSurface.Card -> 1.dp
                    AppCardSurface.Elevated -> 3.dp
                },
                shape = RoundedCornerShape(SqldRadius.lg),
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(SqldRadius.lg))
            .background(bg)
            .border(
                BorderStroke(1.dp, palette.border),
                RoundedCornerShape(SqldRadius.lg),
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .height(IntrinsicSize.Min),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(accentColor),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SqldSpacing.base),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
                content = content,
            )
        }
    }
}

private fun appCardAccentColor(accent: AppCardAccent, palette: SqldpassPalette): Color? =
    when (accent) {
        AppCardAccent.None -> null
        AppCardAccent.Sqld -> palette.certSqld
        AppCardAccent.EngineerPractical -> palette.certEngineerPractical
        AppCardAccent.EngineerWritten -> palette.certEngineerWritten
        AppCardAccent.Cl1 -> palette.certCl1
        AppCardAccent.Cl2 -> palette.certCl2
        AppCardAccent.Adsp -> palette.certAdsp
        AppCardAccent.Success -> palette.success
        AppCardAccent.Warning -> palette.warning
        AppCardAccent.Danger -> palette.danger
        AppCardAccent.Info -> palette.info
    }

@Preview(name = "AppCard — Surfaces")
@Composable
private fun PreviewAppCardSurfaces() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(surface = AppCardSurface.Card) {
                    Text("Card surface", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(surface = AppCardSurface.Elevated) {
                    Text("Elevated surface", color = LocalSqldpassPalette.current.textPrimary)
                }
            }
        }
    }
}

@Preview(name = "AppCard — Cert accents")
@Composable
private fun PreviewAppCardCertAccents() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCard(accent = AppCardAccent.Sqld) {
                    Text("SQLD", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.EngineerPractical) {
                    Text("정보처리기사 실기", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.EngineerWritten) {
                    Text("정보처리기사 필기", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Cl1) {
                    Text("정보처리산업기사", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Cl2) {
                    Text("정보처리기능사", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Adsp) {
                    Text("ADsP", color = LocalSqldpassPalette.current.textPrimary)
                }
            }
        }
    }
}

@Preview(name = "AppCard — Semantic accents")
@Composable
private fun PreviewAppCardSemanticAccents() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCard(accent = AppCardAccent.Success) {
                    Text("성공", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Warning) {
                    Text("주의", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Danger) {
                    Text("위험", color = LocalSqldpassPalette.current.textPrimary)
                }
                AppCard(accent = AppCardAccent.Info) {
                    Text("정보", color = LocalSqldpassPalette.current.textPrimary)
                }
            }
        }
    }
}
