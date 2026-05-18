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
    @State private var pendingNotice: String?

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
            .alert(
                "안내",
                isPresented: Binding(
                    get: { pendingNotice != nil },
                    set: { if !$0 { pendingNotice = nil } }
                ),
                actions: {
                    Button("확인", role: .cancel) { pendingNotice = nil }
                },
                message: {
                    Text(pendingNotice ?? "")
                }
            )
        }
    }

    // MARK: - Header (프로필 헤더 + KPI)

    @ViewBuilder
    private var headerSection: some View {
        Section {
            ProfileHeaderRow(
                nickname: viewModel.me?.nickname,
                provider: viewModel.me?.provider,
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

            // PaymentHistoryView 는 아직 없음 — placeholder.
            // 결제·청구 화면 구현은 별 phase 의 책임.
            Button {
                pendingNotice = "결제·청구 내역 화면은 곧 출시됩니다."
            } label: {
                MenuRowLabel(icon: "creditcard", title: "결제·청구")
            }
            .buttonStyle(.plain)

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
            // 테마 — 현재 iOS 는 시스템 다크 모드 자동 적응. 명시적 토글 화면은 아직 없음.
            Button {
                pendingNotice = "테마는 시스템 설정을 따릅니다. 별도 토글은 곧 추가됩니다."
            } label: {
                MenuRowLabel(icon: "moon.circle", title: "테마")
            }
            .buttonStyle(.plain)

            // 알림 — 푸시 알림 없음(plan Q15). 인앱 안내 placeholder 유지.
            Button {
                pendingNotice = "알림 설정은 곧 출시됩니다."
            } label: {
                MenuRowLabel(icon: "bell", title: "알림")
            }
            .buttonStyle(.plain)

            // 피드백 placeholder — Android 미러: 인앱 폼은 별 phase.
            Button {
                pendingNotice = "피드백은 heehun3658@gmail.com 으로 보내주세요."
            } label: {
                MenuRowLabel(icon: "bubble.left.and.bubble.right", title: "피드백")
            }
            .buttonStyle(.plain)

            Link(destination: URL(string: "https://www.sqldpass.com/terms")!) {
                MenuRowLabel(icon: "doc.text", title: "공지·약관")
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
                        SubscriptionBadge()
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

/// 구독 상태 배지 (헤더 영역).
///
/// 본 step 에서는 정적 "FREE" 라벨만 노출 — 실제 구독 상태 조회는
/// ProfileViewModel 에 SubscriptionInfo 필드를 추가하는 별 phase 작업.
private struct SubscriptionBadge: View {
    var body: some View {
        Text("FREE")
            .font(AppType.caption.weight(.semibold))
            .foregroundStyle(Color.appTextMuted)
            .padding(.horizontal, Spacing.xs)
            .padding(.vertical, 2)
            .background(Color.appElevated)
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
