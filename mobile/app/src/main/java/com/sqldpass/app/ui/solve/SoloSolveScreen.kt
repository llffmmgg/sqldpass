package com.sqldpass.app.ui.solve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.sqldpass.app.ui.common.AppBottomActionBar
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppMascot
import com.sqldpass.app.ui.common.AppMascotPose
import com.sqldpass.app.ui.common.AppNumberCell
import com.sqldpass.app.ui.common.AppNumberCellSize
import com.sqldpass.app.ui.common.AppOptionRow
import com.sqldpass.app.ui.common.AppProgressPill
import com.sqldpass.app.ui.common.AppSectionHeader
import com.sqldpass.app.ui.common.AppStateView
import com.sqldpass.app.ui.common.AppTextField
import com.sqldpass.app.ui.common.AppViewState
import com.sqldpass.app.ui.common.BottomAction
import com.sqldpass.app.ui.common.SoloMarkdownContent
import com.sqldpass.app.ui.common.appOptionStateOf
import com.sqldpass.app.ui.solve.components.OfflineQueueChip
import com.sqldpass.app.ui.solve.components.SoloExplanationCard
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 단일 채점 풀이 화면(웹 /solve phase="solve" 동치).
 *
 * AppViewModel.startSoloSolve(...) 가 먼저 호출돼 state.soloSession 이 채워진 뒤 진입한다.
 * 본 화면은 콜백만 받고 상태는 모두 ViewModel.
 *
 * Inked OMR 마이그레이션: 모든 chrome 은 App* primitive (AppOptionRow / AppProgressPill /
 * AppBottomActionBar / AppCard / AppButton / AppBadge / AppStateView / AppTextField) 로 구성.
 * Material3 colorScheme 직접 참조 없음 — LocalSqldpassPalette.current 만 읽는다.
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
    val palette = LocalSqldpassPalette.current
    val session = state.soloSession
    if (session == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.page),
            contentAlignment = Alignment.Center,
        ) {
            AppStateView(state = AppViewState.Loading)
        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.page),
            contentAlignment = Alignment.Center,
        ) {
            AppStateView(state = AppViewState.Loading)
        }
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
            .background(palette.page),
    ) {
        SoloHeader(
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
                color = palette.accent,
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
                        AppOptionRow(
                            optionNumber = num,
                            optionText = optionText,
                            state = appOptionStateOf(
                                selected = session.selectedOption == num,
                                revealed = session.revealed,
                                isCorrectOption = correctOption == num,
                            ),
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
                            modifier = Modifier.fillMaxWidth(),
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
                    color = palette.warning,
                )
            }
        }

        SoloBottomBar(
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
                AppButton(
                    text = "종료하기",
                    onClick = {
                        showExitConfirm = false
                        onExit()
                    },
                    variant = AppButtonVariant.Destructive,
                    size = AppButtonSize.Compact,
                )
            },
            dismissButton = {
                AppButton(
                    text = "계속 풀기",
                    onClick = { showExitConfirm = false },
                    variant = AppButtonVariant.Tertiary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

/**
 * 단일 채점 풀이 상단 헤더 — 종료 / 진행도 / 북마크 / 메뉴.
 *
 * 좌측 닫기 → 중앙 AppProgressPill → 우측 북마크 + 메뉴(신고).
 * 1dp bottom hairline 만, 그림자 없음. statusBarsPadding 으로 시스템 인셋 회피.
 */
@Composable
private fun SoloHeader(
    solvedCount: Int,
    totalCount: Int,
    correctCount: Int,
    isBookmarked: Boolean,
    onClose: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReport: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val displayCurrent = (solvedCount + 1).coerceAtMost(totalCount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            HeaderIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "풀이 종료",
                tint = palette.textPrimary,
                onClick = onClose,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppProgressPill(
                    current = displayCurrent,
                    total = totalCount,
                )
                Text(
                    "정답 $correctCount / $solvedCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
            HeaderIconButton(
                icon = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "북마크 해제" else "북마크",
                tint = if (isBookmarked) palette.accent else palette.textMuted,
                onClick = onToggleBookmark,
            )
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                HeaderIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "메뉴",
                    tint = palette.textMuted,
                    onClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null) },
                        text = { Text("이 문제 신고") },
                        onClick = {
                            menuOpen = false
                            onReport()
                        },
                    )
                }
            }
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
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(SqldRadius.full))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * 단일 채점 풀이 하단 액션 바 — AppBottomActionBar 위에 미답/공개 상태 매핑.
 */
