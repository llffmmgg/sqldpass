package com.sqldpass.app.ui.runner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.AppDialog
import com.sqldpass.app.ui.common.AppTextField
import com.sqldpass.app.ui.theme.LocalSqldpassPalette

private val FEEDBACK_TYPES = listOf(
    "QUESTION_ERROR" to "문항 오류",
    "BUG" to "앱 오류",
    "FEATURE" to "개선 제안",
    "OTHER" to "기타",
)

@Composable
fun ReportDialog(
    questionId: Long,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, content: String) -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    var selectedType by remember { mutableStateOf(FEEDBACK_TYPES.first().first) }
    var content by remember { mutableStateOf("") }
    val isValid = content.length in 5..2000
    val canSubmit = isValid && !submitting

    AppDialog(
        onDismiss = onDismiss,
        title = "문제 신고하기",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "문항 #$questionId 에 어떤 문제가 있나요?",
                    color = palette.textMuted,
                )
                FeedbackTypeChips(
                    selected = selectedType,
                    onSelect = { selectedType = it },
                )
                AppTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "상세 내용",
                    placeholder = "5자 이상 2000자 이내로 작성해주세요.",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmLabel = if (submitting) "전송 중" else "신고",
        onConfirm = { if (canSubmit) onSubmit(selectedType, content) },
        dismissLabel = "취소",
        onDismissAction = onDismiss,
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
            AppChip(
                label = label,
                selected = value == selected,
                onClick = { onSelect(value) },
            )
        }
    }
}
