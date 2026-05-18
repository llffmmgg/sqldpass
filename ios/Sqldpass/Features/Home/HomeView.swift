import SwiftUI

/// 홈 탭 — 오늘 상태 + 다음 행동 추천.
///
/// 위→아래 정보 위계 (단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1):
///  1) 인사말 헤더 (닉네임 + 짧은 멘트)
///  2) 스트릭 카드 (위험 톤 분기)
///  3) 이어풀기 추천 카드 — 본 step 에서는 lastCert/lastMode 데이터 미통합 → 항상 hidden.
///     후속 phase 에서 lastCert/lastMode 통합 후 enable.
///  4) 자격증 6종 수평 캐러셀 → 카드 탭 시 `.sheet(.medium) CertInfoSheet` 표시
///
/// 본 step 은 step 5 가 MainTabView 의 .dashboard 탭을 HomeView 로 연결할 때까지
/// MainTabView 에서 호출되지 않는다. 컴파일은 통과해야 하므로 자체 NavigationStack
/// 안에서 동작 가능하도록 구성.
///
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/HomeScreen.kt.
struct HomeView: View {
    @State private var viewModel = DashboardViewModel()
    @State private var sheetCert: CertInfo? = nil
    @State private var showPaywall: Bool = false

    // 후속 phase 에서 lastCert/lastMode 가 통합되면 nil 대신 실제 값 주입.
    // 현재는 모두 nil → ContinueLastCard 미노출.
    private let lastCertLabel: String? = nil
    private let lastMode: LastSolveMode? = nil

    private var nickname: String {
        viewModel.member?.nickname ?? "학습자"
    }

    private var heroSubtitle: String {
        if viewModel.streak?.solvedToday == true {
            return "오늘 학습 완료. 내일도 같은 시간에 이어가면 좋아요."
        }
        return "짧게라도 한 세트를 풀고 연속 학습을 이어가세요."
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    AppHeroHeader(
                        eyebrow: "문어CBT",
                        title: "\(nickname)님, 오늘도 한 회차 풀어볼까요?",
                        subtitle: heroSubtitle
                    ) {
                        EmptyView()
                    }

                    VStack(alignment: .leading, spacing: Spacing.base) {
                        if let streak = viewModel.streak {
                            StreakCard(streak: streak)
                        }

                        if let label = lastCertLabel, let mode = lastMode {
                            ContinueLastCard(
                                lastCertLabel: label,
                                lastMode: mode,
                                onClick: {
                                    // 후속 phase: 마지막 모드 별 탭으로 selection 전환.
                                }
                            )
                        }

                        CertCarousel { info in
                            sheetCert = info
                        }

                        if let errorMessage = viewModel.errorMessage {
                            errorBanner(message: errorMessage)
                        }
                    }
                    .padding(.horizontal, Spacing.base)
                    .padding(.top, Spacing.lg)
                    .padding(.bottom, Spacing.xxl)
                }
            }
            .ignoresSafeArea(edges: .top)
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
