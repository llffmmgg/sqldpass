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
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundStyle(Color.brandPrimary)
                Text("프리미엄 이용 중")
                    .font(AppType.bodyEmph)
            }
            if let provider = viewModel.subscription?.provider {
                Text("결제 채널: \(provider)")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
            if let expiresAt = viewModel.subscription?.expiresAt {
                Text("만료: \(expiresAt)")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
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
            Text("자동갱신 안내")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            Text("구매 후 자동으로 갱신되며, 만료 24시간 전까지 취소할 수 있습니다.")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: Spacing.md) {
                Link("이용약관", destination: URL(string: "https://sqldpass.com/terms")!)
                Link("개인정보처리방침", destination: URL(string: "https://sqldpass.com/privacy")!)
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
