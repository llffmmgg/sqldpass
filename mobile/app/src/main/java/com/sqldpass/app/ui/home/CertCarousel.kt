package com.sqldpass.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 홈 자격증 6종 수평 캐러셀 — 카드 탭 시 onCertTap(slug) 호출.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1 / § 4 의 8·13번 규칙.
 */
@Composable
fun CertCarousel(
    onCertTap: (CertInfo) -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val cert = LocalSqldpassSemanticColors.current.cert
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        Text(
            "자격증 둘러보기",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm + 2.dp),
            contentPadding = PaddingValues(vertical = SqldSpacing.xs),
        ) {
            items(CERT_CATALOG, key = { it.slug }) { info ->
                CertCard(
                    info = info,
                    dotColor = certColorOf(info.slug, cert),
                    accent = certAccentOf(info.slug),
                    onClick = { onCertTap(info) },
                )
            }
        }
    }
}

@Composable
private fun CertCard(
    info: CertInfo,
    dotColor: androidx.compose.ui.graphics.Color,
    accent: AppCardAccent,
    onClick: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    AppCard(
        surface = AppCardSurface.Card,
        accent = accent,
        onClick = onClick,
        modifier = Modifier.width(180.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            Box(modifier = Modifier.size(SqldSpacing.sm).background(dotColor, CircleShape))
            Text(
                info.label,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
        }
        Text(
            info.shortDesc,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
        )
        Text(
            "${info.questionCount}문 · ${info.durationLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
        )
    }
}

private fun certAccentOf(slug: String): AppCardAccent = when (slug) {
    "sqld" -> AppCardAccent.Sqld
    "engineer" -> AppCardAccent.EngineerPractical
    "engineer-written" -> AppCardAccent.EngineerWritten
    "computer-literacy-1" -> AppCardAccent.Cl1
    "computer-literacy-2" -> AppCardAccent.Cl2
    "adsp" -> AppCardAccent.Adsp
    else -> AppCardAccent.Sqld
}
