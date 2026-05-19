import SwiftUI

/// 홈 "내 자격증" 수평 캐러셀 — 6종 자격증을 카드 형태로 노출.
///
/// 새 핸드오프(screen-home.jsx)의 cert card 구성:
///  - 상단 3pt 자격증 액센트 컬러 띠
///  - `AppCertBadge`
///  - placeholder 라벨 ("최근 풀이 없음")
///  - 4pt 빈 진행 트랙 (per-cert 진척도 데이터 미통합)
///  - caption "진척도 —"
/// 마지막 카드는 dashed border 의 "+ 추가" 카드.
///
/// 카드 탭 시 `onCertTap(info)` 호출 — 기존 HomeView 의 CertInfoSheet 흐름을 유지.
struct MyCertsCarousel: View {
    let onCertTap: (CertInfo) -> Void
    var onAddTap: () -> Void = {}

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.md) {
                ForEach(CertCatalog.all) { info in
                    MyCertCard(
                        info: info,
                        accent: certColorOf(slug: info.slug),
                        onTap: { onCertTap(info) }
                    )
                }
                AddCertCard(onTap: onAddTap)
            }
            .padding(.vertical, Spacing.xs)
            .padding(.horizontal, Spacing.base)
        }
        // 좌우 inset 을 부모 padding 과 상관없이 화면 끝까지 흐르도록.
        .padding(.horizontal, -Spacing.base)
    }
}

private struct MyCertCard: View {
    let info: CertInfo
    let accent: Color
    let onTap: () -> Void

    private var appCert: AppCert {
        switch info.slug {
        case "sqld":                return .sqld
        case "engineer":            return .engineerPractical
        case "engineer-written":    return .engineerWritten
        case "computer-literacy-1": return .cl1
        case "computer-literacy-2": return .cl2
        case "adsp":                return .adsp
        default:                    return .sqld
        }
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                // 상단 3pt 자격증 액센트 컬러 띠.
                Rectangle()
                    .fill(accent)
                    .frame(height: 3)

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    HStack(alignment: .top) {
                        AppCertBadge(cert: appCert, size: .small)
                        Spacer(minLength: 0)
                    }
                    .padding(.top, Spacing.xs)

                    Text("최근 풀이 없음")
                        .font(AppType.footnote.weight(.semibold))
                        .foregroundStyle(Color.appTextPrimary)
                        .lineLimit(1)

                    // 4pt 빈 진행 트랙 (진척도 데이터 미통합 → 비어 있음).
                    RoundedRectangle(cornerRadius: Radius.full)
                        .fill(Color.appElevated)
                        .frame(height: 4)

                    Text("진척도 —")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextSubtle)
                }
                .padding(Spacing.md)
            }
            .frame(width: 154, alignment: .leading)
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

private struct AddCertCard: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: Spacing.xs) {
                Image(systemName: "plus")
                    .font(AppType.subheading)
                    .foregroundStyle(Color.appTextSubtle)
                Text("추가")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextSubtle)
            }
            .frame(width: 72)
            .frame(maxHeight: .infinity)
            .padding(.vertical, Spacing.md)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .strokeBorder(
                        Color.appBorderStrong,
                        style: StrokeStyle(lineWidth: 1, dash: [4, 4])
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("자격증 추가")
    }
}

#Preview {
    MyCertsCarousel(onCertTap: { _ in })
        .padding(.vertical)
        .background(Color.appPage)
}
