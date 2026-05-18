package com.sqldpass.app.ui.runner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.AppTextField
import com.sqldpass.app.ui.theme.LocalSqldpassPalette

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
    val palette = LocalSqldpassPalette.current
    var selectedType by remember { mutableStateOf(FEEDBACK_TYPES.first().first) }
    var content by remember { mutableStateOf("") }
    val isValid = content.length in 5..2000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이 문제 신고하기", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "문항 #$questionId — 어떤 문제가 있나요?",
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
        confirmButton = {
            AppButton(
                text = if (submitting) "전송중…" else "신고",
                onClick = { onSubmit(selectedType, content) },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Compact,
                enabled = isValid && !submitting,
                loading = submitting,
            )
        },
        dismissButton = {
            AppButton(
                text = "취소",
                onClick = onDismiss,
                variant = AppButtonVariant.Tertiary,
                size = AppButtonSize.Compact,
            )
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
            AppChip(
                label = label,
                selected = value == selected,
                onClick = { onSelect(value) },
            )
        }
    }
}
