package com.sqldpass.app.text

/**
 * 백엔드 응답이 HTML `<pre><code class="language-sql">...</code></pre>` 형태로 올 때
 * fenced markdown(```sql\n...\n```) 으로 정규화. splitMarkdownSegments 호출 전에 한 번.
 *
 * 같은 단계에서 HTML `<table>` 도 Markwon TablePlugin 이 이해할 GFM markdown 표로 변환.
 * frontend 는 rehype-raw 로 HTML 통째로 브라우저에 흘려보내 자연스럽게 렌더하지만,
 * 모바일 Markwon HtmlPlugin 은 `<table>` tag handler 가 없어서 셀이 한 줄로 뭉친다.
 *
 * frontend QuestionContent.tsx 의 ensureCodeFences 와 동등.
 */
private val HTML_CODE_BLOCK_REGEX = Regex(
    "<pre[^>]*>\\s*<code(?:\\s+class=\"language-(\\w+)\")?[^>]*>([\\s\\S]*?)</code>\\s*</pre>",
    RegexOption.IGNORE_CASE,
)

// 인라인 <code>...</code> 도 백틱으로 정규화 (선택). 본문 외 옵션 안에서 흔함.
private val HTML_INLINE_CODE_REGEX = Regex(
    "<code[^>]*>([^<]+)</code>",
    RegexOption.IGNORE_CASE,
)

private val HTML_TABLE_REGEX = Regex(
    "<table\\b[^>]*>([\\s\\S]*?)</table>",
    RegexOption.IGNORE_CASE,
)
private val HTML_TR_REGEX = Regex(
    "<tr\\b[^>]*>([\\s\\S]*?)</tr>",
    RegexOption.IGNORE_CASE,
)
private val HTML_CELL_REGEX = Regex(
    "<(th|td)\\b[^>]*>([\\s\\S]*?)</\\1>",
    RegexOption.IGNORE_CASE,
)

fun ensureCodeFences(input: String): String {
    var text = input

    // <pre><code class="language-sql">...</code></pre> → ```sql\n...\n```
    text = HTML_CODE_BLOCK_REGEX.replace(text) { match ->
        val lang = match.groupValues[1].ifBlank { "" }
        val rawCode = match.groupValues[2]
        val decoded = decodeHtmlEntities(rawCode).trimEnd('\n', ' ')
        if (lang.isNotBlank()) "```$lang\n$decoded\n```" else "```\n$decoded\n```"
    }

    // 남은 인라인 <code>SELECT</code> → `SELECT`
    text = HTML_INLINE_CODE_REGEX.replace(text) { match ->
        val decoded = decodeHtmlEntities(match.groupValues[1])
        "`$decoded`"
    }

    // <table>...</table> → markdown table.
    text = HTML_TABLE_REGEX.replace(text) { match ->
        convertHtmlTableToMarkdown(match.groupValues[1]) ?: match.value
    }

    return text
}

/**
 * 단순한 `<tr><td>` 구조만 다룬다 — 셀 병합(`colspan`/`rowspan`), nested table 은 처리 X.
 * 변환 실패 시 null 반환 → 원본 보존(Markwon 이 그대로 렌더, 깨질 수 있음).
 */
private fun convertHtmlTableToMarkdown(innerHtml: String): String? {
    val rows: List<List<String>> = HTML_TR_REGEX.findAll(innerHtml).map { tr ->
        HTML_CELL_REGEX.findAll(tr.groupValues[1]).map { cell ->
            val raw = cell.groupValues[2]
            // 셀 내부의 줄바꿈/HTML break 는 markdown 표에서 단일 라인 필요 — 공백으로 합침.
            decodeHtmlEntities(raw)
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " / ")
                .replace(Regex("<[^>]+>"), "") // 잔여 inline 태그 제거
                .replace(Regex("[\\r\\n]+"), " ")
                .replace("|", "\\|")
                .trim()
                .ifBlank { " " }
        }.toList()
    }.toList()

    if (rows.isEmpty()) return null
    val width = rows.maxOf { it.size }
    if (width == 0) return null
    val normalized = rows.map { row ->
        if (row.size == width) row
        else row + List(width - row.size) { " " }
    }

    val sb = StringBuilder()
    sb.append("\n")
    // 헤더 row — 입력에 `<th>` 가 있던 첫 행을 헤더로, 없으면 1행을 헤더로 가정.
    val header = normalized.first()
    sb.append("| ").append(header.joinToString(" | ")).append(" |\n")
    sb.append("|").append(List(width) { "---" }.joinToString("|") { " $it " }).append("|\n")
    normalized.drop(1).forEach { row ->
        sb.append("| ").append(row.joinToString(" | ")).append(" |\n")
    }
    sb.append("\n")
    return sb.toString()
}

private fun decodeHtmlEntities(text: String): String =
    text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")  // 마지막 — 다른 엔티티의 & 와 충돌 회피
