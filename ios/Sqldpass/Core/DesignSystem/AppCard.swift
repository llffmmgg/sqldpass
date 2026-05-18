import SwiftUI

// MARK: - Public API

/// Surface for ``AppCard``.
enum AppCardSurface {
    case card       // appSurface
    case elevated   // appElevated

    var fill: Color {
        switch self {
        case .card:     return .appSurface
        case .elevated: return .appElevated
        }
    }
}

/// Optional accent stripe color for ``AppCard``.
enum AppCardAccent {
    case none
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

    var color: Color? {
        switch self {
        case .none:              return nil
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
        }
    }
}

/// 카드 컨테이너. 그림자 없이 단단한 테두리 + 라운드 16 + 옵션 좌측 액센트 바.
/// `onTap` 이 있으면 Button 으로 감싸 press scale 을 적용한다.
struct AppCard<Content: View>: View {
    var surface: AppCardSurface = .card
    var accent: AppCardAccent = .none
    var onTap: (() -> Void)? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        if let onTap {
            Button(action: onTap) {
                cardBody
            }
            .buttonStyle(AppCardButtonStyle())
        } else {
            cardBody
        }
    }

    private var cardBody: some View {
        HStack(spacing: 0) {
            if let accentColor = accent.color {
                Rectangle()
                    .fill(accentColor)
                    .frame(width: 4)
                    .frame(maxHeight: .infinity)
            }
            VStack(alignment: .leading, spacing: Spacing.md) {
                content()
            }
            .padding(Spacing.base)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(surface.fill)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

// MARK: - Button style

struct AppCardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .animation(.spring(response: 0.2, dampingFraction: 0.75),
                       value: configuration.isPressed)
    }
}

// MARK: - Preview

#Preview("AppCard — surfaces + accents") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            AppCard {
                Text("Plain card")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Text("기본 surface, 액센트 없음")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }

            AppCard(surface: .elevated) {
                Text("Elevated card")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Text("appElevated 배경")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }

            ForEach(accents, id: \.label) { item in
                AppCard(accent: item.accent) {
                    Text(item.label)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("좌측 4pt 액센트 바")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                }
            }

            AppCard(accent: .sqld, onTap: {}) {
                Text("Tappable card")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Text("press scale 0.98 적용")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
        }
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}

private struct PreviewAccent {
    let label: String
    let accent: AppCardAccent
}

private let accents: [PreviewAccent] = [
    .init(label: "SQLD",              accent: .sqld),
    .init(label: "정보처리기사 실기", accent: .engineerPractical),
    .init(label: "정보처리기사 필기", accent: .engineerWritten),
    .init(label: "컴활 1급",          accent: .cl1),
    .init(label: "컴활 2급",          accent: .cl2),
    .init(label: "ADsP",              accent: .adsp),
    .init(label: "Success",           accent: .success),
    .init(label: "Warning",           accent: .warning),
    .init(label: "Danger",            accent: .danger),
    .init(label: "Info",              accent: .info)
]
