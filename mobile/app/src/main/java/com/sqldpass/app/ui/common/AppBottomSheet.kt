package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * Inked OMR 바텀시트 primitive.
 *
 * Material3 `ModalBottomSheet` 위에 얇은 wrapper. `ExperimentalMaterial3Api` 어노테이션은
 * 본 함수 내부에 격리해 외부 시그니처에는 노출하지 않는다.
 *
 * - container: `LocalSqldpassPalette.current.card`
 * - shape: 상단 모서리만 `SqldRadius.xxl` 로 라운드
 * - drag handle: `showDragHandle=true` 면 `BottomSheetDefaults.DragHandle()`, 아니면 빈 슬롯
 * - 내부 Column: `navigationBarsPadding` + 수평 `SqldSpacing.lg` + 수직 `SqldSpacing.base`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalSqldpassPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = palette.card,
        shape = RoundedCornerShape(topStart = SqldRadius.xxl, topEnd = SqldRadius.xxl),
        dragHandle = if (showDragHandle) {
            { BottomSheetDefaults.DragHandle() }
        } else {
            {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = SqldSpacing.lg,
                    vertical = SqldSpacing.base,
                ),
            content = content,
        )
    }
}

@Preview(name = "AppBottomSheet — With drag handle")
@Composable
private fun PreviewAppBottomSheetWithHandle() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            AppBottomSheet(onDismiss = {}, showDragHandle = true) {
                Text(
                    "정렬 옵션",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalSqldpassPalette.current.textPrimary,
                )
                Text(
                    "원하는 정렬 기준을 선택하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalSqldpassPalette.current.textMuted,
                )
            }
        }
    }
}

@Preview(name = "AppBottomSheet — No drag handle")
@Composable
private fun PreviewAppBottomSheetNoHandle() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            AppBottomSheet(onDismiss = {}, showDragHandle = false) {
                Text(
                    "필터",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalSqldpassPalette.current.textPrimary,
                )
                Text(
                    "오답만 보기, 북마크만 보기 등 세부 필터를 설정합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalSqldpassPalette.current.textMuted,
                )
            }
        }
    }
}
