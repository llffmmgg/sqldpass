package com.sqldpass.app.ui.runner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqldpass.app.ui.common.AppBadge
import com.sqldpass.app.ui.common.AppBadgeTone
import com.sqldpass.app.ui.common.AppBadgeVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * Inked OMR 코드블록 카드.
 *
 * Outer chrome 은 AppCard(Elevated) — palette 기반.
 * 좌측 상단에 언어 라벨 AppBadge(Accent, Soft) — 없으면 "CODE".
 * 내부 코드 본문은 FROZEN: FontFamily.Monospace + horizontalScroll(softWrap=false) 유지.
 * maxLines / lineLimit 없음 — 가로 스크롤로만 길이 처리.
 */
@Composable
fun CodeBlockCard(language: String?, code: String) {
    AppCard(
        surface = AppCardSurface.Elevated,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = language?.takeIf { it.isNotBlank() }?.uppercase() ?: "CODE"
                AppBadge(
                    label = label,
                    tone = AppBadgeTone.Accent,
                    variant = AppBadgeVariant.Soft,
                )
            }
            // FROZEN — mono Text + horizontalScroll. maxLines/lineLimit 금지.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = code,
                    color = CodeBlockText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    ),
                    softWrap = false,
                )
            }
        }
    }
}

// 코드 본문 글자 — 라이트/다크 무관 가독성 위한 고정 톤.
private val CodeBlockText = Color(0xFFE4E4E7)
