package com.sqldpass.app.ui.common

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sqldpass.app.text.MarkdownSegment
import com.sqldpass.app.text.SqldpassMarkwon
import com.sqldpass.app.text.ensureCodeFences
import com.sqldpass.app.text.splitMarkdownSegments
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * Solo Solve 화면 전용 본문 렌더 — Markdown / Code / Inline SVG / Image 분리.
 *
 * QuestionRunnerScreen 의 동등 패턴을 ui/common 으로 분리해 단일 채점 풀이 화면이 재사용.
 * QuestionRunnerScreen 본체는 그대로 두어 모의고사 응시 회귀 0.
 */
@Composable
fun SoloMarkdownContent(text: String, textSizeSp: Float = 16f) {
    val segments = remember(text) {
        splitMarkdownSegments(ensureCodeFences(text))
    }
    Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
        segments.forEach { seg ->
            when (seg) {
                is MarkdownSegment.Markdown ->
                    SoloMarkwonTextView(text = seg.text, textSizeSp = textSizeSp)
                is MarkdownSegment.CodeBlock ->
                    CodeBlockCard(language = seg.language, code = seg.code)
                is MarkdownSegment.InlineSvg ->
                    InlineSvgView(svgXml = seg.svgXml)
                is MarkdownSegment.Image ->
                    RemoteImageView(src = seg.src, alt = seg.alt)
            }
        }
    }
}

@Composable
private fun SoloMarkwonTextView(text: String, textSizeSp: Float) {
    val ctx = LocalContext.current
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
    val markwon = remember(ctx) { SqldpassMarkwon.get(ctx) }
    val spanned = remember(text, markwon) { markwon.toMarkdown(text) }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { c ->
            TextView(c).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                textSize = textSizeSp
                setLineSpacing(6f, 1.05f)
            }
        },
        update = { view ->
            view.setTextColor(onSurface)
            view.textSize = textSizeSp
            markwon.setParsedMarkdown(view, spanned)
        },
    )
}

@Composable
private fun CodeBlockCard(language: String?, code: String) {
    // 단순 mono 표시 — 가로 스크롤은 TextView 가 자체 처리.
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = (if (!language.isNullOrBlank()) "[$language]\n" else "") + code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
