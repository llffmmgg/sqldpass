import SwiftUI

// MARK: - Public API

/// 6종 자격증 식별자. 모바일 자격증 모델: '내 자격증' 영구 설정 없이 자유 전환.
enum AppCert: String, CaseIterable {
    case sqld              = "SQLD"
    case engineerPractical = "EngineerPractical"
    case engineerWritten   = "EngineerWritten"
    case cl1               = "Cl1"
    case cl2               = "Cl2"
    case adsp              = "Adsp"

    /// 한글 표기.
    var label: String {
        switch self {
        case .sqld:              return "SQLD"
        case .engineerPractical: return "정처기 실기"
        case .engineerWritten:   return "정처기 필기"
        case .cl1:               return "컴활 1급"
        case .cl2:               return "컴활 2급"
        case .adsp:              return "ADsP"
        }
    }

    /// 자격증 액센트 컬러.
    var color: Color {
        switch self {
        case .sqld:              return .certSQLD
        case .engineerPractical: return .certEngineerPractical
        case .engineerWritten:   return .certEngineerWritten
        case .cl1:               return .certComputerL1
        case .cl2:               return .certComputerL2
        case .adsp:              return .certADSP
        }
    }
}

/// 자격증 뱃지의 크기 단계. 헤더 / 카드 / 칩 등 다양한 컨텍스트 대응.
enum AppCertBadgeSize {
    case small    // 24pt
    case medium   // 32pt
    case large    // 44pt

    var height: CGFloat {
        switch self {
        case .small:  return 24
        case .medium: return 32
        case .large:  return 44
        }
    }

    var font: Font {
        switch self {
        case .small:  return AppType.callout.weight(.semibold)
        case .medium: return AppType.bodyEmph
        case .large:  return AppType.subheading
        }
    }

    var horizontalPadding: CGFloat {
        switch self {
        case .small:  return Spacing.sm
        case .medium: return Spacing.md
        case .large:  return Spacing.base
        }
    }
}

/// 자격증을 색으로 즉시 인지시키는 알약 뱃지.
struct AppCertBadge: View {
    let cert: AppCert
    var size: AppCertBadgeSize = .medium

    var body: some View {
        Text(cert.label)
            .font(size.font)
            .foregroundStyle(cert.color)
            .padding(.horizontal, size.horizontalPadding)
            .frame(minHeight: size.height)
            .background(cert.color.opacity(0.14))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.full, style: .continuous)
                    .stroke(cert.color, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
    }
}

// MARK: - Preview

#Preview("AppCertBadge — 6 × 3 grid") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            ForEach([AppCertBadgeSize.small,
                     .medium,
                     .large], id: \.self) { size in
                VStack(alignment: .leading, spacing: Spacing.sm) {
                    Text(label(for: size))
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.appTextMuted)

                    HStack(spacing: Spacing.sm) {
                        ForEach(AppCert.allCases, id: \.rawValue) { cert in
                            AppCertBadge(cert: cert, size: size)
                        }
                    }
                }
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}

extension AppCertBadgeSize: Hashable {}

private func label(for size: AppCertBadgeSize) -> String {
    switch size {
    case .small:  return "SMALL (24pt)"
    case .medium: return "MEDIUM (32pt)"
    case .large:  return "LARGE (44pt)"
    }
}
