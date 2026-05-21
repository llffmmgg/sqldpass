import SwiftUI

/// 하단 5탭 커스텀 바 — SwiftUI TabView 의 iOS 18/26 floating pill 디자인을 우회.
///
/// MainTabView 가 `safeAreaInset(edge: .bottom)` 으로 본 바를 콘텐츠 아래에 깐다.
/// 시스템 home indicator 영역은 safeAreaInset 이 자동 흡수 → 흰 띠 0.
struct CustomTabBar: View {
    @Binding var selection: MainTab

    private struct Item {
        let tab: MainTab
        let label: String
        let icon: String
    }

    private static let items: [Item] = [
        .init(tab: .home,      label: "홈",       icon: "house.fill"),
        .init(tab: .mockExams, label: "모의고사",  icon: "doc.text.fill"),
        .init(tab: .pastExams, label: "기출복원",  icon: "clock.arrow.circlepath"),
        .init(tab: .soloSolve, label: "실전 문제", icon: "play.circle.fill"),
        .init(tab: .profile,   label: "내 정보",   icon: "person.crop.circle.fill"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.appBorder.opacity(0.5))
                .frame(height: 1)
            HStack(spacing: 0) {
                ForEach(Self.items, id: \.tab) { item in
                    tabButton(item)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.top, Spacing.xs)
            .padding(.bottom, Spacing.xs)
        }
        .background(Color.appPage)
    }

    @ViewBuilder
    private func tabButton(_ item: Item) -> some View {
        let isSelected = selection == item.tab
        Button {
            guard selection != item.tab else { return }
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) {
                UIView.performWithoutAnimation {
                    selection = item.tab
                }
            }
        } label: {
            VStack(spacing: 2) {
                Image(systemName: item.icon)
                    .font(.system(size: 22))
                Text(item.label)
                    .font(.system(size: 10, weight: isSelected ? .semibold : .medium))
            }
            .foregroundStyle(isSelected ? Color.brandPrimary : Color.appTextMuted)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
