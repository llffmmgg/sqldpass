import SwiftUI

/// 과목별 정답률 1 줄 (Inked OMR 디자인 시스템).
///
/// 한 줄에 과목명 + 정답수/전체 + 진행률 바.
/// 카드 chrome 은 호출 측(SolveResultView) 의 `AppCard` 가 묶어서 담당한다.
struct SubjectBreakdownCard: View {
    let subjectName: String
    let correct: Int
    let total: Int

    private var rate: Double {
        guard total > 0 else { return 0 }
        return Double(correct) / Double(total)
    }

    private var rateColor: Color {
        if rate >= 0.8 { return Color.semanticSuccess }
        if rate >= 0.6 { return Color.brandPrimary }
        return Color.semanticDanger
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(spacing: Spacing.sm) {
                Text(subjectName)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer(minLength: Spacing.sm)
                Text("\(correct) / \(total)")
                    .font(AppType.monoNumeric.weight(.semibold))
                    .foregroundStyle(rateColor)
            }
            ProgressView(value: rate)
                .tint(rateColor)
        }
    }
}
