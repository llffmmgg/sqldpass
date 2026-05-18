package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
 * Inked OMR 상단 히어로. 기존 HeroHeader(emerald 솔리드) 의 평면화 후속.
 *
 * - bg: `palette.page` — 그라데이션/이미지 없음.
 * - 하단 1dp hairline border.
 * - eyebrow(옵션, accent 색 letter-spaced 1.2sp).
 * - title displayMedium (28sp Black).
 * - 우측 상단 actions 슬롯 (메뉴 아이콘 등). title 과 같은 Row.
 * - 우측 mascot(옵션) 56dp.
 * - subtitle bodyLarge muted.
 *
 * TabScaffold(topBar = false) 와 짝으로 사용. HeroHeader 는 그대로 두고
 * 점진 마이그레이션 예정.
 */
@Composable
fun AppHero(
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    mascot: AppMascotPose? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSqldpassPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.page)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = SqldSpacing.md, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (eyebrow != null) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = palette.accent,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displayMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (mascot != null) {
                AppMascot(pose = mascot, sizeDp = 56)
            }
            actions()
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textMuted,
            )
        }
        // 1dp hairline bottom border
        Box(
            modifier = Modifier
                .padding(top = SqldSpacing.md)
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )
    }
}

@Preview(name = "AppHero — title only")
@Composable
private fun PreviewAppHeroTitle() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppHero(title = "홈")
        }
    }
}

@Preview(name = "AppHero — eyebrow + subtitle")
@Composable
private fun PreviewAppHeroEyebrow() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppHero(
                eyebrow = "오늘의 학습",
                title = "안녕, 회훈!",
                subtitle = "어제까지 12문제를 풀었어요. 오늘은 조금만 더.",
            )
        }
    }
}

@Preview(name = "AppHero — mascot")
@Composable
private fun PreviewAppHeroMascot() {
    SqldpassTheme(darkTheme = true) {
        Box(Modifier.background(LocalSqldpassPalette.current.page)) {
            AppHero(
                eyebrow = "RECOMMENDED",
                title = "추천 모의고사",
                subtitle = "지난 30일 오답률 기반",
                mascot = AppMascotPose.Greeting,
            )
        }
    }
}

@Preview(name = "AppHero — actions")
@Composable
private fun PreviewAppHeroActions() {
    SqldpassTheme(darkTheme = true) {
        val palette = LocalSqldpassPalette.current
        Box(Modifier.background(palette.page)) {
            AppHero(
                title = "내 정보",
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(palette.elevated, androidx.compose.foundation.shape.CircleShape),
                    )
                },
            )
        }
    }
}
