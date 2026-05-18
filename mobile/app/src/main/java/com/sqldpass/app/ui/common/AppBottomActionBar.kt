package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 화면 하단 고정 액션 바. AppButton(size=Large) 1~2개를 IME/내비 인셋 위에 띄운다.
 *
 * - primary 만: 풀폭.
 * - primary + secondary: secondary weight 1f / primary weight 2f. primary 가 항상 더 크다.
 * - 상단 1dp hairline 만, shadow 없음.
 */
data class BottomAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val variant: AppButtonVariant = AppButtonVariant.Primary,
)

@Composable
fun AppBottomActionBar(
    primary: BottomAction,
    secondary: BottomAction? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.card),
    ) {
        // 1dp hairline top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .defaultMinSize(minHeight = 56.dp)
                .padding(
                    horizontal = SqldSpacing.base,
                    vertical = SqldSpacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            if (secondary != null) {
                AppButton(
                    text = secondary.label,
                    onClick = secondary.onClick,
                    variant = secondary.variant,
                    size = AppButtonSize.Large,
                    enabled = secondary.enabled,
                    loading = secondary.loading,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = primary.label,
                    onClick = primary.onClick,
                    variant = primary.variant,
                    size = AppButtonSize.Large,
                    enabled = primary.enabled,
                    loading = primary.loading,
                    modifier = Modifier.weight(2f),
                )
            } else {
                AppButton(
                    text = primary.label,
                    onClick = primary.onClick,
                    variant = primary.variant,
                    size = AppButtonSize.Large,
                    enabled = primary.enabled,
                    loading = primary.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "AppBottomActionBar — variants")
@Composable
private fun PreviewAppBottomActionBar() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(top = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppBottomActionBar(
                    primary = BottomAction(label = "제출하기", onClick = {}),
                )
                AppBottomActionBar(
                    primary = BottomAction(label = "다음 문제", onClick = {}),
                    secondary = BottomAction(
                        label = "이전",
                        onClick = {},
                        variant = AppButtonVariant.Secondary,
                    ),
                )
                AppBottomActionBar(
                    primary = BottomAction(
                        label = "채점 중",
                        onClick = {},
                        loading = true,
                    ),
                )
                AppBottomActionBar(
                    primary = BottomAction(
                        label = "다음",
                        onClick = {},
                        enabled = false,
                    ),
                    secondary = BottomAction(
                        label = "건너뛰기",
                        onClick = {},
                        variant = AppButtonVariant.Tertiary,
                    ),
                )
            }
        }
    }
}
