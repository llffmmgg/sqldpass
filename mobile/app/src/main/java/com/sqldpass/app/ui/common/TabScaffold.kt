package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 각 탭이 공유하는 상단 헤더 — 시각 시그니처는 LocalSqldpassPalette + SqldSpacing.
 *
 * phase `android-polish-and-shared-renderer` step 5 에서 Material3 `TopAppBar` 제거. scrollBehavior
 * 도 제거 — 본 phase 는 정적 헤더만. 스크롤 시 collapse 동작은 별 phase 의 작업.
 *
 * topBar=false 면 헤더를 그리지 않음 — 자체 히어로 헤더(예: AppHero)를 가진 탭(홈/대시보드)용.
 */
@Composable
fun TabScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    topBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    if (!topBar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.page),
        ) { content(PaddingValues()) }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = SqldSpacing.lg,
                    end = SqldSpacing.sm,
                    top = SqldSpacing.sm,
                    bottom = SqldSpacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            )
            actions()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues())
        }
    }
}
