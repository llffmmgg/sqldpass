import SwiftUI

// MARK: - Public API

/// KPI 카드 안에 들어가는 숫자 크기 단계.
enum AppNumberCellSize {
    case compact
    case regular
    case display

    var valueFont: Font {
        switch self {
        case .compact: return AppType.monoNumeric.weight(.bold)
        case .regular: return AppType.monoNumericLarge.weight(.bold)
        case .display: return .system(size: 32, weight: .bold, design: .monospaced)
        }
    }

    var unitFont: Font {
        AppType.monoNumeric.weight(.regular)
    }
}

/// 대시보드/내정보 KPI 그리드에 쓰는 숫자 셀.
///
/// 예) "정답률 92 %", "남은 일수 12 일"
/// - 카드 안에서 라벨 → 숫자 → 단위 순으로 배치.
/// - 숫자는 `.contentTransition(.numericText())` 으로 부드럽게 갱신.
struct AppNumberCell: View {
    let value: String
    let label: String
    var unit: String? = nil
    var accent: Color? = nil
    var size: AppNumberCellSize = .regular

    private var valueColor: Color {
        accent ?? .brandPrimary
    }

    var body: some View {
        AppCard(surface: .card) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(label)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)

                HStack(alignment: .lastTextBaseline, spacing: Spacing.xxs) {
                    Text(value)
                        .font(size.valueFont)
                        .foregroundStyle(valueColor)
                        .contentTransition(.numericText())
                        .animation(.easeOut(duration: 0.22), value: value)
                    if let unit {
                        Text(unit)
                            .font(size.unitFont)
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

// MARK: - Preview

#Preview("AppNumberCell — 2x2 KPI grid") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("Regular (2x2)")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)

            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: Spacing.md),
                GridItem(.flexible(), spacing: Spacing.md)
            ], spacing: Spacing.md) {
                AppNumberCell(value: "92",
                              label: "정답률",
                              unit: "%",
                              accent: .semanticSuccess)
                AppNumberCell(value: "127",
                              label: "푼 문항",
                              unit: "문")
                AppNumberCell(value: "12",
                              label: "남은 일수",
                              unit: "일",
                              accent: .semanticWarning)
                AppNumberCell(value: "3",
                              label: "오답 노트",
                              unit: "건",
                              accent: .semanticDanger)
            }

            Text("Compact")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(spacing: Spacing.md) {
                AppNumberCell(value: "08:32",
                              label: "평균 풀이",
                              size: .compact)
                AppNumberCell(value: "7",
                              label: "연속 일수",
                              unit: "일",
                              size: .compact)
            }

            Text("Display")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppNumberCell(value: "92",
                          label: "오늘의 정답률",
                          unit: "%",
                          accent: .brandPrimary,
                          size: .display)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
