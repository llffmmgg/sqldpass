package com.sqldpass.app.ui.runner

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.sqldpass.app.ui.theme.SqldpassTheme

private val CardCorner = 12.dp
private val ButtonCorner = 12.dp

@Composable
fun QuestionRunnerScreen(
    title: String,
    questions: List<RunnerQuestion>,
    onCancel: () -> Unit,
    onSubmit: (List<RunnerAnswerDraft>) -> Unit,
    submitting: Boolean = false,
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
    val current = questions[index]
    val draft = drafts[current.id] ?: RunnerAnswerDraft(questionId = current.id)
    val isLast by remember { derivedStateOf { index == questions.lastIndex } }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "풀이 닫기",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${index + 1} / ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            HtmlContent(html = current.content)

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
                    (1..4).forEach { option ->
                        OptionRow(
                            label = optionLabel(option),
                            selected = draft.selectedOption == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                drafts[current.id] = draft.copy(selectedOption = option)
                            },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.size(4.dp))
            Text(
                label,
                style = if (selected) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun HtmlContent(html: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                textSize = 16f
                setLineSpacing(8f, 1f)
            }
        },
        update = { view ->
            view.setTextColor(onSurface)
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
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
        )
    }
}

@Preview(name = "Runner — Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun RunnerPreviewLargeFont() {
    SqldpassTheme(darkTheme = false) {
        QuestionRunnerScreen(
            title = "랜덤 10문제",
            questions = sampleQuestions,
            onCancel = {}, onSubmit = {},
        )
    }
}
