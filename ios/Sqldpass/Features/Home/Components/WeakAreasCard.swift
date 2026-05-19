import SwiftUI

/// 홈 "약점 보강" 카드 — wrongRate 내림차순 상위 3개 row.
///
/// `AppListGroupCard` 안에 `WrongAnswerStats` 를 row 로 나열한다.
/// 좌측 36pt 사각형에 `wrongRate%` 를 표시하고, 색은 다음 분기를 따른다.
///  - 50% 이상: `Color.semanticDanger`
///  - 25% 이상: `Color.semanticWarning`
///  - 그 외:    `Color.brandPrimary`
///
/// stats 가 비어 있으면 caller(HomeView) 가 섹션 자체를 숨겨야 한다.
struct WeakAreasCard: View {
    let stats: [WrongAnswerStats]
    var onRowTap: (WrongAnswerStats) -> Void = { _ in }

    private var topStats: [WrongAnswerStats] {
        stats
            .sorted { $0.wrongRate > $1.wrongRate }
            .prefix(3)
            .map { $0 }
    }

    var body: some View {
        AppListGroupCard {
            ForEach(Array(topStats.enumerated()), id: \.element.id) { index, stat in
                if index > 0 {
                    AppListGroupDivider()
                }
                WeakAreaRow(stat: stat, onTap: { onRowTap(stat) })
            }
        }
    }
}

private struct WeakAreaRow: View {
    let stat: WrongAnswerStats
    let onTap: () -> Void

    private var accent: Color {
        if stat.wrongRate >= 50 {
            return .semanticDanger
        }
        if stat.wrongRate >= 25 {
            return .semanticWarning
        }
        return .brandPrimary
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: Spacing.md) {
                // 좌측 36pt rounded square (wrongRate%).
                ZStack {
                    RoundedRectangle(cornerRadius: Radius.md)
                        .fill(accent.opacity(0.14))
                    Text("\(stat.wrongRate)%")
                        .font(AppType.footnote.weight(.bold))
                        .foregroundStyle(accent)
                        .minimumScaleFactor(0.7)
                        .lineLimit(1)
                }
                .frame(width: 36, height: 36)

                // 중앙 2-line: subject + sub caption.
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(stat.subjectName)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                        .lineLimit(1)
                    Text("\(stat.subjectName) · 오답 \(stat.wrongCount)/\(stat.totalSolved)문제")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                        .lineLimit(1)
                }

                Spacer(minLength: Spacing.sm)

                Image(systemName: "chevron.right")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            }
            .padding(.horizontal, Spacing.base)
            .padding(.vertical, Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    WeakAreasCard(
        stats: [
            WrongAnswerStats(subjectId: 1, subjectName: "비트 연산자", totalSolved: 12, wrongCount: 7, wrongRate: 58),
            WrongAnswerStats(subjectId: 2, subjectName: "JOIN 종류", totalSolved: 10, wrongCount: 4, wrongRate: 40),
            WrongAnswerStats(subjectId: 3, subjectName: "함수 종속", totalSolved: 20, wrongCount: 3, wrongRate: 15),
        ]
    )
    .padding()
    .background(Color.appPage)
}
