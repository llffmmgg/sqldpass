import Foundation

/// 내정보 상단 KPI 2x2 그리드용 값 모음.
///
/// 단일 진실 원천: `docs/MOBILE_UX_SPEC.md` § 2.5 / § 6.
///
/// 본 phase(`mobile-ux-restructure` step 7)에서는 `longestStreak` 만
/// `/api/streak/me` 의 실데이터로 채우고, 나머지 세 값은 nil 로 둔다.
/// `nil` 인 경우 UI 는 "—" 로 표시한다.
///
/// 누적 풀이 수·평균 정답률·합격 확률을 백엔드 신설로 채우는 작업은
/// 별 phase `kpi-backend-support` 의 책임이다.
struct ProfileKpi: Equatable {
    /// 총 풀이 문제 수 (누적). 단위: 문제.
    let totalSolved: Int?

    /// 전체 기간 평균 정답률 (0~100, 정수 백분율).
    let avgCorrectRate: Int?

    /// 최장 연속 학습 일수. 단위: 일.
    let longestStreak: Int?

    /// 합격 확률 (0~100, 정수 백분율). 현재 미통합 — 향후 백엔드 신설 예정.
    let passProbability: Int?

    static let empty = ProfileKpi(
        totalSolved: nil,
        avgCorrectRate: nil,
        longestStreak: nil,
        passProbability: nil
    )
}
