import SwiftUI

struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    profileHeader
                }
                Section("학습") {
                    NavigationLink {
                        HistoryView()
                    } label: {
                        Label("학습 기록", systemImage: "list.bullet.rectangle.portrait")
                    }
                    NavigationLink {
                        BookmarksView()
                    } label: {
                        Label("북마크", systemImage: "bookmark")
                    }
                    NavigationLink {
                        InsightsView()
                    } label: {
                        Label("인사이트", systemImage: "chart.line.uptrend.xyaxis")
                    }
                }
                Section("구독") {
                    NavigationLink {
                        PaywallView()
                    } label: {
                        Label("프리미엄 보기", systemImage: "crown")
                    }
                }
                Section("계정") {
                    NavigationLink {
                        NicknameEditView(
                            current: viewModel.me?.nickname ?? "",
                            onUpdated: { viewModel.updateLocalNickname($0) }
                        )
                    } label: {
                        Label("프로필 편집", systemImage: "person.text.rectangle")
                    }
                    Button(role: .destructive) {
                        AuthStore.shared.signOut()
                    } label: {
                        Label("로그아웃", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                    NavigationLink {
                        AccountDeletionConfirmView()
                    } label: {
                        Label("계정 삭제", systemImage: "person.crop.circle.badge.minus")
                            .foregroundStyle(Color.semanticDanger)
                    }
                }
            }
            .navigationTitle("내정보")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.me == nil {
                    await viewModel.load()
                }
            }
        }
    }

    private var profileHeader: some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: "person.crop.circle.fill")
                .resizable()
                .frame(width: 56, height: 56)
                .foregroundStyle(Color.appTextSubtle)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(viewModel.me?.nickname ?? "로그인 상태 확인 중")
                    .font(AppType.bodyEmph)
                if let provider = viewModel.me?.provider {
                    Text("\(provider.lowercased()) 계정")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
        }
        .padding(.vertical, Spacing.xs)
    }
}

#Preview {
    ProfileView()
}
