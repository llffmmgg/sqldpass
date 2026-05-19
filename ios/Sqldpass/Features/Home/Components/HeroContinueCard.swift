import SwiftUI

/// 홈 HERO "이어서 풀기" 카드.
///
/// 두 가지 상태:
///  - **active**: 마지막 풀이가 있는 경우 → caller 가 cert/title/문항수 등을 주입 (현재 단계에선 도달 불가).
///  - **placeholder**: 마지막 풀이 데이터가 없는 기본 상태 — "모의고사 둘러보기" CTA.
///
/// 카드 chrome 은 토큰만 사용하며, glow / radial-gradient 효과는 의도적으로 제외한다
/// (사용자 톤 가이드: AI 스러운 흐릿한 효과 금지).
struct HeroContinueCard: View {
    /// true 면 active 상태 — 본 step 에서는 항상 false (placeholder).
    let isActive: Bool
    var onStartLearning: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(spacing: Spacing.xs) {
                Image(systemName: "sparkles")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.brandPrimary)
                Text("이어서 풀기")
                    .font(AppType.caption.weight(.semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color.brandPrimary)
            }

            Text("아직 진행 중인 풀이가 없어요")
                .font(AppType.subheading)
                .foregroundStyle(Color.appTextPrimary)
                .fixedSize(horizontal: false, vertical: true)

            Text("첫 세트를 시작해 보세요")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)

            Button(action: onStartLearning) {
                HStack(spacing: Spacing.xs) {
                    Text("모의고사 둘러보기")
                        .font(AppType.footnote.weight(.semibold))
                    Image(systemName: "arrow.right")
                        .font(AppType.footnote.weight(.semibold))
                }
                .padding(.horizontal, Spacing.base)
                .padding(.vertical, Spacing.sm)
                .foregroundStyle(Color.brandPrimaryFG)
                .background(Color.brandPrimary)
                .clipShape(RoundedRectangle(cornerRadius: Radius.full))
            }
            .buttonStyle(.plain)
            .padding(.top, Spacing.xs)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: Radius.lg)
                .fill(Color.appSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.brandPrimary, lineWidth: 1)
        )
    }
}

#Preview {
    HeroContinueCard(isActive: false)
        .padding()
        .background(Color.appPage)
}
