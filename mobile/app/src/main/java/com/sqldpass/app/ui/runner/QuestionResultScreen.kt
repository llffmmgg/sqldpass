package com.sqldpass.app.ui.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.PastExamGradeResponse
import com.sqldpass.app.data.PastExamGradedItem
import com.sqldpass.app.data.PastExamSubjectScore
import com.sqldpass.app.data.SolveAnswerResponse
import com.sqldpass.app.data.SolveResponse
import com.sqldpass.app.ui.common.AppBadge
import com.sqldpass.app.ui.common.AppBadgeTone
import com.sqldpass.app.ui.common.AppBadgeVariant
import com.sqldpass.app.ui.common.AppBottomActionBar
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppCodeBlockSurface
import com.sqldpass.app.ui.common.AppMascot
import com.sqldpass.app.ui.common.AppMascotPose
import com.sqldpass.app.ui.common.AppNumberCell
import com.sqldpass.app.ui.common.AppNumberCellSize
import com.sqldpass.app.ui.common.AppQuestionContent
import com.sqldpass.app.ui.common.BottomAction
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassMonoText

/**
 * 다문항 풀이 결과 화면.
 *
 * Inked OMR 마이그레이션:
 *  - 상단: 닫기 + "결과" 타이틀.
 *  - PASS/FAIL 배너 (PastExam): AppCard accent + 마스코트 + 점수 KPI.
 *  - Solve 결과: 점수 KPI 카드 + 문항별 정답/오답 행.
 *  - 과목별 결과 (PastExam): AppCard 행 + 진행바 (success/warning/danger).
 *  - 하단: AppBottomActionBar (닫기 + 다시 풀기).
 */
@Composable
fun QuestionResultScreen(
    result: RunnerResult,
    onClose: () -> Unit,
    onRestart: (() -> Unit)? = null,
) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        ResultHeader(onClose = onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SqldSpacing.lg, vertical = SqldSpacing.base),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.base),
        ) {
            when (result) {
                is RunnerResult.Solve -> {
                    SolveScoreCard(result.response)
                    if (result.response.answers.isNotEmpty()) {
                        SolveAnswerBreakdown(answers = result.response.answers)
                    }
                }
                is RunnerResult.PastExam -> {
                    PassFailBanner(result.response)
                    SubjectScoreList(result.response.subjectScores)
                    if (result.response.items.isNotEmpty()) {
                        PastExamItemBreakdown(items = result.response.items)
                    }
                }
            }
        }

        AppBottomActionBar(
            primary = BottomAction(
                label = "닫기",
                onClick = onClose,
                variant = AppButtonVariant.Primary,
            ),
            secondary = onRestart?.let {
                BottomAction(
                    label = "다시 풀기",
                    onClick = it,
                    variant = AppButtonVariant.Secondary,
                )
            },
        )
    }
}

@Composable
private fun ResultHeader(onClose: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(SqldRadius.full))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "결과 닫기",
                    tint = palette.textPrimary,
                )
            }
            Text(
                "결과",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )
    }
}

