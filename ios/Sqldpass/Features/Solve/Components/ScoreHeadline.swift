import SwiftUI

struct ScoreHeadline: View {
    let score: Int
    let correctCount: Int
    let totalCount: Int
    let milestoneReached: Int?
    let currentStreak: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
                Text("\(score)")
                    .font(AppType.display.weight(.bold))
                    .foregroundStyle(scoreColor)
                Text("점")
                    .font(AppType.title)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
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
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var scoreColor: Color {
        if score >= 80 { return Color.brandPrimary }
        if score >= 60 { return Color.semanticInfo }
        return Color.semanticDanger
    }
}
