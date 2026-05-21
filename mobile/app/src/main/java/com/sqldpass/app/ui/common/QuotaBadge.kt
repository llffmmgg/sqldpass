package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.QuotaResponse
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 일일 한도 배지 종류. label 은 헤더 표시 문구의 단위.
 *
 * - [Question]: SolveTab 진입 헤더에 표시. "오늘 18 / 30 문제"
 * - [Mock]: MockExamTab 진입 헤더에 표시. "오늘 0 / 1 모의고사"
 *
 * **금지**: PastExamTab 마운트 (기출복원 무제한).
 */
enum class QuotaKind(val label: String) {
    Question("문제"),
    Mock("모의고사"),
}

/**
 * 무료 일일 한도 인디케이터 배지.
 *
 * - [quota] 가 null → 미로드 상태 → 표시 숨김.
 * - kind 에 해당하는 `xxxLimit` 가 null → 활성 구독자(Focus 등) → 표시 숨김.
 * - 그 외엔 "오늘 N / M ${label}" 표시.
 *
 * **본 컴포넌트는 표시 전용 — 차단/카운팅 로직 없음.** 한도 판정은 서버.
 */
@Composable
fun QuotaBadge(
    quota: QuotaResponse?,
    kind: QuotaKind,
    modifier: Modifier = Modifier,
) {
    if (quota == null) return
    val limit = when (kind) {
        QuotaKind.Question -> quota.questionLimit
        QuotaKind.Mock -> quota.mockLimit
    } ?: return
    val used = when (kind) {
        QuotaKind.Question -> quota.questionUsed
        QuotaKind.Mock -> quota.mockUsed
    }

    val palette = LocalSqldpassPalette.current
    Box(
        modifier = modifier
            .background(palette.elevated, RoundedCornerShape(SqldRadius.full))
            .border(
                width = 1.dp,
                color = palette.border,
                shape = RoundedCornerShape(SqldRadius.full),
            )
            .padding(PaddingValues(horizontal = SqldSpacing.md, vertical = SqldSpacing.xs)),
    ) {
        Text(
            text = "오늘 $used / $limit ${kind.label}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = palette.textMuted,
        )
    }
}
