import SwiftUI

struct ProfileView: View {
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: Spacing.md) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .frame(width: 56, height: 56)
                            .foregroundStyle(Color.appTextSubtle)
                        VStack(alignment: .leading, spacing: Spacing.xxs) {
                            Text("로그인하지 않음")
                                .font(AppType.bodyEmph)
                            Text("Google 또는 Apple 로 로그인")
                                .font(AppType.footnote)
                                .foregroundStyle(Color.appTextMuted)
                        }
                    }
                    .padding(.vertical, Spacing.xs)
                }

                Section("학습") {
                    Label("푼 문제 기록", systemImage: "list.bullet.rectangle.portrait")
                    Label("북마크", systemImage: "bookmark")
                    Label("인사이트", systemImage: "chart.line.uptrend.xyaxis")
                }

                Section("구독") {
                    Label("프리미엄 보기", systemImage: "crown")
                }

                Section("계정") {
                    Label("설정", systemImage: "gearshape")
                    Label("문의하기", systemImage: "envelope")
                }
            }
            .navigationTitle("내정보")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

#Preview {
    ProfileView()
}
