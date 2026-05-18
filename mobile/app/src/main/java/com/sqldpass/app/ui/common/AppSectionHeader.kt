package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 섹션 헤더 — eyebrow (accent caption, letter-spacing 강조) + title + 선택적 우측 액션.
 */
data class SectionAction(val label: String, val onClick: () -> Unit)

@Composable
fun AppSectionHeader(
    title: String,
    eyebrow: String? = null,
    action: SectionAction? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SqldSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = palette.accent,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
            )
        }
        if (action != null) {
            Row(
                modifier = Modifier.clickable(onClick = action.onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accent,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(name = "AppSectionHeader — variants")
@Composable
private fun PreviewAppSectionHeader() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppSectionHeader(title = "오늘의 학습")
                AppSectionHeader(
                    eyebrow = "RECOMMENDED",
                    title = "추천 모의고사",
                )
                AppSectionHeader(
                    title = "북마크",
                    action = SectionAction(label = "전체보기", onClick = {}),
                )
                AppSectionHeader(
                    eyebrow = "지난 7일",
                    title = "최근 풀이",
                    action = SectionAction(label = "더보기", onClick = {}),
                )
            }
        }
    }
}
