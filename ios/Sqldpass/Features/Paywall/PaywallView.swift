import SwiftUI

/// 결제 페이지 — 웹 `CheckoutLanding` 과 1:1 카피·구조 동치.
///
/// 레이아웃: 헤더 → (활성 구독 배지, 있을 때만) → 가로 스와이프 carousel 4장 →
/// 복원 버튼 → 안내/약관.
///
/// 카드 카피(이름·pitch·features)는 본 파일의 `paywallTiers` 상수에서 직접 관리.
/// App Store Connect 의 `displayName`/`description` 은 화면에서 사용하지 않는다 —
/// 카피 동기화에 Apple 메타데이터 리뷰가 끼지 않도록.
///
/// 가격은 `PaymentProduct.displayPriceKRW` 로 항상 한국 자연체 ("9,900원") 표기 강제.
/// 시뮬레이터 region 이 KR 이 아니어도 동일.
struct PaywallView: View {
    @State private var viewModel = PaywallViewModel()
    @State private var selectedKey: PaywallPlanKey = .oneMonth
    @Environment(SubscriptionStore.self) private var subscriptionStore

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                header
                activeBadge
                planCarousel
                restoreButton
                disclaimerSection
                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                        .padding(.horizontal, Spacing.base)
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle("프리미엄")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            // Paywall 진입 시 활성 구독 상태는 항상 최신화 — 닫았다 다시 열었을 때
            // 만료 직전 / 다른 디바이스에서 결제한 상태 등이 반영되도록.
            await subscriptionStore.refresh()
            if viewModel.products.isEmpty {
                await viewModel.load()
            }
        }
        .overlay {
            if viewModel.isLoading && viewModel.products.isEmpty {
                ProgressView().controlSize(.large)
            }
        }
        .alert(
            "구매 완료",
            isPresented: Binding(
                get: { viewModel.purchaseSuccess },
                set: { if !$0 { viewModel.dismissPurchaseSuccess() } }
            ),
            actions: {
                Button("확인", role: .cancel) { viewModel.dismissPurchaseSuccess() }
            },
            message: {
                Text("프리미엄 기능을 사용할 수 있습니다.")
            }
        )
    }

    // MARK: - 헤더

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("플랜을 선택하세요")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("필요한 기간만큼 이용하고, 고난이도 모의고사까지 바로 풀어보세요.")
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// 활성 구독이 있을 때 헤더 아래에 노출되는 배지 — 웹 CheckoutLanding 의 success pill 과 동치.
    /// 비활성/로딩 중에는 EmptyView (ViewBuilder 분기).
    @ViewBuilder
    private var activeBadge: some View {
        if let sub = subscriptionStore.info, sub.active, let plan = sub.plan {
            HStack(spacing: Spacing.xs) {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundStyle(Color.brandPrimary)
                Text("현재 \(planLabel(plan)) 이용 중")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.brandPrimary)
                if let expiresAt = sub.expiresAt {
                    Text("· \(shortDate(expiresAt)) 만료")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.xs)
            .background(Color.brandPrimary.opacity(0.10))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.full)
                    .stroke(Color.brandPrimary.opacity(0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.full))
        }
    }

    // MARK: - Carousel

    private var planCarousel: some View {
        TabView(selection: $selectedKey) {
            ForEach(paywallTiers) { tier in
                PlanCard(
                    tier: tier,
                    product: product(for: tier.key),
                    state: cardState(for: tier),
                    isPurchasing: viewModel.isPurchasing,
                    onTap: {
                        if let p = product(for: tier.key) {
                            Task { await viewModel.purchase(p) }
                        }
                    }
                )
                .padding(.horizontal, Spacing.xxs)
                .tag(tier.key)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .always))
        .indexViewStyle(.page(backgroundDisplayMode: .always))
        .frame(height: 540)
    }

    private var restoreButton: some View {
        HStack {
            Spacer()
            Button {
                Task { await viewModel.restore() }
            } label: {
                Label("구매 복원", systemImage: "arrow.clockwise")
                    .font(AppType.footnote.weight(.semibold))
            }
            .buttonStyle(.plain)
            .foregroundStyle(Color.semanticInfo)
            .disabled(viewModel.isPurchasing)
            Spacer()
        }
    }

    private var disclaimerSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("결제 안내")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            Text("이 상품은 자동으로 갱신되지 않습니다. 표시된 기간이 만료되면 다시 구매해야 이용을 이어갈 수 있습니다.")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: Spacing.md) {
                Link("이용약관", destination: URL(string: "https://www.sqldpass.com/terms")!)
                Link("환불 정책", destination: URL(string: "https://www.sqldpass.com/refund")!)
                Link("개인정보처리방침", destination: URL(string: "https://www.sqldpass.com/privacy")!)
            }
            .font(AppType.caption)
            .foregroundStyle(Color.semanticInfo)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - 카드 상태 결정

    private func cardState(for tier: PaywallTier) -> PlanCardState {
        guard let sub = subscriptionStore.info, sub.active, let activePlan = sub.plan else {
            return .available
        }
        if planKey(for: activePlan) == tier.key {
            return .current
        }
        // 활성 plan 이 있으면 업그레이드 가능한 카드만 활성. 그 외는 만료 후 가능.
        if isUpgrade(from: activePlan, to: tier.key) {
            return .upgrade
        }
        return .blocked
    }

    private func product(for key: PaywallPlanKey) -> PaymentProduct? {
        viewModel.products.first(where: { $0.id == key.rawValue })
    }

    /// 백엔드 plan 문자열(SubscriptionPlan enum name) 을 carousel key 로 매핑.
    private func planKey(for plan: String) -> PaywallPlanKey? {
        switch plan {
        case "THREE_DAY": return .threeDay
        case "FOCUS":     return .focus
        case "ONE_MONTH": return .oneMonth
        case "UNLIMITED": return .unlimited
        default:          return nil
        }
    }

    /// SubscriptionPlan.isUpgradeFrom 매트릭스 (백엔드와 동일).
    private func isUpgrade(from current: String, to target: PaywallPlanKey) -> Bool {
        switch current {
        case "THREE_DAY": return target == .oneMonth || target == .unlimited
        case "FOCUS":     return target == .oneMonth || target == .unlimited
        case "ONE_MONTH": return target == .unlimited
        case "UNLIMITED": return false
        default:          return false
        }
    }

    private func planLabel(_ plan: String) -> String {
        switch plan {
        case "THREE_DAY": return "Thunder"
        case "FOCUS":     return "Focus"
        case "ONE_MONTH": return "Pro"
        case "UNLIMITED": return "All Pass"
        default:          return plan
        }
    }

    private func shortDate(_ iso: String) -> String {
        // 백엔드는 KST naive ISO (예: "2026-06-19T00:00:00") 또는 일자만 보낼 수 있다.
        // 첫 10자 (YYYY-MM-DD) 만 잘라 노출.
        String(iso.prefix(10))
    }
}

