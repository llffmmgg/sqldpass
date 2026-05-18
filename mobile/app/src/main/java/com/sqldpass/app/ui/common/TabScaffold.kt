package com.sqldpass.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * 각 탭이 공유하는 상단 바 — scrollBehavior 로 컨텐츠 스크롤 시 축소.
 * BottomNavigation 은 상위 Scaffold 에서 처리한다.
 * topBar=false 면 TopAppBar 를 그리지 않고 contentPadding 도 0 — 히어로 헤더를 가진 탭(홈/대시보드)용.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    topBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (!topBar) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) { content(PaddingValues()) }
        return
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = { actions() },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) { content(padding) }
    }
}
