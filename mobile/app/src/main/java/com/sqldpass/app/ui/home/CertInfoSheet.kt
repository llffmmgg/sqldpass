package com.sqldpass.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

/**
 * 홈 자격증 캐러셀에서 카드 탭 시 펼침 — 시험 정보 4종 + "PASS+ 소개" CTA 1개.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 4 의 13번 규칙 (간략 패턴).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertInfoSheet(
    info: CertInfo,
    onDismiss: () -> Unit,
    onOpenPassPlus: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val cert = LocalSqldpassSemanticColors.current.cert
    val dotColor = certColorOf(info.slug, cert)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
                Column {
                    Text(info.label, style = MaterialTheme.typography.titleLarge)
                    Text(
                        info.shortDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(label = "시행처", value = info.issuer)
                InfoRow(label = "문항수", value = "${info.questionCount}문제")
                InfoRow(label = "시험 시간", value = info.durationLabel)
                InfoRow(label = "합격 기준", value = info.passCriteria)
            }

            Button(
                onClick = onOpenPassPlus,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("PASS+ 소개") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
