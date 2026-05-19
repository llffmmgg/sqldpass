import SwiftUI

/// 같은 카테고리의 row 들을 단일 카드 안에 묶는 컨테이너.
///
/// 카드 chrome (radius, border, surface) 은 한 번만 그려지고, 내부 row 사이에는 1pt
/// divider 만 들어간다. Profile 의 메뉴 list, Practice 의 cert grouped 과목 list, Home 의
/// 약점 보강 list 등에 공통 사용.
///
/// 디자인 출처: 871lJPyM `screens.jsx` 의 `MenuList`, Practice grouped card.
struct AppListGroupCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
    }
}

/// `AppListGroupCard` 안에 들어가는 row 사이 구분선. caller 가 첫 row 이후에만 삽입.
struct AppListGroupDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.appBorder)
            .frame(height: 1)
    }
}

#Preview("AppListGroupCard") {
    ScrollView {
        VStack(spacing: Spacing.md) {
            AppListGroupCard {
                ForEach(0..<3, id: \.self) { i in
                    if i > 0 { AppListGroupDivider() }
                    HStack(spacing: Spacing.md) {
                        Image(systemName: "doc.text")
                            .foregroundStyle(Color.brandPrimary)
                        Text("Row \(i + 1)")
                            .font(AppType.body)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextSubtle)
                    }
                    .padding(.horizontal, Spacing.base)
                    .padding(.vertical, Spacing.md)
                }
            }
        }
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}
