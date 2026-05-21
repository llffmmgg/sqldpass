package com.sqldpass.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sqldpass.app.data.QuotaInfo
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 무료 일일 한도 페이월 Bottom Sheet.
 *
 * - 트리거: [com.sqldpass.app.data.remote.QuotaInterceptor] 가 HTTP 402 를 잡고
 *   [com.sqldpass.app.ui.AppViewModel.showQuotaPaywall] 호출 → [com.sqldpass.app.ui.AppViewModel.quotaPaywall] 가 non-null.
 * - 표시 위치: [com.sqldpass.app.MainActivity.SqldpassApp] 최상위.
 * - 액션:
 *   - "Focus 7일권 보기" → 결제 카탈로그.
 *   - "내일 다시 오기" → dismiss.
 *
 * 문구(확정):
 * - DAILY_QUESTION_LIMIT: "오늘의 30문제 완주!" / "내일 다시 만나거나, Focus 7일권으로 끝까지 가볼까요?"
 * - DAILY_MOCK_LIMIT: "오늘 모의고사 1회 완료" / "Focus 7일권으로 매일 풀 수 있어요."
 *
 * 디자인 원칙(feedback_no_ai_blur_effects):
 *  - backdrop blur, glow, opacity pulse 사용 금지.
 *  - 단단한 톤 — palette.card / textPrimary / textMuted / accent 만 사용.
 */
@Composable
fun QuotaPaywallSheet(
    info: QuotaInfo,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val isMock = info.code == "DAILY_MOCK_LIMIT"
    val title = if (isMock) "오늘 모의고사 1회 완료" else "오늘의 30문제 완주!"
    val body = "플랜으로 매일 풀 수 있어요."

    AppBottomSheet(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SqldSpacing.xs, bottom = SqldSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            AppMascot(pose = AppMascotPose.Celebrate, sizeDp = 96)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = palette.textPrimary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            modifier = Modifier.padding(top = SqldSpacing.xs),
        )
        Text(
            text = "오늘 ${info.used} / ${info.limit}",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSubtle,
            modifier = Modifier.padding(top = SqldSpacing.sm),
        )
        if (isMock) {
            Text(
                text = "PASS+ 회차는 Pro 부터",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSubtle,
                modifier = Modifier.padding(top = SqldSpacing.xs),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SqldSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            AppButton(
                text = "플랜 보기",
                onClick = onPurchase,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = "내일 다시 오기",
                onClick = onDismiss,
                variant = AppButtonVariant.Tertiary,
                size = AppButtonSize.Regular,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
