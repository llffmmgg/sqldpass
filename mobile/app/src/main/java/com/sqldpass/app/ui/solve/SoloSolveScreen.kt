package com.sqldpass.app.ui.solve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.QuestionResponse
import com.sqldpass.app.text.parseQuestion
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.SoloSession
import com.sqldpass.app.ui.SOLO_SET_SIZE
import com.sqldpass.app.ui.common.SoloMarkdownContent
import com.sqldpass.app.ui.solve.components.OfflineQueueChip
import com.sqldpass.app.ui.solve.components.SolveOptionRow
import com.sqldpass.app.ui.solve.components.SoloBottomActionBar
import com.sqldpass.app.ui.solve.components.SoloExplanationCard
import com.sqldpass.app.ui.solve.components.SoloProgressHeader
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 단일 채점 풀이 화면(웹 /solve phase="solve" 동치).
 *
 * AppViewModel.startSoloSolve(...) 가 먼저 호출돼 state.soloSession 이 채워진 뒤 진입한다.
 * 본 화면은 콜백만 받고 상태는 모두 ViewModel.
 */
@Composable
fun SoloSolveScreen(
    state: AppUiState,
    pendingSolveCount: Int,
    onSelectOption: (Int) -> Unit,
    onSetAnswerText: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    onReplaySame: () -> Unit,
    onNewRandom: () -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onReport: (Long) -> Unit,
    bookmarkedIds: Set<Long> = emptySet(),
) {
    val session = state.soloSession
    if (session == null) {
        SoloLoadingState()
        return
    }

    if (session.sessionComplete) {
        SessionCompleteCard(
            session = session,
            onReplaySame = onReplaySame,
            onNewRandom = onNewRandom,
            onExit = onExit,
        )
        return
    }

    val current = session.current
    if (current == null) {
        SoloLoadingState()
        return
    }

    var showExitConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // 정답 공개 직후 햅틱 — Composable 안에서 한 번만
    LaunchedEffect(session.revealed, session.detail?.id) {
        if (session.revealed && session.detail != null) {
            val correct = isClientCorrect(session)
            if (correct) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // CONFIRM 동등
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SoloProgressHeader(
            solvedCount = session.solvedCount,
            totalCount = SOLO_SET_SIZE,
            correctCount = session.correctCount,
            isBookmarked = current.id in bookmarkedIds,
            onClose = { showExitConfirm = true },
            onToggleBookmark = { onToggleBookmark(current.id) },
            onReport = { onReport(current.id) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SqldSpacing.lg, vertical = SqldSpacing.base),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.base),
        ) {
            // 오프라인 큐 인디케이터 (count > 0 일 때만)
            OfflineQueueChip(count = pendingSolveCount)

            // 과목 라벨
            Text(
                session.subjectName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            // 문제 본문 카드
            QuestionContentCard(current)

            if (isMcq(current.questionType)) {
                val parsed = remember(current.content) { parseQuestion(current.content) }
                val displayCount = if (parsed.options.isNotEmpty()) parsed.options.size else 4
                val correctOption = session.detail?.correctOption
                Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
                    (1..displayCount).forEach { num ->
                        val optionText = parsed.options.getOrNull(num - 1)
                        SolveOptionRow(
                            optionNumber = num,
                            optionText = optionText,
                            selected = session.selectedOption == num,
                            revealed = session.revealed,
                            isCorrectOption = correctOption == num,
                            onClick = {
                                if (!session.revealed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectOption(num)
                                }
                            },
                            onDoubleClick = {
                                if (!session.revealed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectOption(num)
                                    onSubmit()
                                }
                            },
                        )
                    }
                }
            } else {
                ShortAnswerInput(
                    value = session.answerText,
                    revealed = session.revealed,
                    onChange = onSetAnswerText,
                    onImeSubmit = onSubmit,
                )
            }

            // 정답 공개 시 해설 카드 등장
            AnimatedVisibility(
                visible = session.revealed && session.detail != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                session.detail?.let { d ->
                    SoloExplanationCard(detail = d, isCorrect = isClientCorrect(session))
                }
            }

            // 풀이 기록 저장 실패 시 인라인 안내 (오프라인 큐잉은 step 5)
            session.submitError?.let { err ->
                Text(
                    "풀이 기록 저장 실패 — $err",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalSqldpassSemanticColors.current.state.warning,
                )
            }
        }

        SoloBottomActionBar(
            revealed = session.revealed,
            hasAnswer = session.hasAnswer,
            submitting = session.submitting,
            isLastBeforeComplete = session.solvedCount + 1 >= SOLO_SET_SIZE,
            onSubmit = onSubmit,
            onNext = onNext,
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("풀이 종료") },
            text = { Text("지금까지의 진행은 저장되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onExit()
                }) {
                    Text("종료", color = LocalSqldpassSemanticColors.current.state.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("계속 풀기") }
            },
        )
    }
}

