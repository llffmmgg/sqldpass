package com.sqldpass.app.text

/**
 * 마크다운 본문을 코드블록(fenced)과 일반 텍스트로 분리.
 *
 * Markwon 한 덩어리 TextView 안에서는 코드블록만 따로 Compose 컴포저블로
 * 렌더할 수 없으므로, 클라이언트 측에서 split 한 뒤 segments 를 순차 렌더.
 *
 * 비-fenced 코드(4-space 들여쓰기) 는 다루지 않음 — 우리 컨텐츠는 fenced 가정.
 */
sealed interface MarkdownSegment {
    data class Markdown(val text: String) : MarkdownSegment
    data class CodeBlock(val language: String?, val code: String) : MarkdownSegment
}

// ```lang\ncode\n``` 또는 ```code\n``` 패턴.
private val FENCED_CODE_REGEX = Regex(
    "```([a-zA-Z0-9_+\\-]*)\\s*\\n([\\s\\S]*?)```",
)

fun splitMarkdownSegments(text: String): List<MarkdownSegment> {
    val matches = FENCED_CODE_REGEX.findAll(text).toList()
    if (matches.isEmpty()) return listOf(MarkdownSegment.Markdown(text))

    val out = mutableListOf<MarkdownSegment>()
    var cursor = 0
    for (m in matches) {
        if (m.range.first > cursor) {
            val before = text.substring(cursor, m.range.first)
            if (before.isNotBlank()) out.add(MarkdownSegment.Markdown(before))
        }
        out.add(
            MarkdownSegment.CodeBlock(
                language = m.groupValues[1].takeIf { it.isNotBlank() },
                code = m.groupValues[2].trimEnd('\n', ' '),
            ),
        )
        cursor = m.range.last + 1
    }
    if (cursor < text.length) {
        val tail = text.substring(cursor)
        if (tail.isNotBlank()) out.add(MarkdownSegment.Markdown(tail))
    }
    return out
}
