package com.sqldpass.app.ui.passplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

private data class CatalogEntry(
    val productId: String,
    val planLabel: String,
    val durationLabel: String,
    val benefits: List<String>,
)

private val DEFAULT_CATALOG = listOf(
    CatalogEntry(
        productId = "iap_three_day",
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
    ),
    CatalogEntry(
        productId = "iap_unlimited",
        planLabel = "All Pass",
        durationLabel = "평생",
        benefits = listOf("Pro 의 모든 혜택", "평생 무제한", "출시 후 추가 기능 포함"),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기")
            }
            Text(
                "PASS+",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "더 깊은 학습을 위한 4가지 플랜",
                    style = MaterialTheme.typography.headlineSmall,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionCard(sub: SubscriptionResponse) {
    val highlight = sub.active
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (sub.active) "활성 구독 — ${sub.plan ?: "PASS+"}"
                else "활성 구독 없음",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (sub.active) {
                sub.expiresAt?.let {
                    Text("만료: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    buildString {
                        if (sub.removesAds) append("광고 제거 · ")
                        if (sub.allowsPremium) append("프리미엄 · ")
                        if (sub.allowsPdf) append("PDF · ")
                        if (sub.hasLibraryAccess) append("라이브러리 · ")
                    }.trimEnd(' ', '·', ' ').ifBlank { "활성" },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
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
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(entry.planLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        entry.durationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formattedPrice,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            entry.benefits.forEach { benefit ->
                Text(
                    "· $benefit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = onPurchase,
                enabled = available,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp),
            ) { Text(if (available) "구매" else "로드 중…") }
        }
    }
}
