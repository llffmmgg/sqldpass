package com.sqldpass.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius

/**
 * Deprecated — Step 4 그룹 D 에서 [AppCard] (accent = AppCardAccent.*) 로 통합.
 *
 * AppCard 의 accent 는 enum (Sqld/Success/Warning/... ) 만 받기 때문에 임의 [Color]
 * 를 받던 본 wrapper 는 별도 layout 으로 잔존하되 surface/border 는 palette 토큰을
 * 따른다. 새 코드는 `AppCard(accent = AppCardAccent.X)` 를 사용할 것.
 *
 * 현재 호출처 없음 (예전 "오늘 바로 풀기 CTA" 잔재). API 만 유지.
 */
@Deprecated(
    message = "AppCard(accent = AppCardAccent.*) 로 통합되었습니다.",
    replaceWith = ReplaceWith(
        "AppCard(accent = AppCardAccent.Sqld) { content() }",
    ),
)
@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    accentColor: Color = LocalSqldpassPalette.current.accent,
    content: @Composable () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SqldRadius.lg))
            .background(palette.card),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        ) {
            // 좌측 accent rail (AppCard 와 동일한 4dp).
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
