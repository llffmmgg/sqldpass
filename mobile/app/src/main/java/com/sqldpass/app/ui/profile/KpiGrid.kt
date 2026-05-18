package com.sqldpass.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sqldpass.app.ui.common.AppNumberCell
import com.sqldpass.app.ui.common.AppNumberCellSize
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 내정보 상단 KPI 2x2 — 총 풀이 / 평균 정답률 / 최장 스트릭 / 합격 확률.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.5 / § 6.
 * 본 phase 에서 longestStreak 만 실데이터, 나머지 3개는 placeholder("—").
 * 누적 풀이·평균 정답률·합격 확률 백엔드 신설은 별 phase `kpi-backend-support`.
 *
 * Step 6 PoC: Inked OMR AppNumberCell 로 재작성. 카드/숫자 톤은 primitive 가 모두 책임진다.
 */
@Composable
fun KpiGrid(
    totalSolved: Int?,
    avgCorrectRate: Int?,
    longestStreak: Int?,
    passProbability: Int?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
        Row {
            AppNumberCell(
                value = totalSolved?.toString() ?: "—",
                label = "총 풀이",
                unit = if (totalSolved != null) "문제" else null,
                size = AppNumberCellSize.Regular,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(SqldSpacing.sm))
            AppNumberCell(
                value = avgCorrectRate?.let { "$it%" } ?: "—",
                label = "평균 정답률",
                size = AppNumberCellSize.Regular,
                modifier = Modifier.weight(1f),
            )
        }
        Row {
            AppNumberCell(
                value = longestStreak?.toString() ?: "—",
                label = "최장 연속",
                unit = if (longestStreak != null) "일" else null,
                size = AppNumberCellSize.Regular,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(SqldSpacing.sm))
            AppNumberCell(
                value = passProbability?.let { "$it%" } ?: "—",
                label = "합격 확률",
                size = AppNumberCellSize.Regular,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
