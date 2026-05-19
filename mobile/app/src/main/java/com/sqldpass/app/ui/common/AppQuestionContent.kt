package com.sqldpass.app.ui.common

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sqldpass.app.text.MarkdownSegment
import com.sqldpass.app.text.SqldpassMarkwon
import com.sqldpass.app.text.ensureCodeFences
import com.sqldpass.app.text.splitMarkdownSegments
import com.sqldpass.app.ui.runner.CodeBlockCard
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing
import com.sqldpass.app.ui.theme.SqldpassTheme

/**
 * 코드블록을 어느 chrome 으로 감쌀지 선택.
 *
 *  - [Card]: Runner/Solo 본문/해설 — `ui/runner/CodeBlockCard` (AppCard + 언어 라벨 + 가로 스크롤)
 *  - [Bare]: 카드 안의 카드처럼 중첩이 부담스러운 자리(예: 옵션 행) — mono Text + 가로 스크롤만
 */
enum class AppCodeBlockSurface { Card, Bare }

/**
 * sqldpass Android 의 단일 질문 본문/해설 렌더러.
 *
 * 흐름:
 *  1. [ensureCodeFences] — HTML `<pre><code>` 를 fenced markdown 으로, `<table>` 을 GFM 표로 정규화
 *  2. [splitMarkdownSegments] — Markdown / CodeBlock / InlineSvg / Image 세그먼트로 분할
 *  3. 세그먼트마다 알맞은 위젯으로 분기 (markdown 은 Markwon + AndroidView TextView)
 *
 * Runner 본문, Solo 본문/해설, 결과 화면 해설이 동일 렌더러를 공유 — markdown/code/표/SVG 일관 처리.
 *
 * spec: phases/android-polish-and-shared-renderer/step1.md
 */
@Composable
fun AppQuestionContent(
    text: String,
    modifier: Modifier = Modifier,
    textSizeSp: Float = 16f,
    codeBlockSurface: AppCodeBlockSurface = AppCodeBlockSurface.Card,
) {
    val segments = remember(text) {
        splitMarkdownSegments(ensureCodeFences(text))
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
    ) {
        segments.forEach { seg ->
            when (seg) {
                is MarkdownSegment.Markdown ->
                    AppMarkwonTextView(text = seg.text, textSizeSp = textSizeSp)
                is MarkdownSegment.Table ->
                    QuestionTable(rows = seg.rows)
                is MarkdownSegment.CodeBlock -> when (codeBlockSurface) {
                    AppCodeBlockSurface.Card ->
                        CodeBlockCard(language = seg.language, code = seg.code)
                    AppCodeBlockSurface.Bare ->
                        BareCodeBlock(language = seg.language, code = seg.code)
                }
                is MarkdownSegment.InlineSvg ->
                    InlineSvgView(svgXml = seg.svgXml)
                is MarkdownSegment.Image ->
                    RemoteImageView(src = seg.src, alt = seg.alt)
            }
        }
    }
}

@Composable
private fun QuestionTable(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    val palette = LocalSqldpassPalette.current
    val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
    val normalizedRows = rows.map { row ->
        if (row.size == columnCount) row else row + List(columnCount - row.size) { "" }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(SqldRadius.md))
                .border(
                    BorderStroke(1.dp, palette.border),
                    RoundedCornerShape(SqldRadius.md),
                ),
        ) {
            normalizedRows.forEachIndexed { rowIndex, row ->
                Row {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 104.dp, max = 188.dp)
                                .background(if (rowIndex == 0) palette.elevated else palette.card)
                                .border(BorderStroke(1.dp, palette.border))
                                .padding(horizontal = SqldSpacing.sm, vertical = SqldSpacing.sm),
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = palette.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Markdown 세그먼트 전용 — TextView 에 Markwon spannable 을 세팅.
 *
 * Markwon 인스턴스는 [SqldpassMarkwon.get] 싱글톤 재사용. 본문 색은 팔레트 [LocalSqldpassPalette]
 * 의 `textPrimary` 만 따른다 (Material3 colorScheme 직접 참조 없음 — 디자인 격리).
 */
@Composable
private fun AppMarkwonTextView(text: String, textSizeSp: Float) {
    val ctx = LocalContext.current
    val palette = LocalSqldpassPalette.current
    val textColor = palette.textPrimary.toArgb()
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
            view.setTextColor(textColor)
            view.textSize = textSizeSp
            markwon.setParsedMarkdown(view, spanned)
        },
    )
}

/**
 * Bare 모드 코드블록 — AppCard 중첩 없이 mono Text + 가로 스크롤만.
 * 들여쓰기 보존을 위해 softWrap=false 와 horizontalScroll 유지.
 */
@Composable
private fun BareCodeBlock(language: String?, code: String) {
    val palette = LocalSqldpassPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = (if (!language.isNullOrBlank()) "[$language]\n" else "") + code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = palette.textPrimary,
            softWrap = false,
        )
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

private const val SAMPLE_MIXED = """
다음 SQL 문이 반환하는 결과를 고르시오.

```sql
SELECT name, COUNT(*) AS cnt
FROM employees
WHERE dept_id = 10
GROUP BY name
HAVING COUNT(*) > 1;
```

표는 다음과 같다.

| 컬럼 | 타입 |
| --- | --- |
| name | VARCHAR |
| cnt | NUMBER |

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 40">
<rect x="0" y="0" width="100" height="40" fill="#e4e4e7"/>
<text x="50" y="25" font-size="14" text-anchor="middle">DIAGRAM</text>
</svg>
"""

@Preview(name = "AppQuestionContent — Light", showBackground = true)
@Composable
private fun AppQuestionContentPreviewLight() {
    SqldpassTheme(darkTheme = false) {
        AppQuestionContent(
            text = SAMPLE_MIXED.trim(),
            codeBlockSurface = AppCodeBlockSurface.Card,
        )
    }
}

@Preview(name = "AppQuestionContent — Dark", showBackground = true)
@Composable
private fun AppQuestionContentPreviewDark() {
    SqldpassTheme(darkTheme = true) {
        AppQuestionContent(
            text = SAMPLE_MIXED.trim(),
            codeBlockSurface = AppCodeBlockSurface.Card,
        )
    }
}

@Preview(name = "AppQuestionContent — Bare code", showBackground = true)
@Composable
private fun AppQuestionContentPreviewBare() {
    SqldpassTheme(darkTheme = false) {
        AppQuestionContent(
            text = SAMPLE_MIXED.trim(),
            codeBlockSurface = AppCodeBlockSurface.Bare,
        )
    }
}
