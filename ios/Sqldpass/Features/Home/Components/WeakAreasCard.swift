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
                // 좌측 ring progress — wrongRate% 만큼 trim. 12시 방향에서 시작해 시계방향.
                ZStack {
                    Circle()
                        .stroke(accent.opacity(0.18), lineWidth: 3.5)
                    Circle()
                        .trim(from: 0, to: min(max(Double(stat.wrongRate) / 100.0, 0), 1))
                        .stroke(
                            accent,
                            style: StrokeStyle(lineWidth: 3.5, lineCap: .round)
                        )
                        .rotationEffect(.degrees(-90))
                    Text("\(stat.wrongRate)")
                        .font(AppType.caption.weight(.bold))
                        .monospacedDigit()
                        .foregroundStyle(accent)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                }
                .frame(width: 40, height: 40)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("오답률 \(stat.wrongRate) 퍼센트")

                // 중앙 2-line: 짧은 과목명 + 풀이/오답 카운트.
                VStack(alignment: .leading, spacing: 2) {
                    Text(stat.subjectName)
                        .font(AppType.callout.weight(.semibold))
                        .foregroundStyle(Color.appTextPrimary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Text("오답 \(stat.wrongCount)/\(stat.totalSolved)문제")
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
