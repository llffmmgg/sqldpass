package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 화면/섹션 단위 상태 placeholder — Loading / Empty / Error 3종.
 *
 * - Loading: 28dp 동그란 progress + "불러오는 중…"
 * - Empty:   mascot(pose) + title + optional message + optional secondary action
 * - Error:   mascot(Review) + title + optional message + optional retry button (Secondary)
 */
sealed class AppViewState {
    data object Loading : AppViewState()
    data class Empty(
        val title: String,
        val message: String? = null,
        val mascot: AppMascotPose = AppMascotPose.Guide,
        val action: AppViewAction? = null,
    ) : AppViewState()
    data class ErrorState(
        val title: String,
        val message: String? = null,
        val onRetry: (() -> Unit)? = null,
    ) : AppViewState()
}

data class AppViewAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun AppStateView(
    state: AppViewState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(SqldSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is AppViewState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                ) {
                    AppLoadingIndicator()
                    Text(
                        text = "불러오는 중…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
            }
            is AppViewState.Empty -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                ) {
                    AppMascot(pose = state.mascot, sizeDp = 96)
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (state.message != null) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (state.action != null) {
                        AppButton(
                            text = state.action.label,
                            onClick = state.action.onClick,
                            variant = AppButtonVariant.Secondary,
                        )
                    }
                }
            }
            is AppViewState.ErrorState -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                ) {
                    AppMascot(pose = AppMascotPose.Review, sizeDp = 96)
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (state.message != null) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (state.onRetry != null) {
                        AppButton(
                            text = "다시 시도",
                            onClick = state.onRetry,
                            variant = AppButtonVariant.Secondary,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "AppStateView — Loading")
@Composable
private fun PreviewAppStateViewLoading() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppStateView(state = AppViewState.Loading)
        }
    }
}

@Preview(name = "AppStateView — Empty")
@Composable
private fun PreviewAppStateViewEmpty() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppStateView(
                state = AppViewState.Empty(
                    title = "북마크한 문제가 없어요",
                    message = "풀이 화면 우측 상단의 책갈피 아이콘으로 자주 보는 문제를 저장하세요.",
                    mascot = AppMascotPose.Guide,
                    action = AppViewAction(label = "문제 풀러 가기", onClick = {}),
                ),
            )
        }
    }
}

@Preview(name = "AppStateView — Error")
@Composable
private fun PreviewAppStateViewError() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppStateView(
                state = AppViewState.ErrorState(
                    title = "네트워크에 연결할 수 없어요",
                    message = "잠시 후 다시 시도해주세요.",
                    onRetry = {},
                ),
            )
        }
    }
}
