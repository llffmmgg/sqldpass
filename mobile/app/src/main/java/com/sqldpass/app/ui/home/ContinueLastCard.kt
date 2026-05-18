package com.sqldpass.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

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
    val palette = LocalSqldpassPalette.current
    val modeLabel = when (lastMode) {
        LastSolveMode.PRACTICE -> "어제 풀던 ${lastCertLabel} 랜덤 10문 이어가기"
        LastSolveMode.MOCK_EXAM -> "어제 시작한 ${lastCertLabel} 모의고사 이어가기"
        LastSolveMode.PAST_EXAM -> "어제 시작한 ${lastCertLabel} 기출 이어가기"
    }
    AppCard(
        surface = AppCardSurface.Card,
        accent = AppCardAccent.None,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "이어풀기",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                )
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
            }
            Icon(
                Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = palette.textMuted,
            )
        }
    }
}
