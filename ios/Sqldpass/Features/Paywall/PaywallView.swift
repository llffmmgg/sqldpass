import SwiftUI

struct PaywallView: View {
    @State private var viewModel = PaywallViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                header

                if viewModel.hasActiveSubscription {
                    activeStateCard
                } else {
                    productsSection
                }

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
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("복원") {
                    Task { await viewModel.restore() }
                }
                .disabled(viewModel.isPurchasing)
            }
        }
        .task {
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

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Image(systemName: "crown.fill")
                .font(.system(size: 44))
                .foregroundStyle(Color.semanticWarning)
            Text("문어CBT 프리미엄")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("광고 없이 모든 기능을 이용하세요.")
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var activeStateCard: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundStyle(Color.brandPrimary)
                Text("프리미엄 이용 중")
                    .font(AppType.bodyEmph)
                if let plan = viewModel.subscription?.plan, !plan.isEmpty {
                    Text(plan)
                        .font(AppType.caption.weight(.semibold))
                        .padding(.horizontal, Spacing.sm)
                        .padding(.vertical, Spacing.xxs)
                        .background(Color.brandPrimary.opacity(0.12))
                        .foregroundStyle(Color.brandPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: Radius.full))
                }
            }
            if let expiresAt = viewModel.subscription?.expiresAt {
                Text("만료: \(expiresAt)")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
            entitlementsList
            // 백엔드는 자동갱신 구독이 아닌 비갱신(기간 만료) 모델로 통일. 사용자가 만료 시 다시
            // 구매해야 한다는 점을 명시. Apple Subscriptions 페이지 링크는 자동갱신 상품이 도입되면
            // 가치 있는 안내가 되므로 향후 활성화. 현재는 결제 채널 별도 관리 페이지가 없다.
            Link(destination: URL(string: "https://apps.apple.com/account/subscriptions")!) {
                Label("Apple 구독 관리", systemImage: "arrow.up.right.square")
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(Color.semanticInfo)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.brandPrimary.opacity(0.1))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.brandPrimary.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    @ViewBuilder
    private var entitlementsList: some View {
        if let sub = viewModel.subscription {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                if sub.removesAds == true {
                    entitlementRow("광고 제거")
                }
                if sub.allowsPremium == true {
                    entitlementRow("프리미엄 모의고사 잠금 해제")
                }
                if sub.allowsPdf == true {
                    entitlementRow("PDF 다운로드")
                }
                if sub.hasLibraryAccess == true {
                    entitlementRow("라이브러리 전체 이용")
                }
            }
        }
    }

    private func entitlementRow(_ text: String) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Color.brandPrimary)
            Text(text)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextPrimary)
        }
    }

    private var productsSection: some View {
        VStack(spacing: Spacing.md) {
            ForEach(viewModel.products) { product in
                ProductCard(
                    product: product,
                    isPurchasing: viewModel.isPurchasing,
                    onTap: {
                        Task { await viewModel.purchase(product) }
                    }
                )
            }
        }
    }

    private var disclaimerSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("결제 안내")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            // 백엔드/스토어 모두 비갱신(non-renewing) 모델. 자동갱신 카피를 쓰지 않도록 주의.
            Text("이 상품은 자동으로 갱신되지 않습니다. 표시된 기간이 만료되면 다시 구매해야 이용을 이어갈 수 있습니다.")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: Spacing.md) {
                Link("이용약관", destination: URL(string: "https://www.sqldpass.com/terms")!)
                Link("개인정보처리방침", destination: URL(string: "https://www.sqldpass.com/privacy")!)
            }
            .font(AppType.caption)
            .foregroundStyle(Color.semanticInfo)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ProductCard: View {
    let product: PaymentProduct
    let isPurchasing: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: Spacing.md) {
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(product.displayName)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text(product.description)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                        .lineLimit(2)
                }
                Spacer()
                Text(product.displayPrice)
                    .font(AppType.monoNumericLarge.weight(.bold))
                    .foregroundStyle(Color.brandPrimary)
            }
            .padding(Spacing.base)
            .frame(maxWidth: .infinity)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
        .buttonStyle(.plain)
        .disabled(isPurchasing)
    }
}

#Preview {
    NavigationStack {
        PaywallView()
    }
}
