import SwiftUI

struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    header
                    goalPanel
                    menuSection
                    accountSection
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
            .navigationTitle("마이")
            .navigationBarTitleDisplayMode(.inline)
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

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("마이노트")
                .font(AppType.heading.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("\(viewModel.me?.nickname ?? "학습자")님의 학습 기록과 계정을 관리해요.")
                .font(AppType.callout)
                .foregroundStyle(Color.appTextMuted)
        }
        .padding(.top, Spacing.base)
    }

    private var goalPanel: some View {
        AppPanel {
            HStack {
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text("나의 목표 점수")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("150점")
                        .font(AppType.heading.weight(.bold))
                        .foregroundStyle(Color.brandPrimary)
                }
                Spacer()
                Image(systemName: "target")
                    .font(.title)
                    .foregroundStyle(Color.brandPrimary)
            }
            ProgressView(value: 0.0, total: 150.0)
                .tint(Color.brandPrimary)
            NavigationLink {
                InsightsView()
            } label: {
                Text("현재 점수 확인하기")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.brandPrimaryFG)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(Color.brandPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
            }
            .buttonStyle(.plain)
        }
    }

    private var menuSection: some View {
        VStack(spacing: Spacing.md) {
            NavigationLink { HistoryView() } label: {
                MenuRow(icon: "mic.fill", title: "나의 모의고사", subtitle: "응시 기록 보기")
            }
            NavigationLink { WrongAnswersView() } label: {
                MenuRow(icon: "flame.fill", title: "나의 오답노트", subtitle: "틀린 문제 다시 풀기")
            }
            NavigationLink { BookmarksView() } label: {
                MenuRow(icon: "bookmark.fill", title: "저장한 문제", subtitle: "북마크 모아보기")
            }
            NavigationLink { PaywallView() } label: {
                MenuRow(icon: "crown.fill", title: "프리미엄", subtitle: "구독과 복원 관리")
            }
        }
        .buttonStyle(.plain)
    }

    private var accountSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            AppSectionHeader(title: "계정")
            AppPanel {
                if let provider = viewModel.me?.provider {
                    Text("\(provider.lowercased()) 계정으로 로그인됨")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }

                NavigationLink {
                    NicknameEditView(
                        current: viewModel.me?.nickname ?? "",
                        onUpdated: { viewModel.updateLocalNickname($0) }
                    )
                } label: {
                    Label("프로필 편집", systemImage: "person.text.rectangle")
                }
                .font(AppType.body)
                .foregroundStyle(Color.appTextPrimary)

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
    }
}

private struct MenuRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        AppPanel {
            HStack(spacing: Spacing.base) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(Color.brandPrimary.opacity(0.72))
                    .frame(width: 44, height: 44)
                    .background(Color.appElevated)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(title)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text(subtitle)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
    }
}

#Preview {
    ProfileView()
}
