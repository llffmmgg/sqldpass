import Foundation

/// 자격증별 시험 일정 — 웹 `frontend/src/lib/examSchedules.ts` 미러.
/// 회차 라벨 + 시험 시작일(+ 기간 시험의 경우 종료일) 구조.
/// 데이터는 매년 공식 사이트(dataq.or.kr / 큐넷 / 대한상공회의소) 기준으로 갱신해야 한다.
struct ExamSchedule: Hashable {
    let label: String
    /// 시험 시작일 또는 단일 시험일.
    let startDate: Date
    /// 기간 시험(정처기 CBT 등) 종료일. nil 이면 단일일 시험.
    let endDate: Date?
}

enum ExamSchedules {
    /// slug → 시험 일정 배열. 컴활 1·2급은 상시 시험이라 비어 있음.
    static let bySlug: [String: [ExamSchedule]] = [
        "sqld": [
            schedule("제60회", "2026-03-07"),
            schedule("제61회", "2026-05-31"),
            schedule("제62회", "2026-08-22"),
            schedule("제63회", "2026-11-14"),
        ],
        "engineer-written": [
            schedule("2026년 1회", "2026-01-30", "2026-03-03"),
            schedule("2026년 2회", "2026-05-09", "2026-05-29"),
            schedule("2026년 3회", "2026-08-07", "2026-09-01"),
        ],
        "engineer": [
            schedule("2026년 1회", "2026-04-18", "2026-05-06"),
            schedule("2026년 2회", "2026-07-18", "2026-08-05"),
            schedule("2026년 3회", "2026-10-24", "2026-11-13"),
        ],
        "adsp": [
            schedule("제48회", "2026-02-07"),
            schedule("제49회", "2026-05-17"),
            schedule("제50회", "2026-08-08"),
            schedule("제51회", "2026-10-31"),
        ],
        "computer-literacy-1": [],
        "computer-literacy-2": [],
    ]

    /// 컴활처럼 시험 일정이 없는 상시 시험인지.
    static func isAlwaysOpen(slug: String) -> Bool {
        slug == "computer-literacy-1" || slug == "computer-literacy-2"
    }

    /// 오늘(KST) 기준 진행 중이거나 다가올 가장 가까운 회차. 모두 지났으면 nil.
    /// 기간 시험은 `endDate` 가 지나기 전까지 해당 회차를 반환한다.
    static func upcoming(slug: String, now: Date = Date()) -> ExamSchedule? {
        guard let list = bySlug[slug], !list.isEmpty else { return nil }
        let cal = Calendar(identifier: .gregorian)
        let today = cal.startOfDay(for: now)
        return list.first { sched in
            let end = sched.endDate ?? sched.startDate
            // 종료일(또는 단일일)의 23:59:59 까지는 진행 중으로 본다.
            return cal.startOfDay(for: end) >= today
        }
    }

    /// 다음 회차까지 D-day 문자열. 상시 시험/회차 미정이면 nil.
    /// 기간 시험은 시작일 지난 뒤 종료일 전까지 "진행 중" 으로 표기.
    static func upcomingDDay(slug: String, now: Date = Date()) -> String? {
        if isAlwaysOpen(slug: slug) { return nil }
        guard let s = upcoming(slug: slug, now: now) else { return nil }
        let cal = Calendar(identifier: .gregorian)
        let today = cal.startOfDay(for: now)
        let start = cal.startOfDay(for: s.startDate)
        let days = cal.dateComponents([.day], from: today, to: start).day ?? 0
        if days < 0 { return "진행 중" }
        if days == 0 { return "D-DAY" }
        return "D-\(days)"
    }

    /// "M.d" 한국식 단축 날짜 (KST). 예: "3.7"
    static func shortDateLabel(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        f.dateFormat = "M.d"
        return f.string(from: date)
    }

    /// 카드에 표시할 1줄 일정 문구.
    ///  - 상시 시험 → "상시 시험"
    ///  - 기간 시험 → "{label} · {시작 M.d}~{종료 M.d}"
    ///  - 단일 시험 → "{label} · {M.d}"
    ///  - 일정 미정 → "다음 회차 미정"
    static func upcomingLabel(slug: String, now: Date = Date()) -> String {
        if isAlwaysOpen(slug: slug) { return "상시 시험" }
        guard let s = upcoming(slug: slug, now: now) else { return "다음 회차 미정" }
        if let end = s.endDate {
            return "\(s.label) · \(shortDateLabel(s.startDate))~\(shortDateLabel(end))"
        } else {
            return "\(s.label) · \(shortDateLabel(s.startDate))"
        }
    }

    // MARK: - Helpers

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static func schedule(_ label: String, _ start: String, _ end: String? = nil) -> ExamSchedule {
        ExamSchedule(
            label: label,
            startDate: dateFormatter.date(from: start) ?? Date(),
            endDate: end.flatMap { dateFormatter.date(from: $0) }
        )
    }
}
