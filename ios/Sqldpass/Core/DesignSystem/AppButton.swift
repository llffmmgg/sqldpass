import SwiftUI

// MARK: - Public API

/// Visual variant for ``AppButton``.
enum AppButtonVariant {
    case primary
    case secondary
    case tertiary
    case destructive
}

/// Size for ``AppButton``. Drives min-height and vertical padding.
enum AppButtonSize {
    case compact   // 40pt
    case regular   // 48pt
    case large     // 56pt

    var minHeight: CGFloat {
        switch self {
        case .compact: return 40
        case .regular: return 48
        case .large:   return 56
        }
    }

    var verticalPadding: CGFloat {
        switch self {
        case .compact: return 8
        case .regular: return 12
        case .large:   return 16
        }
    }

    var labelFont: Font {
        switch self {
        case .compact: return AppType.callout.weight(.semibold)
        case .regular: return AppType.bodyEmph
        case .large:   return AppType.bodyEmph
        }
    }
}

/// 브랜드 통합 버튼. `Button` + custom `ButtonStyle` 조합으로 chrome 을 직접 소유한다.
/// 기본 `.bordered*` 스타일을 절대 사용하지 않는다.
struct AppButton: View {
    let title: String
    var variant: AppButtonVariant = .primary
    var size: AppButtonSize = .regular
    var isEnabled: Bool = true
    var isLoading: Bool = false
    var leadingSystemImage: String? = nil
    let action: () -> Void

    @State private var pressedAt: Date = .distantPast

    private var allowsHaptic: Bool {
        variant == .primary || variant == .destructive
    }

    var body: some View {
        Button {
            guard isEnabled, !isLoading else { return }
            if allowsHaptic {
                pressedAt = Date()
            }
            action()
        } label: {
            HStack(spacing: Spacing.sm) {
                if isLoading {
                    ProgressView()
                        .controlSize(.small)
                        .tint(foregroundColor)
                        .frame(width: 18, height: 18)
                } else {
                    if let leadingSystemImage {
                        Image(systemName: leadingSystemImage)
                            .font(size.labelFont)
                            .foregroundStyle(foregroundColor)
                    }
                    Text(title)
                        .font(size.labelFont)
                        .foregroundStyle(foregroundColor)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(minHeight: size.minHeight)
            .padding(.horizontal, Spacing.base)
            .padding(.vertical, size.verticalPadding)
            .background(backgroundColor)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.sm)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
        }
        .buttonStyle(AppButtonStyle())
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1.0 : 0.45)
        .sensoryFeedback(.impact(weight: .light), trigger: pressedAt)
    }

    // MARK: Color tokens

    private var backgroundColor: Color {
        switch variant {
        case .primary:     return .brandPrimary
        case .secondary:   return .appSurface
        case .tertiary:    return .clear
        case .destructive: return .semanticDanger
        }
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary:     return .brandPrimaryFG
        case .secondary:   return .appTextPrimary
        case .tertiary:    return .brandPrimary
        case .destructive: return .white
        }
    }

    private var borderColor: Color {
        switch variant {
        case .secondary: return .appBorder
        default:         return .clear
        }
    }

    private var borderWidth: CGFloat {
        variant == .secondary ? 1 : 0
    }
}

// MARK: - Custom ButtonStyle (press scale)

struct AppButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.18, dampingFraction: 0.7),
                       value: configuration.isPressed)
    }
}

// MARK: - Preview

#Preview("AppButton — variants × sizes × states") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            ForEach([AppButtonVariant.primary,
                     .secondary,
                     .tertiary,
                     .destructive], id: \.self) { variant in
                VStack(alignment: .leading, spacing: Spacing.sm) {
                    Text(label(for: variant))
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.appTextMuted)

                    AppButton(title: "Compact",
                              variant: variant,
                              size: .compact,
                              leadingSystemImage: "play.fill") {}
                    AppButton(title: "Regular",
                              variant: variant,
                              size: .regular) {}
                    AppButton(title: "Large",
                              variant: variant,
                              size: .large) {}
                    AppButton(title: "Disabled",
                              variant: variant,
                              size: .regular,
                              isEnabled: false) {}
                    AppButton(title: "Loading...",
                              variant: variant,
                              size: .regular,
                              isLoading: true) {}
                }
            }
        }
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}

private func label(for v: AppButtonVariant) -> String {
    switch v {
    case .primary:     return "PRIMARY"
    case .secondary:   return "SECONDARY"
    case .tertiary:    return "TERTIARY"
    case .destructive: return "DESTRUCTIVE"
    }
}

extension AppButtonVariant: Hashable {}
