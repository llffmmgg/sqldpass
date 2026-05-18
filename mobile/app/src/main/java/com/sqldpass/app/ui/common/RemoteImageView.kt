package com.sqldpass.app.ui.common

import android.util.Base64
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
 * 문제 본문의 `<img src="...">` 를 Compose Image 로 렌더.
 *
 * 지원:
 *  - 외부 URL (http/https, png/jpg/gif/svg)
 *  - data URI (`data:image/...;base64,...` png/jpg/svg 모두)
 *
 * Coil ImageLoader 에 SvgDecoder 를 등록해 svg src 도 png 같이 처리.
 */
@Composable
fun RemoteImageView(src: String, alt: String?, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val loader = remember(ctx) {
        ImageLoader.Builder(ctx)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val model: Any = remember(src) {
        val trimmed = src.trim()
        if (trimmed.startsWith("data:", ignoreCase = true)) {
            // data URI — base64 페이로드만 떼서 ByteArray 로. SvgDecoder 가 SVG XML 도 처리.
            val idx = trimmed.indexOf(",")
            if (idx > 0) {
                val isBase64 = trimmed.substring(0, idx).contains("base64", ignoreCase = true)
                val payload = trimmed.substring(idx + 1)
                if (isBase64) {
                    runCatching { Base64.decode(payload, Base64.DEFAULT) }
                        .getOrDefault(payload.toByteArray(Charsets.UTF_8))
                } else {
                    // URL-encoded SVG 등 — UTF-8 바이트로 그대로.
                    java.net.URLDecoder.decode(payload, "UTF-8").toByteArray(Charsets.UTF_8)
                }
            } else trimmed
        } else trimmed
    }
    val request = remember(model, ctx) {
        ImageRequest.Builder(ctx).data(model).build()
    }
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            imageLoader = loader,
            contentDescription = alt.orEmpty().ifBlank { "문제 이미지" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