// MARK: - Tier 정적 카탈로그 (웹 CheckoutLanding 의 TIERS 와 동치)

enum PaywallPlanKey: String, CaseIterable, Hashable {
    case threeDay  = "iap_thunder"
    case focus     = "iap_focus"
    case oneMonth  = "iap_one_month"
    case unlimited = "iap_unlimited"
}

struct PaywallTier: Identifiable {
    let key: PaywallPlanKey
    let name: String       // "Thunder" / "Focus" / "Pro" / "All Pass"
    let pitch: String      // 가치 제안 한 단락
    let unit: String       // "/3일" 같은 단위 라벨
    let features: [String] // 체크 리스트
    let ctaLabel: String   // "Pro 시작" 등
    let highlight: Bool    // Pro 만 true

    var id: String { key.rawValue }
}

private let paywallTiers: [PaywallTier] = [
    PaywallTier(
        key: .threeDay, name: "Thunder",
        pitch: "시험 임박 3일, PASS+ 회차까지 벼락치기로 마무리하세요.",
        unit: "3일",
        features: ["PASS+ 회차 풀이", "72시간 풀 액세스", "광고 제거", "오답노트 사용", "즐겨찾기 무제한"],
        ctaLabel: "Thunder 시작", highlight: false
    ),
    PaywallTier(
        key: .focus, name: "Focus",
        pitch: "광고를 제거하고 오답노트로 한 달 동안 집중 공부하세요.",
        unit: "30일",
        features: ["광고 제거", "오답노트 사용", "즐겨찾기 무제한"],
        ctaLabel: "Focus 시작", highlight: false
    ),
    PaywallTier(
        key: .oneMonth, name: "Pro",
        pitch: "한 달 동안 PASS+ 회차와 모든 학습 도구를 무제한으로 사용해보세요.",
        unit: "30일",
        features: ["PASS+ 회차 무제한", "30일 풀 액세스", "광고 제거", "오답노트 사용", "즐겨찾기 무제한"],
        ctaLabel: "Pro 시작", highlight: true
    ),
    PaywallTier(
        key: .unlimited, name: "All Pass",
        pitch: "6개월 동안 새 회차와 모의고사 PDF 다운로드까지 무제한으로 사용하세요.",
        unit: "180일",
        features: ["PASS+ 회차 무제한", "6개월 풀 액세스", "광고 제거", "오답노트 사용", "즐겨찾기 무제한", "PDF 다운로드"],
        ctaLabel: "All Pass 시작", highlight: false
    )
]

