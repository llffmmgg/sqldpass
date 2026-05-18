package com.sqldpass.app.ui.solve.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.QuestionDetailResponse
import com.sqldpass.app.ui.common.SoloMarkdownContent
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 정답 공개 시 노출 — 정답/오답 배너 + (서술/단답일 때) 모범답안 + 키워드 + 해설 카드.
 */
@Composable
fun SoloExplanationCard(
    detail: QuestionDetailResponse,
    isCorrect: Boolean,
) {
    val semantic = LocalSqldpassSemanticColors.current
    val success = semantic.state.success
    val danger = semantic.state.danger
    val accent = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.md)) {
        // 정답/오답 배너
        ResultBanner(
            isCorrect = isCorrect,
            detail = detail,
            success = success,
            danger = danger,
        )

        val type = detail.questionType?.uppercase()
        if (type != null && type != "MCQ") {
            ModelAnswerCard(detail = detail)
        }

        if (!detail.explanation.isNullOrBlank()) {
            ExplanationCard(text = detail.explanation, accent = accent)
        }
    }
}

@Composable
private fun ResultBanner(
    isCorrect: Boolean,
    detail: QuestionDetailResponse,
    success: Color,
    danger: Color,
) {
    val color = if (isCorrect) success else danger
    val bg = color.copy(alpha = 0.10f)
    val type = detail.questionType?.uppercase()
    val message: String = when {
        isCorrect -> "정답입니다!"
        type == "MCQ" -> "오답 — 정답은 ${detail.correctOption ?: "-"}번입니다."
        else -> "오답 — 모범답안: ${detail.answer ?: "(없음)"}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(SqldRadius.lg))
            .border(1.dp, color, RoundedCornerShape(SqldRadius.lg))
            .padding(horizontal = SqldSpacing.base, vertical = SqldSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (isCorrect) "✓" else "✗",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(SqldSpacing.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelAnswerCard(detail: QuestionDetailResponse) {
    val semantic = LocalSqldpassSemanticColors.current
    val success = semantic.state.success
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(SqldRadius.lg))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(SqldRadius.lg))
            .padding(SqldSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        Text(
            "모범답안",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            detail.answer.orEmpty().ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
        )
        if (detail.keywords.isNotEmpty()) {
            Text(
                if (detail.questionType?.uppercase() == "SHORT_ANSWER") "허용 표기" else "채점 키워드",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = SqldSpacing.xs),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                detail.keywords.forEach { kw ->
                    Text(
                        kw,
                        style = MaterialTheme.typography.labelSmall,
                        color = success,
                        modifier = Modifier
                            .background(success.copy(alpha = 0.10f), RoundedCornerShape(SqldRadius.sm))
                            .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(text: String, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.06f), RoundedCornerShape(SqldRadius.lg))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(SqldRadius.lg))
            .padding(SqldSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        Text(
            "해설",
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        SoloMarkdownContent(text = text, textSizeSp = 15f)
    }
}
