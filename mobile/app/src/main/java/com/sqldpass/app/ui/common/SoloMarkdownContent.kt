package com.sqldpass.app.ui.common

import androidx.compose.runtime.Composable

/**
 * Compatibility wrapper for older solo-solve call sites.
 *
 * All question, option, and explanation rendering is delegated to [AppQuestionContent]
 * so markdown, code, tables, SVG, and images share one implementation.
 */
@Composable
fun SoloMarkdownContent(text: String, textSizeSp: Float = 16f) {
    AppQuestionContent(
        text = text,
        textSizeSp = textSizeSp,
        codeBlockSurface = AppCodeBlockSurface.Card,
    )
}