// MARK: - PlanCard

enum PlanCardState {
    case available   // 활성 구독 없음 또는 신규 결제 가능
    case current     // 사용자가 이 plan 을 이미 이용 중
    case upgrade     // 활성 구독에서 업그레이드 가능
    case blocked     // 다운그레이드/UNLIMITED 활성 — 만료 후 가능
}

private struct PlanCard: View {
    let tier: PaywallTier
    let product: PaymentProduct?
    let state: PlanCardState
    let isPurchasing: Bool
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            cardHeader
            priceBlock
            Text(tier.pitch)
                .font(AppType.footnote)
                .foregroundStyle(tier.highlight ? Color.appTextPrimary : Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            ctaButton
            Divider()
                .background(Color.appBorder.opacity(0.6))
            featureList
            Spacer(minLength: 0)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(cardBackground)
        .overlay(cardBorder)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var cardHeader: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(tier.name)
                .font(AppType.title.weight(.bold))
                .foregroundStyle(tier.highlight ? Color.brandPrimary : Color.appTextPrimary)
            if tier.highlight {
                Text("가장 인기")
                    .font(AppType.caption.weight(.bold))
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .background(Color.brandPrimary.opacity(0.15))
                    .foregroundStyle(Color.brandPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.full))
            }
            Spacer()
        }
    }

    private var priceBlock: some View {
        HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
            Text(product?.displayPriceKRW ?? "—")
                .font(AppType.monoNumericLarge.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("/ \(tier.unit)")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextSubtle)
        }
    }

    @ViewBuilder
    private var ctaButton: some View {
        switch state {
        case .current:
            disabledLabel("✓ 이용 중", tone: .success)
        case .blocked:
            disabledLabel("현재 구독 종료 후 가능", tone: .muted)
        case .upgrade:
            primaryButton("업그레이드")
        case .available:
            primaryButton(tier.ctaLabel)
        }
    }

    private func primaryButton(_ title: String) -> some View {
        Button(action: onTap) {
            HStack {
                Spacer()
                if isPurchasing {
                    ProgressView()
                        .controlSize(.small)
                        .tint(Color.brandPrimaryFG)
                } else {
                    Text(title)
                        .font(AppType.bodyEmph)
                }
                Spacer()
            }
            .padding(.vertical, Spacing.md)
            .background(tier.highlight ? Color.brandPrimary : Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.sm)
                    .stroke(tier.highlight ? Color.clear : Color.appBorder, lineWidth: 1)
            )
            .foregroundStyle(tier.highlight ? Color.brandPrimaryFG : Color.appTextPrimary)
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
        }
        .buttonStyle(.plain)
        .disabled(isPurchasing || product == nil)
        .opacity((isPurchasing || product == nil) ? 0.6 : 1)
    }

    private enum LabelTone { case success, muted }

    private func disabledLabel(_ title: String, tone: LabelTone) -> some View {
        HStack {
            Spacer()
            Text(title)
                .font(AppType.bodyEmph)
                .foregroundStyle(tone == .success ? Color.brandPrimary : Color.appTextMuted)
            Spacer()
        }
        .padding(.vertical, Spacing.md)
        .background(tone == .success ? Color.brandPrimary.opacity(0.08) : Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.sm)
                .stroke(tone == .success ? Color.brandPrimary.opacity(0.4) : Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
    }

    private var featureList: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            ForEach(tier.features, id: \.self) { feature in
                HStack(alignment: .top, spacing: Spacing.xs) {
                    Image(systemName: "checkmark")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(tier.highlight ? Color.brandPrimary : Color.appTextSubtle)
                        .padding(.top, 2)
                    Text(feature)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextPrimary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var cardBackground: Color {
        tier.highlight ? Color.brandPrimary.opacity(0.05) : Color.appSurface
    }

    @ViewBuilder
    private var cardBorder: some View {
        RoundedRectangle(cornerRadius: Radius.lg)
            .stroke(
                tier.highlight ? Color.brandPrimary.opacity(0.5) : Color.appBorder,
                lineWidth: tier.highlight ? 1.5 : 1
            )
    }
}

#Preview {
    NavigationStack {
        PaywallView()
            .environment(SubscriptionStore.shared)
    }
}
