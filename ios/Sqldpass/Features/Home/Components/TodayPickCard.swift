import SwiftUI

/// 홈 "오늘의 추천" placeholder 카드.
///
/// 학습 데이터가 충분히 쌓이지 않아 맞춤 미션을 산정하지 못하는 상태를 명확히 안내한다.
/// 좌측 36pt 마스코트(`AppMascot(.guide)`) + 본문 + 우측 화살표.
struct TodayPickCard: View {
    var body: some View {
        HStack(alignment: .center, spacing: Spacing.md) {
            AppMascot(pose: .guide, sizeDp: 36, animateOnAppear: false)

            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text("오늘의 미션")
                    .font(AppType.caption.weight(.semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color.brandPrimary)
                Text("추천 미션은 곧 제공돼요")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(1)
                Text("학습 데이터가 쌓이면 맞춤 미션이 표시돼요")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: Spacing.sm)

            Image(systemName: "arrow.right")
                .font(AppType.callout)
                .foregroundStyle(Color.appTextSubtle)
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
    TodayPickCard()
        .padding()
        .background(Color.appPage)
}
