import SwiftUI

/// 내정보(마이) 탭 루트 화면.
///
/// 구조(위→아래) — `docs/MOBILE_UX_SPEC.md` § 2.5 의 단일 진실 원천:
/// 1. 헤더 섹션: 프로필 헤더(닉네임·구독 배지) + KPI 2x2 그리드
/// 2. 학습 섹션: 오답노트 / 북마크 / 풀이 이력
/// 3. 계정 섹션: 닉네임 편집 / 결제·청구 / PASS+ 구독 관리
/// 4. 설정·지원 섹션: 테마 / 알림 / 피드백 / 공지·약관 / 로그아웃 / 회원 탈퇴
///
/// 본 phase(`mobile-ux-restructure` step 7) 에서는 진입 NavigationLink 만
/// 추가하고 진입 화면(`WrongAnswersView` 등) 본체는 손대지 않는다.
struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()
    @State private var showNicknameEdit = false
    @State private var showFeedback = false

    var body: some View {
        NavigationStack {
            List {
                headerSection
                learningSection
                accountSection
                settingsSupportSection
            }
            .listStyle(.insetGrouped)
            .background(Color.appPage)
            .scrollContentBackground(.hidden)
            .navigationTitle("내정보")
            .navigationBarTitleDisplayMode(.inline)
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
            .sheet(isPresented: $showFeedback) {
                FeedbackComposeView(initialType: .other, questionId: nil)
            }
        }
    }

    // MARK: - Header (프로필 헤더 + KPI)

    @ViewBuilder
    private var headerSection: some View {
        Section {
            ProfileHeaderRow(
                nickname: viewModel.me?.nickname,
                provider: viewModel.me?.provider,
                subscription: viewModel.subscription,
                errorMessage: viewModel.errorMessage
            )
            .listRowInsets(EdgeInsets(top: Spacing.md, leading: Spacing.base,
                                     bottom: Spacing.md, trailing: Spacing.base))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            KpiGrid(kpi: viewModel.kpi)
                .listRowInsets(EdgeInsets(top: 0, leading: Spacing.base,
                                         bottom: Spacing.sm, trailing: Spacing.base))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
        }
    }

    // MARK: - 학습

    private var learningSection: some View {
        Section {
            NavigationLink {
                WrongAnswersView()
            } label: {
                MenuRowLabel(icon: "doc.text.magnifyingglass", title: "오답노트")
            }
            NavigationLink {
                BookmarksView()
            } label: {
                MenuRowLabel(icon: "bookmark", title: "북마크")
            }
            NavigationLink {
                HistoryView()
            } label: {
                MenuRowLabel(icon: "clock.arrow.circlepath", title: "풀이 이력")
            }
        } header: {
            SectionHeader(title: "학습")
        }
    }

    // MARK: - 계정

    private var accountSection: some View {
        Section {
            Button {
                showNicknameEdit = true
            } label: {
                MenuRowLabel(icon: "person.text.rectangle", title: "닉네임 편집")
            }
            .buttonStyle(.plain)

            // 활성 구독이 있을 때만 Apple 구독 관리 페이지로 이동시킨다.
            // 비활성 상태에서는 결제 내역 자체가 없으므로 행 자체를 숨김.
            if viewModel.subscription?.active == true {
                Link(destination: URL(string: "https://apps.apple.com/account/subscriptions")!) {
                    MenuRowLabel(icon: "creditcard", title: "Apple 구독 관리")
                }
            }

            NavigationLink {
                PaywallView()
            } label: {
                MenuRowLabel(icon: "crown.fill", title: "PASS+ 구독 관리")
            }
        } header: {
            SectionHeader(title: "계정")
        }
    }

    // MARK: - 설정·지원

    private var settingsSupportSection: some View {
        Section {
            // 인앱 피드백 폼 — 백엔드 `POST /api/feedback` 호출.
            Button {
                showFeedback = true
            } label: {
                MenuRowLabel(icon: "bubble.left.and.bubble.right", title: "피드백 보내기")
            }
            .buttonStyle(.plain)

            Link(destination: URL(string: "https://www.sqldpass.com/terms")!) {
                MenuRowLabel(icon: "doc.text", title: "이용약관")
            }

            Link(destination: URL(string: "https://www.sqldpass.com/privacy")!) {
                MenuRowLabel(icon: "lock.shield", title: "개인정보처리방침")
            }

            Button(role: .destructive) {
                AuthStore.shared.signOut()
            } label: {
                MenuRowLabel(
                    icon: "rectangle.portrait.and.arrow.right",
                    title: "로그아웃",
                    tone: .danger
                )
            }
            .buttonStyle(.plain)

            NavigationLink {
                AccountDeletionConfirmView()
            } label: {
                MenuRowLabel(
                    icon: "person.crop.circle.badge.minus",
                    title: "회원 탈퇴",
                    tone: .danger
                )
            }
        } header: {
            SectionHeader(title: "설정·지원")
        }
    }
}

// MARK: - 프로필 헤더 행 (닉네임 + 구독 상태 배지)

private struct ProfileHeaderRow: View {
    let nickname: String?
    let provider: String?
    let subscription: SubscriptionInfo?
    let errorMessage: String?

    var body: some View {
        HStack(spacing: Spacing.base) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 44))
                .foregroundStyle(Color.brandPrimary)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(nickname ?? "학습자")
                    .font(AppType.heading.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(1)
                if let provider {
                    HStack(spacing: Spacing.xs) {
                        SubscriptionBadge(subscription: subscription)
                        Text("\(provider.lowercased()) 로그인")
                            .font(AppType.caption)
                            .foregroundStyle(Color.appTextMuted)
                    }
                } else if let errorMessage {
                    Text(errorMessage)
                        .font(AppType.caption)
                        .foregroundStyle(Color.semanticDanger)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

/// 구독 상태 배지 (헤더 영역). `SubscriptionInfo.displayBadgeLabel` 을 그대로 노출.
/// 활성이면 plan 명 또는 "PRO" 색상 강조, 비활성이면 회색 "FREE".
private struct SubscriptionBadge: View {
    let subscription: SubscriptionInfo?

    private var label: String {
        subscription?.displayBadgeLabel ?? "FREE"
    }

    private var isActive: Bool {
        subscription?.active ?? false
    }

    var body: some View {
        Text(label)
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(isActive ? Color.brandPrimary : Color.appTextMuted)
            .padding(.horizontal, Spacing.xs)
            .padding(.vertical, 2)
            .background(isActive ? Color.brandPrimary.opacity(0.12) : Color.appElevated)
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
    }
}

// MARK: - 섹션 헤더 / 메뉴 행 라벨

private struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(AppType.footnote.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
            .textCase(nil)
    }
}

private enum MenuRowTone {
    case normal
    case danger
}

private struct MenuRowLabel: View {
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
        }
        .contentShape(Rectangle())
    }
}

#Preview {
    ProfileView()
}
