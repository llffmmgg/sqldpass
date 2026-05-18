package com.sqldpass.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 이어풀기 추천 카드 — 마지막 풀이의 자격증·모드 기반.
 *
 * lastCertLabel 또는 lastMode 가 null 이면 호출부에서 카드 자체를 노출하지 않음.
 * 본 컴포넌트는 값이 모두 있는 케이스만 다룬다.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1.
 */
enum class LastSolveMode { PRACTICE, MOCK_EXAM, PAST_EXAM }

@Composable
fun ContinueLastCard(
    lastCertLabel: String,
    lastMode: LastSolveMode,
    onClick: () -> Unit,
) {
    val modeLabel = when (lastMode) {
        LastSolveMode.PRACTICE -> "어제 풀던 ${lastCertLabel} 랜덤 10문 이어가기"
        LastSolveMode.MOCK_EXAM -> "어제 시작한 ${lastCertLabel} 모의고사 이어가기"
        LastSolveMode.PAST_EXAM -> "어제 시작한 ${lastCertLabel} 기출 이어가기"
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "이어풀기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Icon(
                Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
