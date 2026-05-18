package com.sqldpass.app.ui.runner

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AppRegistration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sqldpass.app.ui.theme.SqldpassTheme
import kotlinx.coroutines.delay

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "풀이 닫기")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1} / ${questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (timerEnabled) {
                        val danger = remainingSec in 1..300 // 5분 이하
                        Text(
                            formatRemaining(remainingSec),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (danger) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (onToggleBookmark != null) {
                val isBookmarked = current.id in bookmarkedIds
                IconButton(
                    onClick = { onToggleBookmark(current.id) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        if (isBookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isBookmarked) "즐겨찾기 해제" else "즐겨찾기",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = { jumpOpen = true },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.AppRegistration, contentDescription = "문제 이동")
            }
            if (onReport != null) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "메뉴")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null) },
                            text = { Text("이 문제 신고") },
                            onClick = {
                                menuOpen = false
                                onReport(current.id)
                            },
                        )
                    }
                }
            }
        }
        LinearProgressIndicator(
            progress = { (index + 1f) / questions.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Q${current.displayOrder.takeIf { it > 0 } ?: (index + 1)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val parsed = current.parsed
            MarkdownContent(text = parsed.body.ifBlank { current.content })

            if (isShortAnswer(current.questionType)) {
                val focus = LocalFocusManager.current
                OutlinedTextField(
                    value = draft.answerText.orEmpty(),
                    onValueChange = { drafts[current.id] = draft.copy(answerText = it) },
                    label = { Text("답안 입력") },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = parsed.options
                    val displayCount = if (options.isNotEmpty()) options.size else 4
                    (1..displayCount).forEach { option ->
                        val optionText = options.getOrNull(option - 1)
                        OptionRow(
                            optionNumber = option,
                            optionText = optionText,
                            selected = draft.selectedOption == option,
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
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = { if (index > 0) index -= 1 },
                enabled = index > 0 && !submitting,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) { Text("이전") }
            Button(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = {
                    if (isLast) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSubmit(questions.map { drafts[it.id] ?: RunnerAnswerDraft(it.id) })
                    } else {
                        index += 1
                    }
                },
                enabled = !submitting,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text(if (isLast) (if (submitting) "제출중…" else "제출") else "다음")
            }
        }
    }

    if (jumpOpen) {
        RunnerJumpGrid(
            total = questions.size,
            currentIndex = index,
            answeredIndices = answeredIndices,
            onJump = { index = it },
            onDismiss = { jumpOpen = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionRow(
    optionNumber: Int,
    optionText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = onClick,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "option-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200),
        label = "option-fg",
    )
    val accent = MaterialTheme.colorScheme.primary

    // Press 즉시 들어가는 느낌 — 토스/카카오 패턴.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "option-press",
    )

    // Selection bounce — 선택 직후 살짝 튀어오름 (1.04 → 1.0).
    var bounceTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (selected) {
            bounceTrigger = true
            kotlinx.coroutines.delay(140)
            bounceTrigger = false
        }
    }
    val bounceScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "option-bounce",
    )
    val combinedScale = pressScale * bounceScale

    Card(
        modifier = Modifier
            .graphicsLayer { scaleX = combinedScale; scaleY = combinedScale }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            ),
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (selected) BorderStroke(1.dp, accent) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 64.dp)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 좌측 액센트 바 — 선택 시 emerald 6dp, 미선택 시 투명
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (selected) 6.dp else 4.dp)
                    .background(if (selected) accent else androidx.compose.ui.graphics.Color.Transparent),
            )
            Spacer(Modifier.size(8.dp))
            // 번호 서클
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$optionNumber",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(10.dp))
            // 보기 텍스트 — Markwon 으로 렌더 (코드·표 포함 가능)
            Box(modifier = Modifier.weight(1f)) {
                if (optionText != null) {
                    MarkdownContent(text = optionText, textSizeSp = 14f)
                } else {
                    Text(
                        "${optionNumber}번",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (selected) "선택됨" else "미선택",
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 마크다운/HTML 본문 렌더.
 *  1) ensureCodeFences — 백엔드가 `<pre><code>` HTML 로 보낸 경우 fenced markdown 으로 정규화
 *  2) splitMarkdownSegments — 코드블록과 일반 텍스트 분리
 *  3) 일반 = Markwon TextView, 코드블록 = Compose Native `CodeBlockCard`
 */
@Composable
private fun MarkdownContent(text: String, textSizeSp: Float = 16f) {
    val segments = remember(text) {
        com.sqldpass.app.text.splitMarkdownSegments(
            com.sqldpass.app.text.ensureCodeFences(text)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
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
            view.setTextColor(onSurface)
            view.textSize = textSizeSp
            markwon.setParsedMarkdown(view, spanned)
        },
    )
}

@Composable
private fun EmptyRunnerState(title: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            "문제를 불러올 수 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            shape = RoundedCornerShape(ButtonCorner),
            onClick = onCancel,
            modifier = Modifier.sizeIn(minHeight = 48.dp),
        ) { Text("닫기") }
    }
}

private fun formatRemaining(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

private fun optionLabel(option: Int): String = when (option) {
    1 -> "① 1번"
    2 -> "② 2번"
    3 -> "③ 3번"
    4 -> "④ 4번"
    else -> "$option"
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
