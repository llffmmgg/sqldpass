package com.sqldpass.app.text

/**
 * 마크다운 본문을 코드블록(fenced) / inline SVG / 일반 텍스트로 분리.
 *
 * Markwon 한 덩어리 TextView 안에서는 코드블록·SVG 만 따로 Compose 컴포저블로
 * 렌더할 수 없으므로, 클라이언트 측에서 split 한 뒤 segments 를 순차 렌더.
 *
 * - fenced 코드(```lang\n...```): 별 CodeBlockCard 로
 * - inline `<svg>...</svg>` XML: Coil + SVG 디코더로 Compose Image
 * - 나머지: Markwon Spannable
 *
 * 비-fenced 코드(4-space 들여쓰기) 는 다루지 않음 — 우리 컨텐츠는 fenced 가정.
 */
sealed interface MarkdownSegment {
    data class Markdown(val text: String) : MarkdownSegment
    data class CodeBlock(val language: String?, val code: String) : MarkdownSegment
    data class InlineSvg(val svgXml: String) : MarkdownSegment
}

private val FENCED_CODE_REGEX = Regex(
    "```([a-zA-Z0-9_+\\-]*)\\s*\\n([\\s\\S]*?)```",
)

// 문제 본문에 박힌 inline SVG — 한 줄/여러 줄 모두 대응.
private val INLINE_SVG_REGEX = Regex(
    "<svg\\b[^>]*>[\\s\\S]*?</svg>",
    RegexOption.IGNORE_CASE,
)

private data class Hit(val range: IntRange, val segment: MarkdownSegment)

fun splitMarkdownSegments(text: String): List<MarkdownSegment> {
    val hits = mutableListOf<Hit>()

    FENCED_CODE_REGEX.findAll(text).forEach { m ->
        hits += Hit(
            m.range,
            MarkdownSegment.CodeBlock(
                language = m.groupValues[1].takeIf { it.isNotBlank() },
                code = m.groupValues[2].trimEnd('\n', ' '),
            ),
        )
    }
    INLINE_SVG_REGEX.findAll(text).forEach { m ->
        // 코드블록 내부 svg 는 무시 — 코드 예시일 수 있음.
        val insideCode = hits.any { it.segment is MarkdownSegment.CodeBlock && m.range.first in it.range }
        if (!insideCode) hits += Hit(m.range, MarkdownSegment.InlineSvg(m.value))
    }

    if (hits.isEmpty()) return listOf(MarkdownSegment.Markdown(text))

    val sorted = hits.sortedBy { it.range.first }
    val out = mutableListOf<MarkdownSegment>()
    var cursor = 0
    for (h in sorted) {
        if (h.range.first > cursor) {
            val before = text.substring(cursor, h.range.first)
            if (before.isNotBlank()) out.add(MarkdownSegment.Markdown(before))
        }
        out.add(h.segment)
        cursor = h.range.last + 1
    }
    if (cursor < text.length) {
        val tail = text.substring(cursor)
        if (tail.isNotBlank()) out.add(MarkdownSegment.Markdown(tail))
    }
    return out
}