@Composable
private fun PassFailBanner(response: PastExamGradeResponse) {
    val palette = LocalSqldpassPalette.current
    val isPass = response.passed
    val accent = if (isPass) AppCardAccent.Success else AppCardAccent.Danger
    val accentColor = if (isPass) palette.success else palette.danger

    AppCard(
        surface = AppCardSurface.Card,
        accent = accent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
        ) {
            AppMascot(
                pose = if (isPass) AppMascotPose.Celebrate else AppMascotPose.Review,
                sizeDp = 88,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                AppBadge(
                    label = if (isPass) "합격" else "불합격",
                    tone = if (isPass) AppBadgeTone.Success else AppBadgeTone.Danger,
                    variant = AppBadgeVariant.Solid,
                )
                AppNumberCell(
                    value = "${response.correctCount}",
                    label = "맞힌 문제",
                    unit = "/${response.totalCount}",
                    accent = accentColor,
                    size = AppNumberCellSize.Display,
                    modifier = Modifier.fillMaxWidth(),
                )
                response.passReason?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SolveScoreCard(response: SolveResponse) {
    val palette = LocalSqldpassPalette.current
    val safeTotal = response.totalCount.coerceAtLeast(1)
    val rate = response.correctCount * 100 / safeTotal
    val rateColor = when {
        rate >= 70 -> palette.accent
        rate >= 50 -> palette.warning
        else -> palette.danger
    }
    AppCard(
        surface = AppCardSurface.Card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "채점 결과",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
        ) {
            AppNumberCell(
                value = "${response.correctCount}",
                label = "맞힌 문제",
                unit = "/${response.totalCount}",
                accent = rateColor,
                size = AppNumberCellSize.Display,
                modifier = Modifier.weight(1f),
            )
            AppNumberCell(
                value = "$rate",
                label = "정답률",
                unit = "%",
                accent = rateColor,
                size = AppNumberCellSize.Display,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SolveAnswerBreakdown(answers: List<SolveAnswerResponse>) {
    val palette = LocalSqldpassPalette.current
    val correctCount = answers.count { it.correct }
    Text(
        "문항별 결과 ($correctCount/${answers.size} 정답)",
        style = MaterialTheme.typography.titleMedium,
        color = palette.textPrimary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        answers.forEachIndexed { idx, a ->
            ResultAnswerRow(
                index = idx,
                isCorrect = a.correct,
                correctOptionText = "${a.correctOption}",
                selectedOptionText = if (a.selectedOption == 0) "—" else "${a.selectedOption}",
                userInputText = null,
                answerText = null,
            )
        }
    }
}

@Composable
private fun PastExamItemBreakdown(items: List<PastExamGradedItem>) {
    val palette = LocalSqldpassPalette.current
    var expandedQid by remember { mutableStateOf<Long?>(null) }
    val correctCount = items.count { it.correct }
    Text(
        "문항별 정답·해설 ($correctCount/${items.size})",
        style = MaterialTheme.typography.titleMedium,
        color = palette.textPrimary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        items.forEachIndexed { idx, item ->
            val correctOptionStr = item.correctOption?.toString() ?: item.answer ?: "—"
            val selectedStr = item.selectedOption?.toString() ?: item.submittedAnswerText ?: "—"
            val expanded = expandedQid == item.questionId

            AppCard(
                surface = AppCardSurface.Card,
                onClick = {
                    expandedQid = if (expanded) null else item.questionId
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnswerRowContent(
                    index = idx,
                    isCorrect = item.correct,
                    correctOptionText = correctOptionStr,
                    selectedOptionText = selectedStr,
                )
                if (expanded) {
                    item.explanation?.takeIf { it.isNotBlank() }?.let { exp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = SqldSpacing.xs),
                        ) {
                            AppQuestionContent(
                                text = exp,
                                textSizeSp = 14f,
                                codeBlockSurface = AppCodeBlockSurface.Bare,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultAnswerRow(
    index: Int,
    isCorrect: Boolean,
    correctOptionText: String,
    selectedOptionText: String,
    userInputText: String?,
    answerText: String?,
) {
    AppCard(
        surface = AppCardSurface.Card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnswerRowContent(
            index = index,
            isCorrect = isCorrect,
            correctOptionText = correctOptionText,
            selectedOptionText = selectedOptionText,
            userInputText = userInputText,
            answerText = answerText,
        )
    }
}

@Composable
private fun AnswerRowContent(
    index: Int,
    isCorrect: Boolean,
    correctOptionText: String,
    selectedOptionText: String,
    userInputText: String? = null,
    answerText: String? = null,
) {
    val palette = LocalSqldpassPalette.current
    val accent = if (isCorrect) palette.success else palette.danger
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        Icon(
            imageVector = if (isCorrect) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
            contentDescription = if (isCorrect) "정답" else "오답",
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.xxs),
        ) {
            Text(
                "${index + 1}번 — ${if (isCorrect) "정답" else "오답"}",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            val subtitle = if (answerText != null) {
                "정답 $answerText · 입력 ${userInputText ?: "—"}"
            } else {
                "정답 $correctOptionText 번 · 선택 $selectedOptionText 번"
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
        }
    }
}

/**
 * 과목별 결과 카드. 각 과목 = AppCard + 가는 진행바.
 * 진행률 ≥ 70 success, 50–70 warning, < 50 danger. failed 표시 별도.
 */
@Composable
private fun SubjectScoreList(scores: List<PastExamSubjectScore>) {
    if (scores.isEmpty()) return
    val palette = LocalSqldpassPalette.current
    Text(
        "과목별 결과",
        style = MaterialTheme.typography.titleMedium,
        color = palette.textPrimary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        scores.forEach { s ->
            val accuracy = if (s.total > 0) (s.correct.toFloat() / s.total.toFloat()) else 0f
            val barColor = when {
                accuracy >= 0.70f -> palette.success
                accuracy >= 0.50f -> palette.warning
                else -> palette.danger
            }
            AppCard(
                surface = AppCardSurface.Card,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        s.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${s.correct}/${s.total}",
                            style = SqldpassMonoText.small,
                            color = palette.textMuted,
                        )
                        if (s.failed) {
                            AppBadge(
                                label = "과락",
                                tone = AppBadgeTone.Danger,
                                variant = AppBadgeVariant.Solid,
                            )
                        }
                    }
                }
                ProgressBar(
                    progress = accuracy.coerceIn(0f, 1f),
                    color = barColor,
                )
                Text(
                    "${"%.1f".format(s.rate)}%",
                    style = SqldpassMonoText.small,
                    color = palette.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val palette = LocalSqldpassPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(SqldRadius.full))
            .background(palette.elevated),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(color),
        )
    }
}
