package com.sqldpass.app.text

/**
 * 마크다운 본문을 코드블록(fenced) / inline SVG / 외부 이미지 / 일반 텍스트로 분리.
 *
 * Markwon 한 덩어리 TextView 안에서는 코드블록·SVG·이미지를 따로 Compose 컴포저블로
 * 렌더할 수 없으므로, 클라이언트 측에서 split 한 뒤 segments 를 순차 렌더.
 *
 * - fenced 코드(```lang\n...```): 별 CodeBlockCard 로
 * - inline `<svg>...</svg>` XML: Coil + SVG 디코더로 Compose Image
 * - `<img src="...">`: Coil AsyncImage (외부 URL, data URI, png/jpg/svg 무관)
 * - 나머지: Markwon Spannable (markdown 표는 TablePlugin 이 처리)
 *
 * 비-fenced 코드(4-space 들여쓰기) 는 다루지 않음 — 우리 컨텐츠는 fenced 가정.
 */
sealed interface MarkdownSegment {
    data class Markdown(val text: String) : MarkdownSegment
    data class CodeBlock(val language: String?, val code: String) : MarkdownSegment
    data class Table(val rows: List<List<String>>) : MarkdownSegment
    data class InlineSvg(val svgXml: String) : MarkdownSegment
    data class Image(val src: String, val alt: String?) : MarkdownSegment
}

private val FENCED_CODE_REGEX = Regex(
    "```([a-zA-Z0-9_+\\-]*)\\s*\\n([\\s\\S]*?)```",
)

// 문제 본문에 박힌 inline SVG — 한 줄/여러 줄 모두 대응.
private val INLINE_SVG_REGEX = Regex(
    "<svg\\b[^>]*>[\\s\\S]*?</svg>",
    RegexOption.IGNORE_CASE,
)

// <img src="..." alt="..."/> 또는 </img> — 속성 순서/따옴표 종류 무관.
private val HTML_IMG_REGEX = Regex(
    "<img\\b([^>]*?)/?>",
    RegexOption.IGNORE_CASE,
)
private val ATTR_SRC_REGEX = Regex(
    """src\s*=\s*(?:"([^"]+)"|'([^']+)')""",
    RegexOption.IGNORE_CASE,
)
private val ATTR_ALT_REGEX = Regex(
    """alt\s*=\s*(?:"([^"]*)"|'([^']*)')""",
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
    findMarkdownTableHits(text, hits).forEach { hits += it }
    INLINE_SVG_REGEX.findAll(text).forEach { m ->
        // 코드블록 내부 svg 는 무시 — 코드 예시일 수 있음.
        val insideCode = hits.any { it.segment is MarkdownSegment.CodeBlock && m.range.first in it.range }
        if (!insideCode) hits += Hit(m.range, MarkdownSegment.InlineSvg(m.value))
    }
    HTML_IMG_REGEX.findAll(text).forEach { m ->
        val insideOther = hits.any { m.range.first in it.range }
        if (insideOther) return@forEach
        val attrs = m.groupValues[1]
        val srcMatch = ATTR_SRC_REGEX.find(attrs) ?: return@forEach
        val src = (srcMatch.groupValues[1].ifBlank { srcMatch.groupValues[2] }).trim()
        if (src.isBlank()) return@forEach
        val altMatch = ATTR_ALT_REGEX.find(attrs)
        val alt = altMatch?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }
        hits += Hit(m.range, MarkdownSegment.Image(src = src, alt = alt))
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

private data class SourceLine(
    val text: String,
    val start: Int,
    val endInclusive: Int,
)

private fun findMarkdownTableHits(text: String, existingHits: List<Hit>): List<Hit> {
    val lines = buildSourceLines(text)
    val hits = mutableListOf<Hit>()
    var i = 0
    while (i < lines.size - 1) {
        val header = lines[i]
        val delimiter = lines[i + 1]
        val insideExisting = existingHits.any { header.start in it.range || delimiter.start in it.range }
        if (!insideExisting && isPipeRow(header.text) && isTableDelimiterRow(delimiter.text)) {
            val tableLines = mutableListOf(header, delimiter)
            var j = i + 2
            while (j < lines.size && isPipeRow(lines[j].text)) {
                tableLines += lines[j]
                j += 1
            }
            val rows = buildList {
                add(splitTableCells(header.text))
                tableLines.drop(2).forEach { row ->
                    add(splitTableCells(row.text))
                }
            }.filter { it.any(String::isNotBlank) }
            if (rows.isNotEmpty()) {
                hits += Hit(
                    range = header.start..tableLines.last().endInclusive,
                    segment = MarkdownSegment.Table(rows),
                )
            }
            i = j
        } else {
            i += 1
        }
    }
    return hits
}

private fun buildSourceLines(text: String): List<SourceLine> {
    val lines = mutableListOf<SourceLine>()
    var start = 0
    while (start <= text.length) {
        val newline = text.indexOf('\n', start)
        val endExclusive = if (newline == -1) text.length else newline
        val raw = text.substring(start, endExclusive)
        lines += SourceLine(
            text = raw,
            start = start,
            endInclusive = (endExclusive - 1).coerceAtLeast(start),
        )
        if (newline == -1) break
        start = newline + 1
    }
    return lines
}

private fun isPipeRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains("|") && trimmed.count { it == '|' } >= 2
}

private fun isTableDelimiterRow(line: String): Boolean {
    if (!isPipeRow(line)) return false
    val cells = splitTableCells(line)
    return cells.isNotEmpty() && cells.all { cell ->
        cell.matches(Regex(":?-{3,}:?"))
    }
}

private fun splitTableCells(line: String): List<String> {
    val trimmed = line.trim().removePrefix("|").removeSuffix("|")
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var escaped = false
    for (ch in trimmed) {
        when {
            escaped -> {
                current.append(ch)
                escaped = false
            }
            ch == '\\' -> escaped = true
            ch == '|' -> {
                cells += current.toString().trim()
                current.clear()
            }
            else -> current.append(ch)
        }
    }
    cells += current.toString().trim()
    return cells
}
