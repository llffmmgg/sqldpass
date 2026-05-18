import SwiftUI

/// 컬러 톤 매핑.
enum AppBadgeTone {
    case accent
    case sqld
    case engineerPractical
    case engineerWritten
    case cl1
    case cl2
    case adsp
    case success
    case warning
    case danger
    case info
    case neutral

    var base: Color {
        switch self {
        case .accent:            return .brandPrimary
        case .sqld:              return .certSQLD
        case .engineerPractical: return .certEngineerPractical
        case .engineerWritten:   return .certEngineerWritten
        case .cl1:               return .certComputerL1
        case .cl2:               return .certComputerL2
        case .adsp:              return .certADSP
        case .success:           return .semanticSuccess
        case .warning:           return .semanticWarning
        case .danger:            return .semanticDanger
        case .info:              return .semanticInfo
        case .neutral:           return .appTextSubtle
        }
    }
}

/// soft = base 14% bg + base text. solid = base bg + white text.
enum AppBadgeVariant {
    case soft
    case solid
}

/// 라벨 작은 메타 배지. soft / solid 두 표현.
/// 라벨이 순수 숫자(또는 % / : 등 기호) 면 monospaced digit 폰트로 자동 전환.
struct AppBadge: View {
    let label: String
    let tone: AppBadgeTone
    var variant: AppBadgeVariant = .soft

    var body: some View {
        Text(label)
            .font(font)
            .foregroundStyle(textColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
    }

    // MARK: - Numeric heuristic

    private var isNumericLooking: Bool {
        // digits + . , % : / · space
        let allowed = CharacterSet(charactersIn: "0123456789.,%:/· ")
        return !label.isEmpty
            && label.unicodeScalars.allSatisfy { allowed.contains($0) }
    }

    private var font: Font {
        if isNumericLooking {
            return AppType.monoNumeric.weight(.semibold)
        } else {
            return AppType.caption.weight(.semibold)
        }
    }

    // MARK: - Colors

    private var backgroundColor: Color {
        switch variant {
        case .soft:  return tone.base.opacity(0.14)
        case .solid: return tone.base
        }
    }

    private var textColor: Color {
        switch variant {
        case .soft:  return tone.base
        case .solid: return .white
        }
    }
}

// MARK: - Preview

#Preview("AppBadge — tones × variants") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("Soft")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            FlexRow {
                AppBadge(label: "최신",   tone: .accent)
                AppBadge(label: "SQLD",  tone: .sqld)
                AppBadge(label: "실기",  tone: .engineerPractical)
                AppBadge(label: "필기",  tone: .engineerWritten)
                AppBadge(label: "컴활1", tone: .cl1)
                AppBadge(label: "컴활2", tone: .cl2)
                AppBadge(label: "ADsP", tone: .adsp)
                AppBadge(label: "성공",  tone: .success)
                AppBadge(label: "주의",  tone: .warning)
                AppBadge(label: "위험",  tone: .danger)
                AppBadge(label: "정보",  tone: .info)
                AppBadge(label: "보통",  tone: .neutral)
            }

            Text("Solid")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            FlexRow {
                AppBadge(label: "최신",   tone: .accent,            variant: .solid)
                AppBadge(label: "SQLD",  tone: .sqld,              variant: .solid)
                AppBadge(label: "실기",  tone: .engineerPractical, variant: .solid)
                AppBadge(label: "필기",  tone: .engineerWritten,   variant: .solid)
                AppBadge(label: "성공",  tone: .success,           variant: .solid)
                AppBadge(label: "위험",  tone: .danger,            variant: .solid)
            }

            Text("Numeric (mono digit)")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            FlexRow {
                AppBadge(label: "92%",    tone: .success)
                AppBadge(label: "3/10",   tone: .accent)
                AppBadge(label: "12:34",  tone: .info)
                AppBadge(label: "0.87",   tone: .neutral)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}

/// Lightweight preview helper — wraps badges into a flex row.
private struct FlexRow<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        HStack(alignment: .center, spacing: Spacing.sm) {
            content
        }
    }
}
