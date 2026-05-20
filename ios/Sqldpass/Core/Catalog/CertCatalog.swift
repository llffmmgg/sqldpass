import SwiftUI

/// 자격증 6종 정적 정보 — 홈 자격증 캐러셀 + 자격증 소개 모달 시트가 사용.
///
/// 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 5.
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/CertCatalog.kt.
///
/// `slug` 는 백엔드 cert slug (api/public/past-exams?cert=…) 와 동일.
struct CertInfo: Identifiable, Hashable {
    /// SwiftUI `.sheet(item:)` / `ForEach` 용. slug 와 동일.
    var id: String { slug }

    let slug: String
    let label: String
    let shortDesc: String
    let issuer: String
    let questionCount: Int
    let durationLabel: String
    let passCriteria: String
}

enum CertCatalog {
    static let all: [CertInfo] = [
        CertInfo(
            slug: "sqld",
            label: "SQLD",
            shortDesc: "데이터 활용 SQL 개발자",
            issuer: "한국데이터산업진흥원",
            questionCount: 50,
            durationLabel: "90분",
            passCriteria: "60점 이상 (과목별 40% 이상)"
        ),
        CertInfo(
            slug: "engineer-written",
            label: "정보처리기사 필기",
            shortDesc: "정보처리기사 필기",
            issuer: "한국산업인력공단",
            questionCount: 100,
            durationLabel: "150분",
            passCriteria: "평균 60점, 과목별 40점 이상"
        ),
        CertInfo(
            slug: "engineer",
            label: "정보처리기사 실기",
            shortDesc: "정보처리기사 실기",
            issuer: "한국산업인력공단",
            questionCount: 20,
            durationLabel: "150분",
            passCriteria: "60점 이상"
        ),
        CertInfo(
            slug: "computer-literacy-1",
            label: "컴퓨터활용능력 1급",
            shortDesc: "컴퓨터활용능력 1급 필기",
            issuer: "대한상공회의소",
            questionCount: 60,
            durationLabel: "60분",
            passCriteria: "평균 60점, 과목별 40점 이상"
        ),
        CertInfo(
            slug: "computer-literacy-2",
            label: "컴퓨터활용능력 2급",
            shortDesc: "컴퓨터활용능력 2급 필기",
            issuer: "대한상공회의소",
            questionCount: 40,
            durationLabel: "40분",
            passCriteria: "평균 60점, 과목별 40점 이상"
        ),
        CertInfo(
            slug: "adsp",
            label: "ADsP",
            shortDesc: "데이터분석 준전문가",
            issuer: "한국데이터산업진흥원",
            questionCount: 50,
            durationLabel: "90분",
            passCriteria: "60점 이상 (과목별 40% 이상)"
        ),
    ]
}

/// 자격증 slug → 디자인 토큰 색 매핑. 토큰 외 색은 사용하지 않는다.
func certColorOf(slug: String) -> Color {
    switch slug {
    case "sqld":                return .certSQLD
    case "engineer":            return .certEngineerPractical
    case "engineer-written":    return .certEngineerWritten
    case "computer-literacy-1": return .certComputerL1
    case "computer-literacy-2": return .certComputerL2
    case "adsp":                return .certADSP
    default:                    return .certSQLD
    }
}
