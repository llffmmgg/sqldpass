package com.sqldpass.app.ui.solve.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 오프라인 답안 큐 미동기화 카운트 표시 — count > 0 일 때만 노출.
 *
 * "오프라인 — N개 보관 중" warning 톤. 실패가 아닌 "보관 중" 상태이므로 danger 색 X.
 */
@Composable
fun OfflineQueueChip(count: Int) {
    if (count <= 0) return
    val warning = LocalSqldpassSemanticColors.current.state.warning
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
        modifier = Modifier
            .background(warning.copy(alpha = 0.12f), RoundedCornerShape(SqldRadius.full))
            .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = warning,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            text = "오프라인 — ${count}개 보관 중",
            style = MaterialTheme.typography.labelMedium,
            color = warning,
        )
    }
}
