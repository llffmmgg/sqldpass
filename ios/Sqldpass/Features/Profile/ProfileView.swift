import SwiftUI

/// 내정보(마이) 탭 루트 화면.
///
/// 구조(위→아래) — 871lJPyM 디자인 핸드오프 `screens.jsx` MyScreen (line 348~) 기준:
/// 1. `AppPageHeader(title: "내정보")`
/// 2. Hero 카드 — 닉네임/구독 배지 + streak strip(최장 연속·요일 7셀)
/// 3. KPI 2x2 그리드 (총 풀이 / 평균 정답률 / 합격 확률 / 오답)
/// 4. 학습 섹션 — 오답노트 / 북마크 / 풀이 이력
/// 5. 계정 섹션 — 닉네임 편집 / Apple 구독 관리(활성 시) / PASS+ 구독 관리
/// 6. 설정·지원 섹션 — 피드백 / 이용약관 / 개인정보처리방침 / 로그아웃 / 회원 탈퇴
///
/// 메뉴는 `AppListGroupCard` 로 묶고 row 사이에 `AppListGroupDivider` 를 두어
/// 핸드오프의 `MenuList` 카드형 레이아웃을 그대로 따른다.
struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()
    @State private var showNicknameEdit = false
    @State private var showFeedback = false
    @State private var showPaywall = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    Text("내 정보")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, Spacing.xs)

                    ProfileHeroCard(
                        nickname: viewModel.me?.nickname,
                        provider: viewModel.me?.provider,
                        subscription: viewModel.subscription,
                        streak: viewModel.streak,
                        errorMessage: viewModel.errorMessage,
                        onUpgradeTap: { showPaywall = true }
                    )

                    learningSection
                    accountSection
                    settingsSupportSection
                }
                .padding(.horizontal, Spacing.base)
                .padding(.bottom, Spacing.xxl)
            }
            .background(Color.appPage)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.me == nil {
                    await viewModel.load()
                }
            }
            .navigationDestination(isPresented: $showNicknameEdit) {
                NicknameEditView(
                    current: viewModel.me?.nickname ?? "",
                    onUpdated: { viewModel.updateLocalNickname($0) }
                )
            }
            .navigationDestination(isPresented: $showPaywall) {
                PaywallView()
            }
            .sheet(isPresented: $showFeedback) {
                FeedbackComposeView(initialType: .other, questionId: nil)
            }
        }
    }

    // MARK: - 학습

    private var learningSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            SectionHeader(title: "학습")
            AppListGroupCard {
                NavigationLink {
                    WrongAnswersView()
                } label: {
                    MenuRow(icon: "doc.text.magnifyingglass", title: "오답노트")
                }
                .buttonStyle(.plain)

                AppListGroupDivider()

                NavigationLink {
                    BookmarksView()
                } label: {
                    MenuRow(icon: "star", title: "즐겨찾기")
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - 계정

    private var accountSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            SectionHeader(title: "계정")
            AppListGroupCard {
                Button {
                    showNicknameEdit = true
                } label: {
                    MenuRow(icon: "person.text.rectangle", title: "닉네임 편집")
                }
                .buttonStyle(.plain)

                AppListGroupDivider()

                NavigationLink {
                    PaywallView()
                } label: {
                    MenuRow(icon: "sparkles", title: "플랜 관리")
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - 설정·지원

    private var settingsSupportSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            SectionHeader(title: "설정·지원")
            AppListGroupCard {
                // 인앱 피드백 폼 — 백엔드 `POST /api/feedback` 호출.
                Button {
                    showFeedback = true
                } label: {
                    MenuRow(icon: "bubble.left.and.bubble.right", title: "피드백 보내기")
                }
                .buttonStyle(.plain)

                AppListGroupDivider()

                Link(destination: URL(string: "https://www.sqldpass.com/terms")!) {
                    MenuRow(icon: "doc.text", title: "이용약관")
                }

                AppListGroupDivider()

                Link(destination: URL(string: "https://www.sqldpass.com/privacy")!) {
                    MenuRow(icon: "lock.shield", title: "개인정보처리방침")
                }

                AppListGroupDivider()

                Button(role: .destructive) {
                    AuthStore.shared.signOut()
                } label: {
                    MenuRow(
                        icon: "rectangle.portrait.and.arrow.right",
                        title: "로그아웃",
                        tone: .danger
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - 섹션 헤더 / 메뉴 행

private struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(AppType.footnote.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
            .textCase(nil)
            .padding(.horizontal, Spacing.xs)
    }
}

private enum MenuRowTone {
    case normal
    case danger
}

private struct MenuRow: View {
    let icon: String
    let title: String
    var tone: MenuRowTone = .normal

    private var foreground: Color {
        switch tone {
        case .normal: return Color.appTextPrimary
        case .danger: return Color.semanticDanger
        }
    }

    private var iconForeground: Color {
        switch tone {
        case .normal: return Color.appTextMuted
        case .danger: return Color.semanticDanger
        }
    }

    var body: some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(iconForeground)
                .frame(width: 24, alignment: .center)
            Text(title)
                .font(AppType.body)
                .foregroundStyle(foreground)
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextSubtle)
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.md)
        .contentShape(Rectangle())
    }
}

#Preview {
    ProfileView()
}
