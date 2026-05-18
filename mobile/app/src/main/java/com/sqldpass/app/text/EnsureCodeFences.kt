package com.sqldpass.app.text

/**
 * 백엔드 응답이 HTML `<pre><code class="language-sql">...</code></pre>` 형태로 올 때
 * fenced markdown(```sql\n...\n```) 으로 정규화. splitMarkdownSegments 호출 전에 한 번.
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

    return text
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