@Composable
private fun QuestionContentCard(question: QuestionResponse) {
    val body = remember(question.content) {
        if (isMcq(question.questionType)) parseQuestion(question.content).body
        else question.content
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(SqldRadius.lg))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(SqldRadius.lg))
            .padding(SqldSpacing.base),
    ) {
        SoloMarkdownContent(text = body.ifBlank { question.content })
    }
}

@Composable
private fun ShortAnswerInput(
    value: String,
    revealed: Boolean,
    onChange: (String) -> Unit,
    onImeSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs)) {
        Text(
            "답안 입력",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = !revealed,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onImeSubmit() }),
            singleLine = false,
        )
        Text(
            "대소문자·앞뒤 공백은 자동으로 무시됩니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SoloLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SessionCompleteCard(
    session: SoloSession,
    onReplaySame: () -> Unit,
    onNewRandom: () -> Unit,
    onExit: () -> Unit,
) {
    val total = session.solvedCount.coerceAtLeast(1)
    val rate = (session.correctCount * 100 / total)
    val semantic = LocalSqldpassSemanticColors.current
    val rateColor = when {
        rate >= 90 -> semantic.state.success
        rate >= 70 -> semantic.state.warning
        else -> semantic.state.danger
    }
    val message = when {
        rate >= 90 -> "완벽해요! 같은 과목을 더 풀어볼까요?"
        rate >= 70 -> "잘하고 있어요. 한 세트 더 풀면 손에 더 익을 거예요."
        else -> "괜찮아요. 약한 문제부터 다시 한 번 풀어보세요."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(SqldSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.base),
    ) {
        Text(
            "세션 완료",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            session.subjectName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${session.correctCount}",
                style = MaterialTheme.typography.displayMedium,
                color = rateColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "/$total",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = SqldSpacing.xs, bottom = SqldSpacing.sm),
            )
            Text(
                "  $rate%",
                style = MaterialTheme.typography.titleLarge,
                color = rateColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = SqldSpacing.xs),
            )
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            OutlinedButton(
                onClick = onReplaySame,
                shape = RoundedCornerShape(SqldRadius.sm),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("같은 10문제 다시") }
            Button(
                onClick = onNewRandom,
                shape = RoundedCornerShape(SqldRadius.sm),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("새 10문제") }
            TextButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("다른 과목 선택") }
        }
    }
}

private fun isMcq(type: String?): Boolean =
    type == null || type.equals("MCQ", ignoreCase = true)

private fun isClientCorrect(session: SoloSession): Boolean {
    val d = session.detail ?: return false
    val type = d.questionType?.uppercase()
    if (type == null || type == "MCQ") {
        return session.selectedOption != null && session.selectedOption == d.correctOption
    }
    val normalize: (String) -> String = { it.trim().lowercase().replace(Regex("\\s+"), " ") }
    val submitted = normalize(session.answerText)
    if (submitted.isEmpty()) return false
    d.answer?.let { if (normalize(it) == submitted) return true }
    return d.keywords.any { normalize(it) == submitted }
}
