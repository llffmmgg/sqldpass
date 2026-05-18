package com.sqldpass.app.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.BookmarkSummary
import com.sqldpass.app.text.formatKstDate
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppMascotPose
import com.sqldpass.app.ui.common.AppStateView
import com.sqldpass.app.ui.common.AppViewState
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 마이 > 북마크한 문제. iOS BookmarksView 와 동치 — 단, Android Inked OMR 톤으로 평면화.
 *
 * 진입 시 GET /api/bookmarks 로 목록 fetch.  toggleBookmark 는 ViewModel 의 동일 메서드
 * 재사용(낙관적 제거 + DELETE 자동 호출). 본 화면에서는 항목 우측 북마크 아이콘 탭이
 * "제거" 액션 — iOS swipe action 의 Android 변형.
 */
@Composable
fun BookmarksScreen(
    state: AppUiState,
    onLoadBookmarks: () -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) onLoadBookmarks()
    }

    val palette = LocalSqldpassPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        BookmarksTopBar(onBack = onBack)

        when {
            state.bookmarksLoading && state.bookmarks.isEmpty() -> {
                AppStateView(
                    state = AppViewState.Loading,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.bookmarksError != null && state.bookmarks.isEmpty() -> {
                AppStateView(
                    state = AppViewState.ErrorState(
                        title = "북마크를 불러오지 못했어요",
                        message = state.bookmarksError,
                        onRetry = onLoadBookmarks,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.bookmarks.isEmpty() -> {
                AppStateView(
                    state = AppViewState.Empty(
                        title = "북마크가 없어요",
                        message = "풀이 화면 상단의 책갈피 아이콘으로 자주 보는 문제를 저장하세요.",
                        mascot = AppMascotPose.Guide,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SqldSpacing.lg - SqldSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
                ) {
                    items(state.bookmarks, key = { it.questionId }) { item ->
                        BookmarkRow(
                            bookmark = item,
                            onToggle = { onToggleBookmark(item.questionId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTopBar(onBack: () -> Unit) {
    val palette = LocalSqldpassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.page)
            .statusBarsPadding()
            .padding(
                start = SqldSpacing.sm,
                end = SqldSpacing.lg,
                top = SqldSpacing.sm,
                bottom = SqldSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "뒤로가기",
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "북마크",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkSummary,
    onToggle: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        bookmark.subjectName ?: "과목 미지정",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                    formatKstDate(bookmark.createdAt)?.let { date ->
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSubtle,
                        )
                        Text(
                            date,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSubtle,
                        )
                    }
                }
                Text(
                    bookmark.questionContent.trim().take(160),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 우측 북마크 아이콘 — 탭 시 해제 (낙관적 제거).
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Bookmark,
                    contentDescription = "북마크 해제",
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
