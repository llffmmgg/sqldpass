import SwiftUI

struct AppHeroHeader<Accessory: View>: View {
    let eyebrow: String
    let title: String
    let subtitle: String
    @ViewBuilder let accessory: Accessory

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            HStack(alignment: .center) {
                Text(eyebrow)
                    .font(AppType.heading.weight(.bold))
                    .foregroundStyle(Color.brandPrimary)
                Spacer()
                accessory
            }

            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text(title)
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.top, Spacing.xxl)
        .padding(.bottom, Spacing.xl)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appPage)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}

struct AppSheet<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            content
        }
        .padding(.horizontal, Spacing.base)
        .padding(.top, Spacing.lg)
        .padding(.bottom, Spacing.xxl)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appPage)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

struct AppPanel<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            content
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

struct MetricTile: View {
    let title: String
    let value: String
    let caption: String
    let icon: String
    var color: Color = .brandPrimary

    var body: some View {
        AppPanel {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
            Text(title)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
                Text(value)
                    .font(AppType.monoNumericLarge.weight(.bold))
                    .foregroundStyle(color)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                Text(caption)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
    }
}
