import SwiftUI

/// 내정보 KPI 그리드의 단일 타일.
///
/// 구성(위→아래):
/// 1. SF Symbols 아이콘
/// 2. 라벨 (예: "총 풀이")
/// 3. 값 + 선택적 단위 (예: "123 문제", "62%", "—")
///
/// 값이 `nil` 인 경우 호출 측에서 "—" 를 넘긴다.
/// 디자인 토큰(`Color.*`, `AppType.*`, `Spacing.*`, `Radius.*`) 만 사용한다.
struct KpiTile: View {
    let icon: String
    let label: String
    let value: String
    let unit: String?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color.brandPrimary)
            Text(label)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(alignment: .firstTextBaseline, spacing: Spacing.xxs) {
                Text(value)
                    .font(AppType.monoNumericLarge.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                if let unit, !unit.isEmpty {
                    Text(unit)
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextSubtle)
                }
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

#Preview {
    VStack(spacing: Spacing.md) {
        HStack(spacing: Spacing.md) {
            KpiTile(icon: "checklist", label: "총 풀이", value: "—", unit: "문제")
            KpiTile(icon: "percent", label: "평균 정답률", value: "—", unit: nil)
        }
        HStack(spacing: Spacing.md) {
            KpiTile(icon: "flame.fill", label: "최장 연속", value: "12", unit: "일")
            KpiTile(icon: "chart.line.uptrend.xyaxis", label: "합격 확률", value: "—", unit: nil)
        }
    }
    .padding()
    .background(Color.appPage)
}
