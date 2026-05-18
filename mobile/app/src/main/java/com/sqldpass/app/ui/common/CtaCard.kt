package com.sqldpass.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 라운드 카드 컨테이너 + 풀폭 primary CTA. 잠금 시 우상단 잠금 아이콘 + 버튼 비활성.
 *
 * Step 4 그룹 D: 내부를 AppCard(Elevated) + AppButton 으로 통합.
 * 호출 API 는 그대로 유지 (모의/기출/대시보드/인사이트/오답 노트가 사용 중).
 */
@Composable
fun CtaCard(
    title: String,
    meta: String? = null,
    highlight: String? = null,
    ctaLabel: String,
    lockedCtaLabel: String = ctaLabel,
    locked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val debounced = rememberDebouncedClick(onClick = onClick)
    AppCard(
        surface = AppCardSurface.Elevated,
        accent = AppCardAccent.None,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xxs),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                meta?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
                highlight?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,
                    )
                }
            }
            if (locked) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "잠김",
                    tint = palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        AppButton(
            text = if (locked) lockedCtaLabel else ctaLabel,
            onClick = debounced,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.Regular,
            enabled = !locked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
