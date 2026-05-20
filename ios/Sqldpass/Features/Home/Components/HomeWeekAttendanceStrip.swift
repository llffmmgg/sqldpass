import SwiftUI

/// 최근 7일 학습 출석 — GitHub commit chart 식 4단계 강도 strip.
/// 인덱스 0 = 6일 전, 인덱스 6 = 오늘. 풀이 문항 수에 따라 셀 색 농도 차등.
struct HomeWeekAttendanceStrip: View {
    /// 길이 7 배열 — `HomeViewModel.weekCounts` 가 그대로 전달된다.
    let counts: [Int]

    private static func level(for count: Int) -> Int {
        switch count {
        case ...0:      return 0
        case 1...5:     return 1
        case 6...20:    return 2
        default:        return 3
        }
    }

    private static func fill(for level: Int) -> Color {
        switch level {
        case 1:  return Color.brandPrimary.opacity(0.22)
        case 2:  return Color.brandPrimary.opacity(0.50)
        case 3:  return Color.brandPrimary
        default: return Color.appElevated
        }
    }

    private var dayLabels: [String] {
        let cal = Calendar(identifier: .gregorian)
        let today = cal.startOfDay(for: Date())
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "EEEEE"
        return (0..<7).map { offset in
            let date = cal.date(byAdding: .day, value: offset - 6, to: today) ?? today
            return formatter.string(from: date)
        }
    }

    private var activeDayCount: Int { counts.filter { $0 > 0 }.count }
    private var totalSolved: Int { counts.reduce(0, +) }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            header
            HStack(spacing: 3) {
                ForEach(0..<7, id: \.self) { i in
                    cellColumn(index: i)
                }
            }
            legend
        }
        .padding(Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
        .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 1)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 6) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.brandPrimary)
            Text("최근 7일 학습")
                .font(AppType.footnote.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            Spacer(minLength: 0)
            HStack(spacing: 4) {
                Text("\(activeDayCount)")
                    .font(AppType.footnote.weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(Color.brandPrimary)
                Text("/ 7일")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
        }
    }

    @ViewBuilder
    private func cellColumn(index i: Int) -> some View {
        let isToday = i == 6
        let count = i < counts.count ? counts[i] : 0
        let lv = Self.level(for: count)
        VStack(spacing: 4) {
            cellShape(level: lv, isToday: isToday)
            Text(i < dayLabels.count ? dayLabels[i] : "")
                .font(AppType.caption)
                .foregroundStyle(isToday ? Color.brandPrimary : Color.appTextSubtle)
                .fontWeight(isToday ? .semibold : .regular)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(count > 0 ? "\(count)문제" : "풀이 없음")
    }

    @ViewBuilder
    private func cellShape(level: Int, isToday: Bool) -> some View {
        let shape = RoundedRectangle(cornerRadius: 4, style: .continuous)
        shape
            .fill(Self.fill(for: level))
            .frame(height: 18)
            .overlay(
                shape.stroke(
                    isToday ? Color.brandPrimary : Color.clear,
                    lineWidth: 1.5
                )
            )
    }

    private var legend: some View {
        HStack(spacing: 6) {
            Text("적음")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
            ForEach(0..<4) { lv in
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(Self.fill(for: lv))
                    .frame(width: 9, height: 9)
            }
            Text("많음")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }
}

#Preview {
    VStack(spacing: Spacing.md) {
        HomeWeekAttendanceStrip(counts: [0, 3, 15, 0, 8, 25, 12])
        HomeWeekAttendanceStrip(counts: Array(repeating: 0, count: 7))
    }
    .padding()
    .background(Color.appPage)
}
