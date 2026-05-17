package com.sqldpass.app.ui.runner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FEEDBACK_TYPES = listOf(
    "WRONG_ANSWER" to "정답 오류",
    "TYPO" to "오탈자",
    "UNCLEAR" to "지문 모호",
    "OUT_OF_SCOPE" to "출제 범위 외",
    "OTHER" to "기타",
)

@Composable
fun ReportDialog(
    questionId: Long,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, content: String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(FEEDBACK_TYPES.first().first) }
    var content by remember { mutableStateOf("") }
    val isValid = content.length in 5..2000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이 문제 신고하기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "문항 #$questionId — 어떤 문제가 있나요?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FeedbackTypeChips(
                    selected = selectedType,
                    onSelect = { selectedType = it },
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("상세 내용 (5~2000자)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValid && !submitting,
                onClick = { onSubmit(selectedType, content) },
            ) { Text(if (submitting) "전송중…" else "신고") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackTypeChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FEEDBACK_TYPES.forEach { (value, label) ->
            val isSelected = value == selected
            AssistChip(
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = if (isSelected) AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) else AssistChipDefaults.assistChipColors(),
            )
        }
    }
}
