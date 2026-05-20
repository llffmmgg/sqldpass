import SwiftUI

/// 홈 자격증 캐러셀 — 옆으로 스왑되는 시험 정보 카드.
/// 진척도/학습 상태 없이 시험 정보(다음 일정 + D-day / 문항·시간 / 합격기준)만 노출.
struct HomeCertCarousel: View {
    var body: some View {
        ScrollView(.horizontal) {
            LazyHStack(alignment: .top, spacing: Spacing.sm) {
                ForEach(CertCatalog.all) { cert in
                    HomeCertCard(cert: cert)
                        .containerRelativeFrame(
                            .horizontal,
                            count: 10,
                            span: 8,
                            spacing: Spacing.sm
                        )
                }
            }
            .scrollTargetLayout()
        }
        .contentMargins(.horizontal, Spacing.base, for: .scrollContent)
        .scrollTargetBehavior(.viewAligned)
        .scrollIndicators(.hidden)
    }
}

/// 단일 자격증 카드 — 자격증명 + D-day 배지 / 부제 / 일정·문항·합격기준 컴팩트 행.
/// iOS 네이티브 톤: 좌측 stripe 제거, 컴팩트 padding, 액센트는 D-day capsule 에만.
private struct HomeCertCard: View {
    let cert: CertInfo

    private var accent: Color { certColorOf(slug: cert.slug) }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            titleRow
            Text(cert.shortDesc)
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
                .lineLimit(1)

            infoRow(icon: "calendar", text: ExamSchedules.upcomingLabel(slug: cert.slug))
            infoRow(icon: "list.number", text: "\(cert.questionCount)문항 · \(cert.durationLabel)")
            infoRow(icon: "checkmark.seal", text: cert.passCriteria)
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
        // 카드 컨텐츠 차이로 LazyHStack max child height 가 잉여 vertical 영역을 만들지 않도록 고정.
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .frame(height: 150)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .stroke(Color.appTextMuted.opacity(0.25), lineWidth: 0.5)
        )
    }

    private var titleRow: some View {
        HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
            Text(cert.label)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer(minLength: Spacing.xs)
            if let dday = ExamSchedules.upcomingDDay(slug: cert.slug) {
                Text(dday)
                    .font(AppType.caption.weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(accent.opacity(0.15)))
            } else if ExamSchedules.isAlwaysOpen(slug: cert.slug) {
                Text("상시")
                    .font(AppType.caption.weight(.bold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(accent.opacity(0.15)))
            }
        }
    }

    private func infoRow(icon: String, text: String) -> some View {
        HStack(alignment: .center, spacing: Spacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Color.appTextSubtle)
                .frame(width: 14, alignment: .center)
            Text(text)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(1)
                .truncationMode(.tail)
                .minimumScaleFactor(0.85)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: Spacing.md) {
        AppSectionHeader(title: "자격증 둘러보기")
            .padding(.horizontal, Spacing.base)
        HomeCertCarousel()
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.vertical, Spacing.lg)
    .background(Color.appPage)
}
