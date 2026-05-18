package com.sqldpass.app.text

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 백엔드는 LocalDateTime/LocalDate 를 TZ 정보 없이 ISO 형식으로 직렬화 (KST naive).
 * 예: "2026-05-18T16:25:35" 또는 "2026-05-18".
 * 모바일에서 사람 가독 라벨로 변환할 때 사용.
 */

private val DATE_LABEL = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val DATETIME_LABEL = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

fun formatKstDateTime(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(raw).format(DATETIME_LABEL)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw).format(DATE_LABEL)
        } catch (_: DateTimeParseException) {
            raw
        }
    }
}

fun formatKstDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw).format(DATE_LABEL)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(raw).toLocalDate().format(DATE_LABEL)
        } catch (_: DateTimeParseException) {
            raw
        }
    }
}
