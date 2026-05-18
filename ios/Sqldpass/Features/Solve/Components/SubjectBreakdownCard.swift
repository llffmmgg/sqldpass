import SwiftUI

struct SubjectBreakdownCard: View {
    let subjectName: String
    let correct: Int
    let total: Int

    private var rate: Double {
        guard total > 0 else { return 0 }
        return Double(correct) / Double(total)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(subjectName)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer()
                Text("\(correct) / \(total)")
                    .font(AppType.monoNumeric.weight(.semibold))
                    .foregroundStyle(rateColor)
            }
            ProgressView(value: rate)
                .tint(rateColor)
        }
        .padding(Spacing.md)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }

    private var rateColor: Color {
        if rate >= 0.8 { return Color.brandPrimary }
        if rate >= 0.6 { return Color.semanticInfo }
        return Color.semanticDanger
    }
}
