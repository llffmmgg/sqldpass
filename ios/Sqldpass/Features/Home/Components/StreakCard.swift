import SwiftUI

/// 홈 스트릭 카드 — 일반 톤(amber/sqld) vs 위험 톤(warning) 분기.
///
/// 위험 조건: `streak.currentStreak > 0` 이고 `lastSolveDate` 가 오늘이 아님
/// (KST 기준). 즉 어제까지는 풀었으나 오늘은 아직 안 푼 상태.
///
/// 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1 / § 4 의 11번 규칙(인앱 톤만).
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/HomeScreen.kt 의 StreakCard.
struct StreakCard: View {
    let streak: StreakInfo

    private var atRisk: Bool { Self.isStreakAtRisk(streak) }

    private var accent: Color {
        atRisk ? Color.semanticWarning : Color.certSQLD
    }

    private var iconName: String {
        atRisk ? "exclamationmark.triangle.fill" : "flame.fill"
    }

    private var title: String {
        if atRisk {
            return "오늘 자정이 끝나면 연속 일수가 끊겨요"
        }
        if streak.currentStreak >= 1 {
            return "연속 학습 \(streak.currentStreak)일째"
        }
        return "오늘 풀이로 streak 을 시작해요"
    }

    private var body_: String {
        if atRisk {
            return "한 문제만 풀어도 \(streak.currentStreak)일이 이어집니다."
        }
        if streak.currentStreak >= 1 {
            return "꾸준함이 합격을 만듭니다."
        }
        return "한 문제만 풀어도 streak 1일."
    }

    var body: some View {
        HStack(alignment: .center, spacing: Spacing.md) {
            Image(systemName: iconName)
                .font(.title3)
                .foregroundStyle(accent)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(title)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(body_)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: Radius.lg)
                .fill(Color.appSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(accent, lineWidth: atRisk ? 1.5 : 1)
        )
    }

    /// 위험 톤 분기 헬퍼.
    /// - `currentStreak == 0` 이면 끊길 게 없으므로 false.
    /// - `lastSolveDate` 가 nil 이거나 파싱 실패면 false (보수적).
    /// - 그 외 `lastSolveDate < 오늘(KST)` 이면 true.
    static func isStreakAtRisk(_ streak: StreakInfo) -> Bool {
        guard streak.currentStreak > 0 else { return false }
        guard let lastStr = streak.lastSolveDate else { return false }

        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "Asia/Seoul")
        formatter.dateFormat = "yyyy-MM-dd"
        guard let lastSolved = formatter.date(from: lastStr) else { return false }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        let today = calendar.startOfDay(for: Date())
        let lastDay = calendar.startOfDay(for: lastSolved)
        return lastDay < today
    }
}

#Preview("일반 톤") {
    StreakCard(
        streak: StreakInfo(
            currentStreak: 7,
            longestStreak: 12,
            lastSolveDate: nil,
            solvedToday: true
        )
    )
    .padding()
    .background(Color.appPage)
}

#Preview("위험 톤") {
    StreakCard(
        streak: StreakInfo(
            currentStreak: 3,
            longestStreak: 12,
            lastSolveDate: "2020-01-01",
            solvedToday: false
        )
    )
    .padding()
    .background(Color.appPage)
}
