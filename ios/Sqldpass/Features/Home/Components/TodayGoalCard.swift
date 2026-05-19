import SwiftUI

/// 홈 "오늘 목표" 카드 — Streak 카드와 2-column grid 의 우측에 위치.
///
/// 현재는 백엔드에 today_solved 데이터가 없어 placeholder 로 표시한다.
/// `AppProgressRing` 가운데 라벨에 "—" 를 넣고 caption 으로 안내 문구를 노출.
///
/// 단일 진실 원천: design_handoff_mobile_screens/ios/project/screen-home.jsx 의
/// "오늘 목표" 카드. 다만 데이터 미통합 상태에서는 비활성 톤으로 보여준다.
struct TodayGoalCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                Text("오늘 목표")
                    .font(AppType.caption.weight(.semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color.appTextMuted)
                Spacer(minLength: 0)
                Image(systemName: "target")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            }

            HStack(spacing: Spacing.md) {
                AppProgressRing(
                    value: 0,
                    total: 10,
                    color: .brandPrimary,
                    size: 44,
                    stroke: 4,
                    label: "—"
                )
                Text("오늘 목표 기능은 곧 제공돼요")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: Radius.lg)
                .fill(Color.appSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
    }
}

#Preview {
    TodayGoalCard()
        .padding()
        .background(Color.appPage)
}
