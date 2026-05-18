package com.sqldpass.app.ui.solve.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 단일 채점 풀이 상단 헤더 — 종료/진행도/북마크/신고 + 진행 바.
 */
@Composable
fun SoloProgressHeader(
    solvedCount: Int,
    totalCount: Int,
    correctCount: Int,
    isBookmarked: Boolean,
    onClose: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReport: () -> Unit,
) {
    val displayCurrent = (solvedCount + 1).coerceAtMost(totalCount)
    val progress by animateFloatAsState(
        targetValue = (solvedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200),
        label = "solo-progress",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "풀이 종료")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$displayCurrent / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "정답 $correctCount / $solvedCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    if (isBookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isBookmarked) "즐겨찾기 해제" else "즐겨찾기",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "메뉴")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null) },
                    text = { Text("이 문제 신고") },
                    onClick = {
                        menuOpen = false
                        onReport()
                    },
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
