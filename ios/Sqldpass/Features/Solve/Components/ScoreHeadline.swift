import SwiftUI

/// 모의고사 채점 결과 헤드라인 — PASS/FAIL 배너 (Inked OMR 디자인 시스템).
///
/// - 60점 이상이면 합격, 미만이면 불합격으로 표시한다.
/// - 좌측 마스코트 + 우측 합/불 뱃지 + 큰 점수.
struct ScoreHeadline: View {
    let score: Int
    let correctCount: Int
    let totalCount: Int
    let milestoneReached: Int?
    let currentStreak: Int?

    /// 60점 이상이면 합격 (sqldpass 기본 합격선).
    private var isPass: Bool { score >= 60 }

    private var passBadge: (label: String, tone: AppBadgeTone) {
        isPass ? ("합격", .success) : ("불합격", .danger)
    }

    private var accentColor: Color {
        isPass ? .semanticSuccess : .semanticDanger
    }

    var body: some View {
        AppCard(
            surface: .card,
            accent: isPass ? .success : .danger
        ) {
            HStack(alignment: .top, spacing: Spacing.md) {
                AppMascot(
                    pose: isPass ? .celebrate : .review,
                    sizeDp: 88
                )

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    AppBadge(
                        label: passBadge.label,
                        tone: passBadge.tone,
                        variant: .solid
                    )

                    HStack(alignment: .lastTextBaseline, spacing: Spacing.xxs) {
                        Text("\(score)")
                            .font(.system(size: 32, weight: .bold, design: .monospaced))
                            .foregroundStyle(accentColor)
                            // 0 → score 자연스러운 카운트 transition. iOS 17+ 의 numericText.
                            .contentTransition(.numericText(value: Double(score)))
                            .animation(.spring(response: 0.6, dampingFraction: 0.7), value: score)
                        Text("점")
                            .font(AppType.title)
                            .foregroundStyle(Color.appTextMuted)
                    }

                    Text("\(correctCount) / \(totalCount) 정답")
                        .font(AppType.body)
                        .foregroundStyle(Color.appTextMuted)

                    if let milestone = milestoneReached {
                        Label("\(milestone)일 연속 풀이 달성!", systemImage: "flame.fill")
                            .font(AppType.footnote.weight(.semibold))
                            .foregroundStyle(Color.semanticWarning)
                    } else if let streak = currentStreak, streak > 0 {
                        Label("연속 \(streak)일", systemImage: "flame")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}
