package com.sqldpass.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

/**
 * 문제 본문에 박힌 inline `<svg>...</svg>` 청크를 Compose Image 로 렌더.
 *
 * SVG XML 문자열을 그대로 Coil 의 ImageRequest data 로 넘기고,
 * SvgDecoder 가 디코딩한다. CDN/네트워크 호출 없음.
 */
@Composable
fun InlineSvgView(svgXml: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val loader = remember(ctx) {
        ImageLoader.Builder(ctx)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val request = remember(svgXml, ctx) {
        ImageRequest.Builder(ctx)
            .data(svgXml.toByteArray(Charsets.UTF_8))
            .build()
    }
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            imageLoader = loader,
            contentDescription = "문제 도식",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
