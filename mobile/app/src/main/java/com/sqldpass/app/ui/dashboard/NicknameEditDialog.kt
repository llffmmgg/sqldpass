package com.sqldpass.app.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sqldpass.app.ui.common.AppDialog
import com.sqldpass.app.ui.common.AppTextField

@Composable
fun NicknameEditDialog(
    currentNickname: String?,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var value by remember { mutableStateOf(currentNickname.orEmpty()) }
    val valid = value.length in 2..30
    val canSubmit = valid && !submitting && value != currentNickname

    AppDialog(
        onDismiss = onDismiss,
        title = "닉네임 변경",
        content = {
            AppTextField(
                value = value,
                onValueChange = { value = it },
                label = "닉네임 (2~30자)",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmLabel = if (submitting) "변경중…" else "변경",
        onConfirm = { if (canSubmit) onSubmit(value) },
        dismissLabel = "취소",
        onDismissAction = onDismiss,
    )
}
