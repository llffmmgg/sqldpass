package com.sqldpass.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

/**
 * 홈 자격증 6종 수평 캐러셀 — 카드 탭 시 onCertTap(slug) 호출.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1 / § 4 의 8·13번 규칙.
 */
@Composable
fun CertCarousel(
    onCertTap: (CertInfo) -> Unit,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "자격증 둘러보기",
            style = MaterialTheme.typography.titleMedium,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(CERT_CATALOG, key = { it.slug }) { info ->
                CertCard(
                    info = info,
                    dotColor = certColorOf(info.slug, cert),
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
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
        modifier = Modifier.width(180.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                Text(info.label, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                info.shortDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${info.questionCount}문 · ${info.durationLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
