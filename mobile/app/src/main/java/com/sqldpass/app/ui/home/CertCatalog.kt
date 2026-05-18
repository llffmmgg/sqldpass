package com.sqldpass.app.ui.home

import androidx.compose.ui.graphics.Color
import com.sqldpass.app.ui.theme.CertColors

/**
 * 자격증 6종 정적 정보 — 홈 자격증 캐러셀 + 자격증 소개 모달 시트가 사용.
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 5.
 * iOS 미러: ios/Sqldpass/Core/Catalog/CertCatalog.swift.
 *
 * `slug` 는 백엔드 cert slug (api/public/past-exams?cert=…) 와 동일.
 */
data class CertInfo(
    val slug: String,
    val label: String,
    val shortDesc: String,
    val issuer: String,
    val questionCount: Int,
    val durationLabel: String,
    val passCriteria: String,
)

val CERT_CATALOG: List<CertInfo> = listOf(
    CertInfo(
        slug = "sqld",
        label = "SQLD",
        shortDesc = "데이터 활용 SQL 개발자",
        issuer = "한국데이터산업진흥원",
        questionCount = 50,
        durationLabel = "90분",
        passCriteria = "60점 이상 (과목별 40% 이상)",
    ),
    CertInfo(
        slug = "engineer-written",
        label = "정처기 필기",
        shortDesc = "정보처리기사 필기",
        issuer = "한국산업인력공단",
        questionCount = 100,
        durationLabel = "150분",
        passCriteria = "평균 60점, 과목별 40점 이상",
    ),
    CertInfo(
        slug = "engineer",
        label = "정처기 실기",
        shortDesc = "정보처리기사 실기",
        issuer = "한국산업인력공단",
        questionCount = 20,
        durationLabel = "150분",
        passCriteria = "60점 이상",
    ),
    CertInfo(
        slug = "computer-literacy-1",
        label = "컴활 1급",
        shortDesc = "컴퓨터활용능력 1급 필기",
        issuer = "대한상공회의소",
        questionCount = 60,
        durationLabel = "60분",
        passCriteria = "평균 60점, 과목별 40점 이상",
    ),
    CertInfo(
        slug = "computer-literacy-2",
        label = "컴활 2급",
        shortDesc = "컴퓨터활용능력 2급 필기",
        issuer = "대한상공회의소",
        questionCount = 40,
        durationLabel = "40분",
        passCriteria = "평균 60점, 과목별 40점 이상",
    ),
    CertInfo(
        slug = "adsp",
        label = "ADsP",
        shortDesc = "데이터분석 준전문가",
        issuer = "한국데이터산업진흥원",
        questionCount = 50,
        durationLabel = "90분",
        passCriteria = "60점 이상 (과목별 40% 이상)",
    ),
)

fun certColorOf(slug: String, cert: CertColors): Color = when (slug) {
    "sqld" -> cert.sqld
    "engineer" -> cert.engineerPractical
    "engineer-written" -> cert.engineerWritten
    "computer-literacy-1" -> cert.cl1
    "computer-literacy-2" -> cert.cl2
    "adsp" -> cert.adsp
    else -> cert.sqld
}
