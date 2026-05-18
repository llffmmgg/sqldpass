package com.sqldpass.app.ui.solve.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sqldpass.app.data.QuestionDetailResponse
import com.sqldpass.app.ui.common.AppBadge
import com.sqldpass.app.ui.common.AppBadgeTone
import com.sqldpass.app.ui.common.AppBadgeVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.SoloMarkdownContent
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 정답 공개 시 노출 — 정답/오답 배너 + (서술/단답일 때) 모범답안 + 키워드 + 해설 카드.
 *
 * AppCard + AppBadge 로 재구성. 외곽은 success/danger accent 의 AppCard,
 * 상단 행에 결과 AppBadge(Solid) + 보조 메시지, 본문에 해설 markdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoloExplanationCard(
    detail: QuestionDetailResponse,
    isCorrect: Boolean,
) {
    val palette = LocalSqldpassPalette.current
    val type = detail.questionType?.uppercase()
    val isMcq = type == null || type == "MCQ"

    val resultBadgeTone = if (isCorrect) AppBadgeTone.Success else AppBadgeTone.Danger
    val resultLabel = if (isCorrect) "정답" else "오답"

    AppCard(
        surface = AppCardSurface.Card,
        accent = if (isCorrect) AppCardAccent.Success else AppCardAccent.Danger,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 1) 상단 배지 행 — 결과 + 보조 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            AppBadge(
                label = resultLabel,
                tone = resultBadgeTone,
                variant = AppBadgeVariant.Solid,
            )
            // MCQ: 정답 번호 / 서답형: 모범답안 헤더
            val rightLabel = when {
                isMcq && detail.correctOption != null -> "정답 — ${detail.correctOption}번"
                isMcq -> "정답 — -"
                else -> "모범답안"
            }
            Text(
                rightLabel,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 2) 서답형이면 모범답안 본문 + 키워드 칩
        if (!isMcq) {
            Text(
                detail.answer.orEmpty().ifBlank { "-" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
                color = palette.textPrimary,
            )
            if (detail.keywords.isNotEmpty()) {
                Text(
                    if (type == "SHORT_ANSWER") "허용 표기" else "채점 키워드",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
                ) {
                    detail.keywords.forEach { kw ->
                        AppBadge(
                            label = kw,
                            tone = AppBadgeTone.Accent,
                            variant = AppBadgeVariant.Soft,
                        )
                    }
                }
            }
        }

        // 3) 해설 본문 — 렌더러 변경 금지
        if (!detail.explanation.isNullOrBlank()) {
            Text(
                "해설",
                style = MaterialTheme.typography.titleSmall,
                color = palette.accent,
                fontWeight = FontWeight.SemiBold,
            )
            SoloMarkdownContent(text = detail.explanation, textSizeSp = 16f)
        }
    }
}
