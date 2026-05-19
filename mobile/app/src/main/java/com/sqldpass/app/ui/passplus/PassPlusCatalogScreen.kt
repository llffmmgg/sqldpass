package com.sqldpass.app.ui.passplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.billing.BillingProductSnapshot
import com.sqldpass.app.data.SubscriptionResponse
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

private data class CatalogEntry(
    val productId: String,
    val planLabel: String,
    val durationLabel: String,
    val benefits: List<String>,
    val recommended: Boolean = false,
)

private val DEFAULT_CATALOG = listOf(
    CatalogEntry(
        productId = "iap_thunder",
        planLabel = "Thunder",
        durationLabel = "3일",
        benefits = listOf("프리미엄 회차 풀이", "광고 제거", "오답·즐겨찾기 무제한"),
    ),
    CatalogEntry(
        productId = "iap_focus",
        planLabel = "Focus",
        durationLabel = "30일",
        benefits = listOf("광고 제거", "오답 노트 전체 잠금 해제", "즐겨찾기 무제한"),
    ),
    CatalogEntry(
        productId = "iap_one_month",
        planLabel = "Pro",
        durationLabel = "30일",
        benefits = listOf("프리미엄 회차 + Focus 혜택", "PDF 다운로드"),
        recommended = true,
    ),
    CatalogEntry(
        productId = "iap_unlimited",
        planLabel = "All Pass",
        durationLabel = "6개월",
        benefits = listOf("Pro 의 모든 혜택", "6개월 PASS+ 무제한 풀이", "모의고사 PDF 다운로드"),
    ),
)

@Composable
fun PassPlusCatalogScreen(
    subscription: SubscriptionResponse?,
    products: List<BillingProductSnapshot>,
    onLoadProducts: () -> Unit,
    onLoadSubscription: () -> Unit,
    onPurchase: (productId: String) -> Unit,
    onClose: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onLoadProducts()
        onLoadSubscription()
    }
    val productsById = products.associateBy { it.productId }
    val palette = LocalSqldpassPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SqldSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "닫기",
                    tint = palette.textPrimary,
                )
            }
            Text(
                "PASS+",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = SqldSpacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "더 깊은 학습을 위한 4가지 플랜",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
            }
            if (subscription != null) {
                item { ActiveSubscriptionCard(subscription) }
            }
            items(DEFAULT_CATALOG) { entry ->
                val product = productsById[entry.productId]
                PlanCard(
                    entry = entry,
                    formattedPrice = product?.formattedPrice.takeIf { it?.isNotBlank() == true }
                        ?: "가격 정보 없음",
                    available = product != null,
                    onPurchase = { onPurchase(entry.productId) },
                )
            }
            if (products.isEmpty()) {
                item {
                    Text(
                        "Play Billing 상품 정보를 불러오는 중입니다…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionCard(sub: SubscriptionResponse) {
    val palette = LocalSqldpassPalette.current
    AppCard(
        surface = AppCardSurface.Card,
        accent = if (sub.active) AppCardAccent.Success else AppCardAccent.None,
    ) {
        Text(
            if (sub.active) "활성 구독 — ${sub.plan ?: "PASS+"}"
            else "활성 구독 없음",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        if (sub.active) {
            com.sqldpass.app.text.formatKstDateTime(sub.expiresAt)?.let {
                Text(
                    "만료: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                )
            }
            Text(
                buildString {
                    if (sub.removesAds) append("광고 제거 · ")
                    if (sub.allowsPremium) append("프리미엄 · ")
                    if (sub.allowsPdf) append("PDF · ")
                    if (sub.hasLibraryAccess) append("라이브러리 · ")
                }.trimEnd(' ', '·', ' ').ifBlank { "활성" },
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
        }
    }
}

@Composable
private fun PlanCard(
    entry: CatalogEntry,
    formattedPrice: String,
    available: Boolean,
    onPurchase: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val accent = palette.accent
    val borderModifier = if (entry.recommended) {
        Modifier.border(
            width = 2.dp,
            color = accent,
            shape = RoundedCornerShape(SqldRadius.lg),
        )
    } else Modifier
    AppCard(
        surface = AppCardSurface.Card,
        modifier = borderModifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        entry.planLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    if (entry.recommended) {
                        com.sqldpass.app.ui.common.SqldpassBadge(
                            label = "가장 인기",
                            base = accent,
                        )
                    }
                }
                Text(
                    entry.durationLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
            Text(
                formattedPrice,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
        }
        entry.benefits.forEach { benefit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    benefit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                )
            }
        }
        AppButton(
            text = if (available) "구매" else "로드 중…",
            onClick = onPurchase,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.Regular,
            enabled = available,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
