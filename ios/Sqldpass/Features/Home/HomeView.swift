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
    @Binding var selectedTab: MainTab

    @State private var viewModel = HomeViewModel()
    @State private var showPaywall: Bool = false
    @State private var showNoticeSheet = false
    @State private var showWrongAnswers = false

    /// 시험 D-day 설정값. `@AppStorage` 로 영속화 — 미설정(0) 이면 오늘 + 300일 기본값.
    @AppStorage("home.examTargetDate") private var examDateRaw: Double = 0
    @State private var showExamDatePicker = false
    @State private var draftExamDate: Date = Date()

    private var nickname: String {
        viewModel.member?.nickname ?? "학습자"
    }

    private var examDate: Date {
        examDateRaw > 0
            ? Date(timeIntervalSince1970: examDateRaw)
            : Calendar.current.date(byAdding: .day, value: 300, to: Date()) ?? Date()
    }

    private var daysRemaining: Int {
        let cal = Calendar.current
        let start = cal.startOfDay(for: Date())
        let end = cal.startOfDay(for: examDate)
        return cal.dateComponents([.day], from: start, to: end).day ?? 0
    }

    private var dDayText: String {
        if daysRemaining == 0 { return "D-DAY" }
        if daysRemaining < 0 { return "D+\(-daysRemaining)" }
        return "D-\(daysRemaining)"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HomeBrandHeader(
                    onPlanTap: { showPaywall = true },
                    onNotificationTap: { showNoticeSheet = true },
                    onProfileTap: { selectedTab = .profile }
                )

                ScrollView {
                    VStack(spacing: 0) {
                        HomeDDayBanner(
                            dDayText: dDayText,
                            daysRemaining: max(0, daysRemaining),
                            nickname: nickname,
                            streakDays: viewModel.streak?.currentStreak ?? 0,
                            onEditExamDateTap: {
                                draftExamDate = examDate
                                showExamDatePicker = true
                            }
                        )

                        VStack(alignment: .leading, spacing: 0) {
                            KpiGrid(kpi: viewModel.kpi)
                                .padding(.top, Spacing.lg)
                                .padding(.bottom, Spacing.md)

                            HomeWeekAttendanceStrip(counts: viewModel.weekCounts)
                                .padding(.bottom, Spacing.lg)

                            myCertsSection
                                .padding(.bottom, Spacing.lg)

                            if !viewModel.wrongStats.isEmpty {
                                weakAreasSection
                                    .padding(.bottom, Spacing.lg)
                            } else if viewModel.wrongStatsLocked {
                                weakAreasLockedSection
                                    .padding(.bottom, Spacing.lg)
                            }

                            if let errorMessage = viewModel.errorMessage {
                                errorBanner(message: errorMessage)
                                    .padding(.top, Spacing.lg)
                            }
                        }
                        .padding(.horizontal, Spacing.base)
                        .padding(.bottom, Spacing.xxl)
                        .frame(maxWidth: .infinity, minHeight: 1200, alignment: .top)
                        .background(Color.appPage)
                    }
                }
                .background(Color.brandPrimary) // D-day 위쪽(위로 bounce 시) 은 초록 유지, 흰 본문 minHeight 가 아래 영역을 덮음
            }
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
            .sheet(isPresented: $showPaywall) {
                PaywallView()
            }
            .sheet(isPresented: $showExamDatePicker) {
                examDatePickerSheet
            }
            .sheet(isPresented: $showNoticeSheet) {
                noticeSheet
            }
            .navigationDestination(isPresented: $showWrongAnswers) {
                WrongAnswersView()
            }
        }
    }

    // MARK: - Notice sheet (공지)

    private var noticeSheet: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                Image(systemName: "megaphone")
                    .font(.system(size: 44, weight: .regular))
                    .foregroundStyle(Color.appTextSubtle)
                Text("공지가 없어요")
                    .font(AppType.heading)
                    .foregroundStyle(Color.appTextPrimary)
                Text("새 공지가 등록되면 여기에서 알려드릴게요.")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                    .multilineTextAlignment(.center)
            }
            .padding(Spacing.lg)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appPage)
            .navigationTitle("공지")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { showNoticeSheet = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Exam date sheet

    private var examDatePickerSheet: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker(
                        "시험일",
                        selection: $draftExamDate,
                        in: Date()...,
                        displayedComponents: .date
                    )
                    .datePickerStyle(.graphical)
                } footer: {
                    Text("선택한 날짜를 기준으로 D-day 가 매일 갱신돼요.")
                }
            }
            .navigationTitle("시험일 설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { showExamDatePicker = false }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("저장") {
                        examDateRaw = draftExamDate.timeIntervalSince1970
                        showExamDatePicker = false
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Sections

    /// 자격증 둘러보기 — 시험 정보 카드 캐러셀 (진척도/탭 액션 없음, 순수 정보).
    /// 캐러셀은 화면 풀폭으로 그리기 위해 부모 horizontal padding 을 음수로 상쇄한다.
    private var myCertsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("자격증 둘러보기")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            HomeCertCarousel()
                .padding(.horizontal, -Spacing.base)
        }
    }

    /// 약점 TOP5 — 인라인 헤더(bodyEmph) + WeakAreasCard. row 탭 시 오답노트로 push.
    private var weakAreasSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("약점 TOP5")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            WeakAreasCard(stats: viewModel.wrongStats) { _ in
                showWrongAnswers = true
            }
        }
    }

    /// 약점 TOP5 잠금 — 플랜 미가입으로 오답노트 API 가 거부된 경우.
    private var weakAreasLockedSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("약점 TOP5")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)

            Button {
                showPaywall = true
            } label: {
                HStack(alignment: .center, spacing: Spacing.md) {
                    ZStack {
                        Circle()
                            .fill(Color.brandPrimary.opacity(0.12))
                        Image(systemName: "lock.fill")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(Color.brandPrimary)
                    }
                    .frame(width: 40, height: 40)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("플랜 전용 기능이에요")
                            .font(AppType.callout.weight(.semibold))
                            .foregroundStyle(Color.appTextPrimary)
                        Text("플랜에 가입하면 약점 영역을 분석해드려요")
                            .font(AppType.caption)
                            .foregroundStyle(Color.appTextMuted)
                            .lineLimit(1)
                    }

                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextSubtle)
                }
                .padding(Spacing.base)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.appSurface)
                .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                        .stroke(Color.appTextMuted.opacity(0.25), lineWidth: 0.5)
                )
            }
            .buttonStyle(.plain)
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
    HomeView(selectedTab: .constant(.home))
}
