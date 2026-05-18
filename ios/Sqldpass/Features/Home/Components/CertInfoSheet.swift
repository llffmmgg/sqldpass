import SwiftUI

/// 홈 자격증 캐러셀에서 카드 탭 시 펼침 — 시험 정보 4종 + "PASS+ 소개" CTA 1개.
///
/// 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 4 의 13번 규칙
/// (시행처·문항수·시험 시간·합격 기준 + PASS+ 소개 CTA, 그 외 추가 금지).
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/CertInfoSheet.kt.
///
/// 호출 패턴: `.sheet(item: $sheetCert) { info in CertInfoSheet(info: info, ...) }`.
/// 시트는 `.presentationDetents([.medium])` 로 medium detent 만 사용.
struct CertInfoSheet: View {
    let info: CertInfo
    let onOpenPassPlus: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            header

            VStack(alignment: .leading, spacing: Spacing.sm) {
                InfoRow(label: "시행처", value: info.issuer)
                InfoRow(label: "문항수", value: "\(info.questionCount)문제")
                InfoRow(label: "시험 시간", value: info.durationLabel)
                InfoRow(label: "합격 기준", value: info.passCriteria)
            }

            Button {
                dismiss()
                onOpenPassPlus()
            } label: {
                Text("PASS+ 소개")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.brandPrimaryFG)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, Spacing.md)
                    .background(Color.brandPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.top, Spacing.lg)
        .padding(.bottom, Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appPage)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: Spacing.sm) {
            Circle()
                .fill(certColorOf(slug: info.slug))
                .frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(info.label)
                    .font(AppType.heading)
                    .foregroundStyle(Color.appTextPrimary)
                Text(info.shortDesc)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
        }
    }
}

private struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
            Spacer(minLength: Spacing.base)
            Text(value)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextPrimary)
                .multilineTextAlignment(.trailing)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    CertInfoSheet(
        info: CertCatalog.all[0],
        onOpenPassPlus: {}
    )
    .background(Color.appPage)
}
