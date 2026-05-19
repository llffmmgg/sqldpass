package com.sqldpass.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette

/**
 * 후속 step 에서 채울 빈 화면 헬퍼. Hero + 한 줄 안내.
 */
@Composable
fun PlaceholderTab(title: String, body: String) {
    val palette = LocalSqldpassPalette.current
    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(title = title)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
        }
    }
}
