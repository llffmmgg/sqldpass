package com.sqldpass.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.AppBottomSheet
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 홈 자격증 캐러셀에서 카드 탭 시 펼침 — 시험 정보 4종 + "PASS+ 소개" CTA 1개.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 4 의 13번 규칙 (간략 패턴).
 *
 * Step 3 가 AppBottomSheet 적용. Step 4 그룹 D 는 내부 Material3 Button 만 AppButton 으로 교체.
 */
@Composable
fun CertInfoSheet(
    info: CertInfo,
    onDismiss: () -> Unit,
    onOpenPassPlus: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val cert = LocalSqldpassSemanticColors.current.cert
    val dotColor = certColorOf(info.slug, cert)

    AppBottomSheet(
        onDismiss = onDismiss,
        showDragHandle = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.lg - 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm + 2.dp),
            ) {
                Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
                Column {
                    Text(
                        info.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                    )
                    Text(
                        info.shortDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm + 2.dp)) {
                InfoRow(label = "시행처", value = info.issuer)
                InfoRow(label = "문항수", value = "${info.questionCount}문제")
                InfoRow(label = "시험 시간", value = info.durationLabel)
                InfoRow(label = "합격 기준", value = info.passCriteria)
            }

            AppButton(
                text = "PASS+ 소개",
                onClick = onOpenPassPlus,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Regular,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalSqldpassPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            modifier = Modifier.padding(start = SqldSpacing.base),
        )
    }
}
