import SwiftUI

/// 홈 탭 — 새 디자인 핸드오프(screen-home.jsx) 구조에 맞춰 재구성.
///
/// 위→아래 정보 위계:
///  1) 인사말 row (날짜 + 닉네임 + 우측 44pt 마스코트 박스)
///  2) HERO 이어서 풀기 카드 (현재 placeholder)
///  3) Streak + 오늘 목표 2-column grid (.45/.55)
///  4) 내 자격증 수평 캐러셀
///  5) 약점 보강 — `WrongAnswerService.stats()` 상위 3개 (실패/빈 결과 시 섹션 숨김)
///  6) 오늘의 추천 placeholder 카드
///
/// 데이터 미통합 섹션(오늘 목표/추천 미션, 자격증별 진척도)은 "곧 제공돼요" placeholder 로 노출.
/// 자격증 카드 탭 시 기존 `CertInfoSheet` 흐름은 유지한다.
struct HomeView: View {
    @State private var viewModel = HomeViewModel()
    @State private var sheetCert: CertInfo? = nil
    @State private var showPaywall: Bool = false

    // 후속 phase 에서 lastCert/lastMode 가 통합되면 hero 카드를 active 상태로 전환한다.
    private var isHeroActive: Bool { false }

    private var nickname: String {
        viewModel.member?.nickname ?? "학습자"
    }

    /// "2026.05.19 · 화요일" 포맷. KST 기준의 한국어 요일.
    private var greetingDateLabel: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        formatter.dateFormat = "yyyy.MM.dd · EEEE"
        return formatter.string(from: Date())
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    greetingRow
                        .padding(.top, Spacing.sm)
                        .padding(.bottom, Spacing.lg)

                    HeroContinueCard(isActive: isHeroActive)
                        .padding(.bottom, Spacing.md)

                    streakAndGoalGrid
                        .padding(.bottom, Spacing.xl)

                    myCertsSection
                        .padding(.bottom, Spacing.xl)

                    if !viewModel.wrongStats.isEmpty {
                        weakAreasSection
                            .padding(.bottom, Spacing.xl)
                    }

                    todayPickSection

                    if let errorMessage = viewModel.errorMessage {
                        errorBanner(message: errorMessage)
                            .padding(.top, Spacing.lg)
                    }
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
                if viewModel.member == nil {
                    await viewModel.load()
                }
            }
            .overlay {
                if viewModel.isLoading && viewModel.member == nil {
                    ProgressView()
                        .controlSize(.large)
                }
            }
            .sheet(item: $sheetCert) { info in
                CertInfoSheet(
                    info: info,
                    onOpenPassPlus: {
                        showPaywall = true
                    }
                )
            }
            .sheet(isPresented: $showPaywall) {
                PaywallView()
            }
        }
    }

    // MARK: - Sections

    /// 1) 인사말 row — 날짜+요일 위에 큰 닉네임 문구, 우측에 44pt 마스코트 박스.
    private var greetingRow: some View {
        HStack(alignment: .center, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(greetingDateLabel)
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(Color.appTextSubtle)
                (
                    Text("안녕하세요, ")
                        .foregroundStyle(Color.appTextPrimary)
                    + Text(nickname)
                        .foregroundStyle(Color.brandPrimary)
                    + Text("님")
                        .foregroundStyle(Color.appTextPrimary)
                )
                .font(AppType.title.weight(.bold))
                .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: Spacing.sm)

            ZStack {
                RoundedRectangle(cornerRadius: Radius.md)
                    .fill(Color.appSurface)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(Color.appBorder, lineWidth: 1)
                    )
                AppMascot(pose: .greeting, sizeDp: 32, animateOnAppear: false)
            }
            .frame(width: 44, height: 44)
        }
    }

    /// 3) Streak (.45) + 오늘 목표 (.55) 2-column grid.
    /// SwiftUI 의 `GridItem(.flexible)` 두 개를 사용해 폭을 자동 분할한다.
    @ViewBuilder
    private var streakAndGoalGrid: some View {
        let columns = [
            GridItem(.flexible(), spacing: Spacing.md),
            GridItem(.flexible(), spacing: Spacing.md),
        ]
        LazyVGrid(columns: columns, alignment: .leading, spacing: Spacing.md) {
            // 좌측 — 실제 streak 있으면 카드, 없으면 placeholder (백엔드 미응답).
            if let streak = viewModel.streak {
                StreakCard(streak: streak)
            } else {
                streakPlaceholder
            }
            TodayGoalCard()
        }
    }

    /// streak fetch 실패/대기 시 동일한 frame 의 빈 카드.
    private var streakPlaceholder: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("STREAK")
                .font(AppType.caption.weight(.semibold))
                .tracking(1.2)
                .foregroundStyle(Color.appTextSubtle)
            Text("—")
                .font(AppType.monoNumericLarge)
                .foregroundStyle(Color.appTextMuted)
            Text("불러오는 중이에요")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextSubtle)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: Radius.lg)
                .fill(Color.appSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
    }

    /// 4) 내 자격증 — section header + 수평 캐러셀.
    private var myCertsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            AppSectionHeader(
                title: "내 자격증",
                action: SectionAction(label: "전체", onTap: {})
            )
            MyCertsCarousel(onCertTap: { info in
                sheetCert = info
            })
        }
    }

    /// 5) 약점 보강 — section header (subtitle 보조) + WeakAreasCard.
    private var weakAreasSection: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                AppSectionHeader(title: "약점 보강")
                Text("오답이 많은 영역부터 다시 풀어보기")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
            WeakAreasCard(stats: viewModel.wrongStats)
        }
    }

    /// 6) 오늘의 추천 — section header (subtitle 보조) + placeholder 카드.
    private var todayPickSection: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                AppSectionHeader(title: "오늘의 추천")
                Text("오늘의 추천 미션은 곧 제공돼요")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
            TodayPickCard()
        }
    }

    // MARK: - Error banner

    private func errorBanner(message: String) -> some View {
        HStack(alignment: .top, spacing: Spacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color.semanticDanger)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text("불러오기 실패")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Text(message)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
            Button("다시") {
                Task { await viewModel.load() }
            }
            .font(AppType.footnote)
            .foregroundStyle(Color.brandPrimary)
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

#Preview {
    HomeView()
}
