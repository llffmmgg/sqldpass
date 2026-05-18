import SwiftUI

/// 필터/선택용 칩. 자격증 칩 캐러셀, 책갈피 필터, 정렬 토글 등에서 재사용한다.
/// 둥근 capsule, idle/selected/disabled 3상태, optional 아이콘 + 카운트 배지.
struct AppChip: View {
    let label: String
    let isSelected: Bool
    var leadingSystemImage: String? = nil
    var count: Int? = nil
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: {
            guard isEnabled else { return }
            action()
        }) {
            HStack(spacing: Spacing.sm) {
                if let leadingSystemImage {
                    Image(systemName: leadingSystemImage)
                        .font(AppType.footnote.weight(.semibold))
                        .foregroundStyle(labelColor)
                }
                Text(label)
                    .font(AppType.callout.weight(isSelected ? .semibold : .regular))
                    .foregroundStyle(labelColor)
                    .lineLimit(1)
                if let count {
                    Text("\(count)")
                        .font(AppType.caption.monospacedDigit().weight(.semibold))
                        .foregroundStyle(countLabelColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(
                            Capsule().fill(countBackgroundColor)
                        )
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .frame(minHeight: 36)
            .background(backgroundColor)
            .overlay(
                Capsule()
                    .stroke(borderColor, lineWidth: borderWidth)
            )
            .clipShape(Capsule())
        }
        .buttonStyle(AppChipButtonStyle())
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1.0 : 0.45)
        .animation(.easeOut(duration: 0.18), value: isSelected)
    }

    // MARK: - Colors

    private var backgroundColor: Color {
        isSelected ? Color.brandPrimary.opacity(0.10) : Color.appElevated
    }
    private var borderColor: Color {
        isSelected ? Color.brandPrimary : Color.appBorder
    }
    private var borderWidth: CGFloat {
        isSelected ? 1.5 : 1
    }
    private var labelColor: Color {
        isSelected ? Color.brandPrimary : Color.appTextPrimary
    }
    private var countBackgroundColor: Color {
        isSelected ? Color.brandPrimary : Color.appSurface
    }
    private var countLabelColor: Color {
        isSelected ? Color.brandPrimaryFG : Color.appTextMuted
    }
}

// MARK: - Press style

struct AppChipButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.18, dampingFraction: 0.7),
                       value: configuration.isPressed)
    }
}

// MARK: - Preview

#Preview("AppChip — states") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("Idle / Selected")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(spacing: Spacing.sm) {
                AppChip(label: "전체", isSelected: false) {}
                AppChip(label: "SQLD", isSelected: true) {}
                AppChip(label: "ADsP", isSelected: false) {}
            }

            Text("With leading icon")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(spacing: Spacing.sm) {
                AppChip(label: "최신순", isSelected: true,
                        leadingSystemImage: "arrow.down") {}
                AppChip(label: "오답률",
                        isSelected: false,
                        leadingSystemImage: "exclamationmark.triangle") {}
            }

            Text("With count badge")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(spacing: Spacing.sm) {
                AppChip(label: "책갈피", isSelected: false, count: 12) {}
                AppChip(label: "오답",
                        isSelected: true,
                        leadingSystemImage: "xmark.circle",
                        count: 47) {}
            }

            Text("Disabled")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(spacing: Spacing.sm) {
                AppChip(label: "준비 중", isSelected: false, isEnabled: false) {}
                AppChip(label: "잠금",
                        isSelected: true,
                        leadingSystemImage: "lock.fill",
                        isEnabled: false) {}
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
