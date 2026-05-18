package com.sqldpass.app.ui.runner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.common.AppDialog
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassMonoText

/**
 * 풀이 중 문제 점프 그리드 — 5열 LazyVerticalGrid.
 *
 * 셀 상태별 컬러:
 *  - Unanswered: card bg + border + textMuted.
 *  - Answered:   accentSoftBg + accent border + accent text.
 *  - Current:    accent fill + accentFg text.
 *  - Bookmarked: answered 와 동일 + 우상단 작은 별표 dot (warning).
 *
 * AppDialog content slot 위에 LazyVerticalGrid 를 올린다. 닫기 버튼은 confirm 슬롯(Tertiary 톤).
 */
@Composable
fun RunnerJumpGrid(
    total: Int,
    currentIndex: Int,
    answeredIndices: Set<Int>,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
    bookmarkedIndices: Set<Int> = emptySet(),
) {
    val palette = LocalSqldpassPalette.current
    AppDialog(
        onDismiss = onDismiss,
        title = "문제 이동",
        content = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(SqldSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            ) {
                items((0 until total).toList()) { idx ->
                    val isCurrent = idx == currentIndex
                    val isAnswered = idx in answeredIndices
                    val isBookmarked = idx in bookmarkedIndices

                    val bg = when {
                        isCurrent -> palette.accent
                        isAnswered -> palette.accentSoftBg
                        else -> palette.card
                    }
                    val numberColor = when {
                        isCurrent -> palette.accentFg
                        isAnswered -> palette.accent
                        else -> palette.textMuted
                    }
                    val borderColor = when {
                        isCurrent -> palette.accent
                        isAnswered -> palette.accent
                        else -> palette.border
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(SqldRadius.sm))
                            .background(bg)
                            .border(
                                BorderStroke(1.dp, borderColor),
                                RoundedCornerShape(SqldRadius.sm),
                            )
                            .clickable {
                                onJump(idx)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${idx + 1}",
                            style = SqldpassMonoText.small,
                            color = numberColor,
                        )
                        if (isBookmarked) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.StarRate,
                                    contentDescription = null,
                                    tint = palette.warning,
                                    modifier = Modifier.size(8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmLabel = "닫기",
        onConfirm = onDismiss,
        dismissLabel = null,
    )
}
