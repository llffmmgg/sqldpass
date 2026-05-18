package com.sqldpass.app.ui.runner

import android.view.ViewGroup
import android.widget.TextView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AppRegistration
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sqldpass.app.ui.common.AppBottomActionBar
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppOptionRow
import com.sqldpass.app.ui.common.AppOptionState
import com.sqldpass.app.ui.common.AppProgressPill
import com.sqldpass.app.ui.common.AppProgressPillTimer
import com.sqldpass.app.ui.common.AppTextField
import com.sqldpass.app.ui.common.BottomAction
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme
import kotlinx.coroutines.delay

/**
 * 다문항 풀이 러너 화면.
 *
 * Inked OMR 마이그레이션: 모든 chrome 은 App* primitive 로 구성.
 *  - Top bar: 닫기 + Progress/Timer pill + 책갈피 + 메뉴(드롭다운).
 *  - 본문: AppCard 안 SoloMarkdownContent (FROZEN) + AppOptionRow/AppTextField.
 *  - 하단: AppBottomActionBar (이전/다음/제출).
 *  - 점프 그리드 / 신고 다이얼로그 / 종료 확인 다이얼로그 분기.
 *
 * 동작은 ViewModel/콜백을 통째로 보존 — 시각 레이어만 교체.
 */
@Composable
fun QuestionRunnerScreen(
    title: String,
    questions: List<RunnerQuestion>,
    onCancel: () -> Unit,
    onSubmit: (List<RunnerAnswerDraft>) -> Unit,
    submitting: Boolean = false,
    durationSeconds: Int = 0,
    bookmarkedIds: Set<Long> = emptySet(),
    onToggleBookmark: ((Long) -> Unit)? = null,
    onReport: ((Long) -> Unit)? = null,
) {
    val palette = LocalSqldpassPalette.current
    if (questions.isEmpty()) {
        EmptyRunnerState(title = title, onCancel = onCancel)
        return
    }

    val drafts = remember(questions) {
        mutableStateMapOf<Long, RunnerAnswerDraft>().apply {
            questions.forEach { put(it.id, RunnerAnswerDraft(questionId = it.id)) }
        }
    }
    var index by remember(questions) { mutableStateOf(0) }
    var jumpOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var giveUpConfirmOpen by remember { mutableStateOf(false) }
    val current = questions[index]
    val draft = drafts[current.id] ?: RunnerAnswerDraft(questionId = current.id)
    val isLast by remember { derivedStateOf { index == questions.lastIndex } }
    val haptic = LocalHapticFeedback.current

    var remainingSec by remember(questions, durationSeconds) { mutableStateOf(durationSeconds) }
    val timerEnabled = durationSeconds > 0
    val autoSubmitted = remember(questions) { mutableStateOf(false) }
    LaunchedEffect(timerEnabled, questions) {
        if (!timerEnabled) return@LaunchedEffect
        while (remainingSec > 0) {
            delay(1000)
            remainingSec -= 1
        }
        if (!autoSubmitted.value) {
            autoSubmitted.value = true
            onSubmit(questions.map { drafts[it.id] ?: RunnerAnswerDraft(it.id) })
        }
    }

    val answeredIndices = remember(drafts.values.toList()) {
        questions.mapIndexedNotNull { i, q ->
            if (drafts[q.id]?.isAnswered == true) i else null
        }.toSet()
    }
    val bookmarkedIndices = remember(questions, bookmarkedIds) {
        questions.mapIndexedNotNull { i, q ->
            if (q.id in bookmarkedIds) i else null
        }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        RunnerHeader(
            title = title,
            currentIndex = index,
            totalQuestions = questions.size,
            remainingSeconds = remainingSec,
            durationSeconds = durationSeconds,
            isBookmarked = current.id in bookmarkedIds,
            onClose = onCancel,
            onToggleBookmark = onToggleBookmark?.let { tb -> { tb(current.id) } },
            onJumpClick = { jumpOpen = true },
            onReportClick = onReport?.let { r -> { r(current.id) } },
            onGiveUp = { giveUpConfirmOpen = true },
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SqldSpacing.lg, vertical = SqldSpacing.base),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.base),
        ) {
            Text(
                "Q${current.displayOrder.takeIf { it > 0 } ?: (index + 1)}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
            )
            val parsed = current.parsed
            AppCard(
                surface = AppCardSurface.Card,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MarkdownContent(text = parsed.body.ifBlank { current.content })
            }

            if (isShortAnswer(current.questionType)) {
                val focus = LocalFocusManager.current
                AppTextField(
                    value = draft.answerText.orEmpty(),
                    onValueChange = { drafts[current.id] = draft.copy(answerText = it) },
                    label = "답안 입력",
                    placeholder = "답안을 입력하세요",
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
                    val options = parsed.options
                    val displayCount = if (options.isNotEmpty()) options.size else 4
                    (1..displayCount).forEach { option ->
                        val optionText = options.getOrNull(option - 1)
                        val isSelected = draft.selectedOption == option
                        AppOptionRow(
                            optionNumber = option,
                            optionText = optionText,
                            state = if (isSelected) AppOptionState.Selected else AppOptionState.Idle,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                drafts[current.id] = draft.copy(selectedOption = option)
                            },
                            onDoubleClick = {
                                // 더블 탭: 선택 + 다음 문제. 마지막 문제면 제출.
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                drafts[current.id] = draft.copy(selectedOption = option)
                                if (isLast) {
                                    onSubmit(questions.map { drafts[it.id] ?: RunnerAnswerDraft(it.id) })
                                } else {
                                    index += 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        AppBottomActionBar(
            primary = BottomAction(
                label = if (isLast) (if (submitting) "제출중…" else "제출") else "다음",
                onClick = {
                    if (isLast) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSubmit(questions.map { drafts[it.id] ?: RunnerAnswerDraft(it.id) })
                    } else {
                        index += 1
                    }
                },
                variant = AppButtonVariant.Primary,
                enabled = !submitting,
                loading = isLast && submitting,
            ),
            secondary = BottomAction(
                label = "이전",
                onClick = { if (index > 0) index -= 1 },
                variant = AppButtonVariant.Secondary,
                enabled = index > 0 && !submitting,
            ),
        )
    }

    if (jumpOpen) {
        RunnerJumpGrid(
            total = questions.size,
            currentIndex = index,
            answeredIndices = answeredIndices,
            bookmarkedIndices = bookmarkedIndices,
            onJump = { index = it },
            onDismiss = { jumpOpen = false },
        )
    }

    if (giveUpConfirmOpen) {
        AlertDialog(
            onDismissRequest = { giveUpConfirmOpen = false },
            title = { Text("풀이를 포기할까요?", color = palette.textPrimary) },
            text = {
                Text(
                    "지금까지의 답안은 저장되지 않습니다.",
                    color = palette.textMuted,
                )
            },
            confirmButton = {
                AppButton(
                    text = "포기하기",
                    onClick = {
                        giveUpConfirmOpen = false
                        onCancel()
                    },
                    variant = AppButtonVariant.Destructive,
                    size = AppButtonSize.Compact,
                )
            },
            dismissButton = {
                AppButton(
                    text = "계속 풀기",
                    onClick = { giveUpConfirmOpen = false },
                    variant = AppButtonVariant.Tertiary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

/**
 * 러너 상단 헤더 — 56dp Row + 1dp hairline.
 *  - 좌: 닫기 (40dp Box.clickable Icon)
 *  - 중: durationSeconds > 0 이면 AppProgressPillTimer, 아니면 AppProgressPill
 *  - 우: (옵션) 책갈피 + 점프 + 더보기(신고 / 포기)
 */
@Composable
private fun RunnerHeader(
    title: String,
    currentIndex: Int,
    totalQuestions: Int,
    remainingSeconds: Int,
    durationSeconds: Int,
    isBookmarked: Boolean,
    onClose: () -> Unit,
    onToggleBookmark: (() -> Unit)?,
    onJumpClick: () -> Unit,
    onReportClick: (() -> Unit)?,
    onGiveUp: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
) {
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
            HeaderIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "풀이 닫기",
                tint = palette.textPrimary,
                onClick = onClose,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                    maxLines = 1,
                )
                if (durationSeconds > 0) {
                    AppProgressPillTimer(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = durationSeconds,
                    )
                } else {
                    AppProgressPill(
                        current = currentIndex + 1,
                        total = totalQuestions,
                    )
                }
            }
            if (onToggleBookmark != null) {
                HeaderIconButton(
                    icon = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "북마크 해제" else "북마크",
                    tint = if (isBookmarked) palette.accent else palette.textMuted,
                    onClick = onToggleBookmark,
                )
            }
            HeaderIconButton(
                icon = Icons.Outlined.AppRegistration,
                contentDescription = "문제 이동",
                tint = palette.textMuted,
                onClick = onJumpClick,
            )
            Box {
                HeaderIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "메뉴",
                    tint = palette.textMuted,
                    onClick = { onMenuOpenChange(true) },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                ) {
                    if (onReportClick != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Report,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                )
                            },
                            text = { Text("신고하기", color = palette.textPrimary) },
                            onClick = {
                                onMenuOpenChange(false)
                                onReportClick()
                            },
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = null,
                                tint = palette.danger,
                            )
                        },
                        text = { Text("포기하기", color = palette.danger) },
                        onClick = {
                            onMenuOpenChange(false)
                            onGiveUp()
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
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(SqldRadius.full))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * 본문 마크다운/HTML 렌더. ensureCodeFences → splitMarkdownSegments → 분기.
 *  - 일반 = Markwon TextView
 *  - 코드블록 = CodeBlockCard (runner 패키지 변형 — palette/AppBadge chrome)
 *  - SVG/이미지 = ui/common 위젯
 *
 * QuestionRunnerScreen 만의 사용처. SoloMarkdownContent 와 코드블록 chrome 만 다르다.
 */
@Composable
private fun MarkdownContent(text: String, textSizeSp: Float = 16f) {
    val segments = remember(text) {
        com.sqldpass.app.text.splitMarkdownSegments(
            com.sqldpass.app.text.ensureCodeFences(text)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        segments.forEach { seg ->
            when (seg) {
                is com.sqldpass.app.text.MarkdownSegment.Markdown ->
                    MarkwonTextView(text = seg.text, textSizeSp = textSizeSp)
                is com.sqldpass.app.text.MarkdownSegment.CodeBlock ->
                    CodeBlockCard(language = seg.language, code = seg.code)
                is com.sqldpass.app.text.MarkdownSegment.InlineSvg ->
                    com.sqldpass.app.ui.common.InlineSvgView(svgXml = seg.svgXml)
                is com.sqldpass.app.text.MarkdownSegment.Image ->
                    com.sqldpass.app.ui.common.RemoteImageView(src = seg.src, alt = seg.alt)
            }
        }
    }
}

@Composable
private fun MarkwonTextView(text: String, textSizeSp: Float) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val palette = LocalSqldpassPalette.current
    val textColor = palette.textPrimary.toArgb()
    val markwon = remember(ctx) { com.sqldpass.app.text.SqldpassMarkwon.get(ctx) }
    val spanned = remember(text, markwon) { markwon.toMarkdown(text) }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { c ->
            TextView(c).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                textSize = textSizeSp
                setLineSpacing(6f, 1.05f)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.textSize = textSizeSp
            markwon.setParsedMarkdown(view, spanned)
        },
    )
}

@Composable
private fun EmptyRunnerState(title: String, onCancel: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page)
            .padding(SqldSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Text(
            "문제를 불러올 수 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
        AppButton(
            text = "닫기",
            onClick = onCancel,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.Regular,
        )
    }
}

private fun isShortAnswer(type: String?): Boolean =
    type.equals("SHORT_ANSWER", ignoreCase = true) ||
        type.equals("DESCRIPTIVE", ignoreCase = true)

private val sampleQuestions = listOf(
    RunnerQuestion(
        id = 1L,
        displayOrder = 1,
        content = "<b>다음 중 SQL 조인 종류가 아닌 것은?</b><br/>1. INNER<br/>2. LEFT OUTER<br/>3. CROSS<br/>4. UNION",
        questionType = "MCQ",
    ),
    RunnerQuestion(
        id = 2L,
        displayOrder = 2,
        content = "정규화 1NF 의 조건을 한 줄로 설명하세요.",
        questionType = "SHORT_ANSWER",
    ),
)

@Preview(name = "Runner — Light", showBackground = true)
@Composable
private fun RunnerPreviewLight() {
    SqldpassTheme(darkTheme = false) {
        QuestionRunnerScreen(
            title = "SQLD 모의고사 1회",
            questions = sampleQuestions,
            onCancel = {}, onSubmit = {},
            durationSeconds = 5400,
            bookmarkedIds = setOf(1L),
            onToggleBookmark = {},
            onReport = {},
        )
    }
}

@Preview(name = "Runner — Dark", showBackground = true)
@Composable
private fun RunnerPreviewDark() {
    SqldpassTheme(darkTheme = true) {
        QuestionRunnerScreen(
            title = "기출복원 2024 1회",
            questions = sampleQuestions,
            onCancel = {}, onSubmit = {},
            durationSeconds = 0,
        )
    }
}
