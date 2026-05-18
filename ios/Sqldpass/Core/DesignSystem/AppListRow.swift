import SwiftUI

/// 설정/메뉴 리스트의 한 줄. leading 아이콘 + 제목/서브타이틀 + trailing (텍스트 또는 chevron).
///
/// 구분선은 부모(예: VStack 사이 Divider) 책임. 자체 보더/그림자 없음.
struct AppListRow: View {
    let title: String
    var subtitle: String? = nil
    var leadingSystemImage: String? = nil
    var leadingTint: Color = .appTextMuted
    var trailing: String? = nil
    var trailingSystemImage: String = "chevron.right"
    var destructive: Bool = false
    var isEnabled: Bool = true
    var onTap: (() -> Void)? = nil

    var body: some View {
        Group {
            if let onTap, isEnabled {
                Button(action: onTap) {
                    content
                }
                .buttonStyle(AppListRowButtonStyle())
            } else {
                content
            }
        }
        .opacity(isEnabled ? 1.0 : 0.45)
    }

    private var content: some View {
        HStack(alignment: .center, spacing: Spacing.md) {
            if let leadingSystemImage {
                Image(systemName: leadingSystemImage)
                    .font(AppType.body)
                    .foregroundStyle(leadingColor)
                    .frame(width: 24, height: 24)
            }

            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(title)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(titleColor)
                    .fixedSize(horizontal: false, vertical: true)
                if let subtitle {
                    Text(subtitle)
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let trailing {
                Text(trailing)
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
                    .lineLimit(1)
            }

            if onTap != nil {
                Image(systemName: trailingSystemImage)
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(minHeight: 56)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .contentShape(Rectangle())
    }

    private var titleColor: Color {
        destructive ? .semanticDanger : .appTextPrimary
    }

    private var leadingColor: Color {
        destructive ? .semanticDanger : leadingTint
    }
}

// MARK: - Press style

struct AppListRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.18, dampingFraction: 0.7),
                       value: configuration.isPressed)
    }
}

// MARK: - Preview

#Preview("AppListRow — variants") {
    ScrollView {
        VStack(spacing: 1) {
            AppListRow(title: "기본",
                       leadingSystemImage: "person",
                       onTap: {})
            Divider()
            AppListRow(title: "서브타이틀 있음",
                       subtitle: "두 번째 줄 보조 설명",
                       leadingSystemImage: "envelope",
                       onTap: {})
            Divider()
            AppListRow(title: "현재 자격증",
                       leadingSystemImage: "graduationcap",
                       trailing: "SQLD",
                       onTap: {})
            Divider()
            AppListRow(title: "알림 설정",
                       subtitle: "푸시 알림과 이메일 발송 설정",
                       leadingSystemImage: "bell",
                       trailing: "켜짐",
                       onTap: {})
            Divider()
            AppListRow(title: "로그아웃",
                       leadingSystemImage: "rectangle.portrait.and.arrow.right",
                       destructive: true,
                       onTap: {})
            Divider()
            AppListRow(title: "계정 삭제",
                       subtitle: "되돌릴 수 없습니다",
                       leadingSystemImage: "trash",
                       destructive: true,
                       onTap: {})
            Divider()
            AppListRow(title: "비활성 항목",
                       subtitle: "준비 중",
                       leadingSystemImage: "lock",
                       isEnabled: false)
            Divider()
            AppListRow(title: "탭 불가 (정보 행)",
                       leadingSystemImage: "info.circle",
                       trailing: "v1.0.0")
        }
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}