@Composable
private fun SoloBottomBar(
    revealed: Boolean,
    hasAnswer: Boolean,
    submitting: Boolean,
    isLastBeforeComplete: Boolean,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    if (!revealed) {
        AppBottomActionBar(
            primary = BottomAction(
                label = if (submitting) "확인중…" else "정답 확인",
                onClick = onSubmit,
                enabled = hasAnswer && !submitting,
                loading = submitting,
                variant = AppButtonVariant.Primary,
            ),
        )
    } else {
        AppBottomActionBar(
            primary = BottomAction(
                label = if (isLastBeforeComplete) "결과 보기" else "다음 문제",
                onClick = onNext,
                enabled = !submitting,
                variant = AppButtonVariant.Primary,
            ),
        )
    }
}

@Composable
private fun QuestionContentCard(question: QuestionResponse) {
    val body = remember(question.content) {
        if (isMcq(question.questionType)) parseQuestion(question.content).body
        else question.content
    }
    AppCard(
        surface = AppCardSurface.Card,
        modifier = Modifier.fillMaxWidth(),
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
    AppTextField(
        value = value,
        onValueChange = onChange,
        label = "답안 입력",
        placeholder = "답안을 입력하세요",
        helper = "대소문자·앞뒤 공백은 자동으로 무시됩니다.",
        enabled = !revealed,
        singleLine = false,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onImeSubmit() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SessionCompleteCard(
    session: SoloSession,
    onReplaySame: () -> Unit,
    onNewRandom: () -> Unit,
    onExit: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val total = session.solvedCount.coerceAtLeast(1)
    val rate = (session.correctCount * 100 / total)
    val rateColor = when {
        rate >= 90 -> palette.success
        rate >= 70 -> palette.warning
        else -> palette.danger
    }
    val message = when {
        rate >= 90 -> "완벽해요! 같은 과목을 더 풀어볼까요?"
        rate >= 70 -> "잘하고 있어요. 한 세트 더 풀면 손에 더 익을 거예요."
        else -> "괜찮아요. 약한 문제부터 다시 한 번 풀어보세요."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page)
            .statusBarsPadding()
            .padding(SqldSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.base),
    ) {
        AppSectionHeader(
            eyebrow = "세션 완료",
            title = session.subjectName,
        )

        // KPI 그리드 — 맞힌 문제 / 정답률
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
        ) {
            AppNumberCell(
                value = "${session.correctCount}",
                label = "맞힌 문제",
                unit = "/$total",
                size = AppNumberCellSize.Display,
                accent = rateColor,
                modifier = Modifier.weight(1f),
            )
            AppNumberCell(
                value = "$rate%",
                label = "정답률",
                size = AppNumberCellSize.Display,
                accent = rateColor,
                modifier = Modifier.weight(1f),
            )
        }

        // 마스코트 + 메시지
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppMascot(pose = AppMascotPose.Celebrate, sizeDp = 88)
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.textMuted,
        )

        Box(modifier = Modifier.weight(1f))

        // 액션 버튼 3종 — Primary / Secondary / Tertiary
        Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
            AppButton(
                text = "새 10문제",
                onClick = onNewRandom,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Regular,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = "같은 10문제 다시",
                onClick = onReplaySame,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Regular,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = "다른 과목 선택",
                onClick = onExit,
                variant = AppButtonVariant.Tertiary,
                size = AppButtonSize.Regular,
                modifier = Modifier.fillMaxWidth(),
            )
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
