import SwiftUI

/// 홈 자격증 6종 수평 캐러셀 — 카드 탭 시 `onCertTap(info)` 호출.
///
/// 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1 / § 4 의 8·13번 규칙
/// ("내 자격증" 영구 설정 없음, 자격증 소개 시트는 시험 정보 4종 + CTA 1개).
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/CertCarousel.kt.
struct CertCarousel: View {
    let onCertTap: (CertInfo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("자격증 둘러보기")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.md) {
                    ForEach(CertCatalog.all) { info in
                        CertCard(
                            info: info,
                            dotColor: certColorOf(slug: info.slug),
                            onTap: { onCertTap(info) }
                        )
                    }
                }
                .padding(.vertical, Spacing.xs)
            }
        }
    }
}

private struct CertCard: View {
    let info: CertInfo
    let dotColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                HStack(spacing: Spacing.sm) {
                    Circle()
                        .fill(dotColor)
                        .frame(width: Spacing.sm, height: Spacing.sm)
                    Text(info.label)
                        .font(AppType.subheading)
                        .foregroundStyle(Color.appTextPrimary)
                        .lineLimit(1)
                }
                Text(info.shortDesc)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                Text("\(info.questionCount)문 · \(info.durationLabel)")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
            }
            .padding(Spacing.md)
            .frame(width: 180, alignment: .leading)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(info.label) 자격증 정보 보기")
    }
}

#Preview {
    CertCarousel(onCertTap: { _ in })
        .padding()
        .background(Color.appPage)
}
