package com.sqldpass.app.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun NicknameEditDialog(
    currentNickname: String?,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var value by remember { mutableStateOf(currentNickname.orEmpty()) }
    val valid = value.length in 2..30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("닉네임 변경") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("닉네임 (2~30자)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = valid && !submitting && value != currentNickname,
                onClick = { onSubmit(value) },
            ) { Text(if (submitting) "변경중…" else "변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
