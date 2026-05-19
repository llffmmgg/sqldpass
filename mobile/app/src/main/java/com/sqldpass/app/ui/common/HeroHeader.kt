package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette

/**
 * 상단 system bar 영역까지 primary 솔리드로 채우는 히어로 헤더.
 * TabScaffold(topBar = false) 와 짝으로 사용 — 히어로가 타이틀을 흡수한다.
 */
@Composable
fun HeroHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    val bg = palette.accent
    val fg = palette.accentFg
    CompositionLocalProvider(LocalContentColor provides fg) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(bg)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = fg,
                )
                actions()
            }
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg,
                )
            }
        }
    }
}
