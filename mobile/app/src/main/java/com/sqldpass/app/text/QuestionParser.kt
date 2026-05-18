package com.sqldpass.app.text

/**
 * frontend/src/lib/parseQuestion.ts 1:1 포팅.
 * ①②③④ 마커를 기준으로 본문(body) / 보기(options) 분리.
 *
 * 백엔드 응답은 question.content 한 덩어리 — 프론트 / 모바일 모두 클라이언트
 * 측에서 split. 동등 처리.
 *
 * 규칙:
 *  - 옵션 마커로 시작하는 줄 → options[] (마커 + 공백 제거)
 *  - 마커 이전 줄 → body (마크다운/HTML 그대로)
 *  - 옵션 수집 시작 후 마커 없는 줄 → 직전 옵션에 이어붙임 (들여쓰기 보존)
 *    정처기 필기 같이 보기 안에 코드 조각 있는 케이스
 */
data class ParsedQuestion(
    val body: String,
    val options: List<String>,
)

private val OPTION_MARKERS = listOf("①", "②", "③", "④")
private val LEADING_MARKER_REGEX = Regex("^[①②③④]\\s*")

fun parseQuestion(content: String): ParsedQuestion {
    val lines = content.split("\n")
    val bodyLines = mutableListOf<String>()
    val options = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        if (OPTION_MARKERS.any { trimmed.startsWith(it) }) {
            options.add(LEADING_MARKER_REGEX.replace(trimmed, ""))
        } else if (options.isEmpty()) {
            bodyLines.add(line)
        } else {
            // 다중 라인 옵션: 들여쓰기·줄바꿈 보존하고 직전 옵션에 이어붙임
            val last = options.removeAt(options.lastIndex)
            options.add(last + "\n" + line)
        }
    }

    return ParsedQuestion(body = bodyLines.joinToString("\n"), options = options)
}
