package com.sqldpass.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * Inked OMR 스켈레톤 wrapper — `AppSkeleton` 위에 얹은 호환 레이어.
 *
 * - `SkeletonLine`: 인라인 한 줄 placeholder. `AppSkeleton` 으로 위임.
 * - `SkeletonCard`: 카드 모양 placeholder. `AppCard` 와 동일한 surface/border 위에
 *   여러 줄 `AppSkeleton` 을 적층한다.
 *
 * 본 wrapper 의 존재 이유: SolveTab / MockExamTab / PastExamTab 의 기존 호출처를
 * 건드리지 않고 Material3 colorScheme 의존을 제거한다 (Step 4 그룹 A 범위).
 */
@Composable
fun SkeletonLine(width: Modifier = Modifier.fillMaxWidth(), height: Dp = 16.dp) {
    AppSkeleton(
        modifier = width.height(height),
        shape = RoundedCornerShape(SqldRadius.sm),
    )
}

@Composable
fun SkeletonCard(lines: Int = 3) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SqldRadius.lg))
            .background(palette.card)
            .border(
                BorderStroke(1.dp, palette.border),
                RoundedCornerShape(SqldRadius.lg),
            )
            .padding(SqldSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm + 2.dp),
    ) {
        SkeletonLine(height = 20.dp)
        repeat((lines - 1).coerceAtLeast(0)) {
            SkeletonLine(height = 14.dp)
        }
        Spacer(Modifier.height(SqldSpacing.xxs))
        SkeletonLine(
            width = Modifier.fillMaxWidth(0.4f),
            height = 28.dp,
        )
    }
}
