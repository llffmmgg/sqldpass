package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Inked OMR 다이얼로그 primitive.
 *
 * Material3 `AlertDialog` 를 우회하고 `androidx.compose.ui.window.Dialog` 위에 시각 chrome 을
 * 직접 그린다 — surface 색, 라운드, 패딩, 액션 버튼을 모두 `LocalSqldpassPalette` /
 * `SqldRadius` / `SqldSpacing` / `AppButton` 토큰만으로 구성해 진정한 visible identity 격리.
 *
 * - title (선택) + message (선택) + content slot (선택) 로 본문 구성.
 * - confirm: 항상 표시. destructive=true 면 `AppButtonVariant.Destructive`.
 * - dismiss: `dismissLabel` 이 null 이면 생략.
 * - 백드롭 탭 / 시스템 백 → `onDismiss`.
 */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    confirmLabel: String = "확인",
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = "취소",
    onDismissAction: (() -> Unit)? = null,
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val palette = LocalSqldpassPalette.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = SqldSpacing.lg)
                .widthIn(min = 280.dp, max = 360.dp)
                .clip(RoundedCornerShape(SqldRadius.xl))
                .background(palette.card)
                .padding(SqldSpacing.lg),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
                if (content != null) {
                    content()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SqldSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(
                        SqldSpacing.sm,
                        alignment = Alignment.End,
                    ),
                ) {
                    if (dismissLabel != null) {
                        AppButton(
                            text = dismissLabel,
                            onClick = { onDismissAction?.invoke() ?: onDismiss() },
                            variant = AppButtonVariant.Tertiary,
                            size = AppButtonSize.Compact,
                        )
                    }
                    AppButton(
                        text = confirmLabel,
                        onClick = { onConfirm?.invoke() ?: onDismiss() },
                        variant = if (destructive) {
                            AppButtonVariant.Destructive
                        } else {
                            AppButtonVariant.Primary
                        },
                        size = AppButtonSize.Compact,
                    )
                }
            }
        }
    }
}

@Preview(name = "AppDialog — Title only")
@Composable
private fun PreviewAppDialogTitleOnly() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDialog(
                onDismiss = {},
                title = "저장하시겠어요?",
                dismissLabel = null,
            )
        }
    }
}

@Preview(name = "AppDialog — Confirm + Dismiss")
@Composable
private fun PreviewAppDialogConfirmDismiss() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDialog(
                onDismiss = {},
                title = "변경사항을 저장할까요?",
                message = "아직 저장되지 않은 변경사항이 있어요. 지금 저장하지 않으면 사라집니다.",
                confirmLabel = "저장",
                dismissLabel = "취소",
            )
        }
    }
}

@Preview(name = "AppDialog — Destructive")
@Composable
private fun PreviewAppDialogDestructive() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDialog(
                onDismiss = {},
                title = "계정을 삭제할까요?",
                message = "이 작업은 되돌릴 수 없어요. 모든 학습 기록이 함께 삭제됩니다.",
                confirmLabel = "삭제",
                dismissLabel = "취소",
                destructive = true,
            )
        }
    }
}

@Preview(name = "AppDialog — Custom content")
@Composable
private fun PreviewAppDialogCustomContent() {
    SqldpassTheme(darkTheme = true) {
        Box(
            Modifier
                .background(LocalSqldpassPalette.current.page)
                .padding(16.dp),
        ) {
            AppDialog(
                onDismiss = {},
                title = "닉네임 변경",
                confirmLabel = "변경",
                dismissLabel = "취소",
                content = {
                    AppTextField(
                        value = "heehun",
                        onValueChange = {},
                        label = "새 닉네임",
                        placeholder = "닉네임 입력",
                    )
                },
            )
        }
    }
}
