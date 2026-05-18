package com.sqldpass.app.text

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Markwon 인스턴스 — 풀이 본문/옵션·해설 등 마크다운/HTML 텍스트를 Spannable 로 변환.
 *
 * 포함 플러그인:
 *  - core: 기본 markdown (heading/list/code/blockquote/emphasis)
 *  - html: 백엔드 응답이 HTML 일 경우 처리 (<pre>, <code>, <br>, <table> 등)
 *  - linkify: 자동 URL 링크
 *  - tables: GFM table 렌더링
 *
 * WebView 도입 아님 — Markwon 은 TextView 의 Spannable 만 다룸.
 */
object SqldpassMarkwon {
    @Volatile
    private var instance: Markwon? = null

    fun get(context: Context): Markwon {
        val cached = instance
        if (cached != null) return cached
        return synchronized(this) {
            val existing = instance
            if (existing != null) existing
            else {
                val built = Markwon.builder(context.applicationContext)
                    .usePlugin(HtmlPlugin.create())
                    .usePlugin(LinkifyPlugin.create())
                    .usePlugin(TablePlugin.create(context.applicationContext))
                    .build()
                instance = built
                built
            }
        }
    }
}
