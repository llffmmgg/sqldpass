package com.sqldpass.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 내정보 상단 KPI 2x2 — 총 풀이 / 평균 정답률 / 최장 스트릭 / 합격 확률.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.5 / § 6.
 * 본 phase 에서 longestStreak 만 실데이터, 나머지 3개는 placeholder("—").
 * 누적 풀이·평균 정답률·합격 확률 백엔드 신설은 별 phase `kpi-backend-support`.
 */
@Composable
fun KpiGrid(
    totalSolved: Int?,
    avgCorrectRate: Int?,
    longestStreak: Int?,
    passProbability: Int?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(
                icon = Icons.Outlined.Quiz,
                label = "총 풀이",
                value = totalSolved?.toString() ?: "—",
                unit = "문제",
                modifier = Modifier.weight(1f),
            )
            KpiTile(
                icon = Icons.Outlined.Percent,
                label = "평균 정답률",
                value = avgCorrectRate?.let { "$it%" } ?: "—",
                unit = null,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(
                icon = Icons.Outlined.LocalFireDepartment,
                label = "최장 연속",
                value = longestStreak?.toString() ?: "—",
                unit = "일",
                modifier = Modifier.weight(1f),
            )
            KpiTile(
                icon = Icons.Outlined.QueryStats,
                label = "합격 확률",
                value = passProbability?.let { "$it%" } ?: "—",
                unit = null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (unit != null) {
                    Text(
                        " $unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}
