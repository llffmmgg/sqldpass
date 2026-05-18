# Step 5 — Paywall View + Profile 라우팅

## Background

Step 4 의 StoreKitService + PaymentService 위에 페이월 UI. iOS HIG + App Store Guideline 3.1.2 의 자동갱신 약관/가격/주기/EULA/개인정보 링크 명시 노출.

ProfileView 의 "프리미엄 보기" Label 을 PaywallView NavigationLink 로 교체.

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Paywall/PaywallViewModel.swift` | 신규 — 상품 로딩 + 구매 + 검증 |
| `ios/Sqldpass/Features/Paywall/PaywallView.swift` | 신규 — UI |
| `ios/Sqldpass/Features/Profile/ProfileView.swift` | "프리미엄 보기" placeholder → PaywallView NavigationLink |

## Implementation

### `PaywallViewModel.swift`

```swift
import Foundation
import Observation
import StoreKit

@Observable
final class PaywallViewModel {
    private(set) var products: [PaymentProduct] = []
    private(set) var subscription: SubscriptionInfo?
    private(set) var isLoading = false
    private(set) var isPurchasing = false
    private(set) var errorMessage: String?
    private(set) var purchaseSuccess = false

    var hasActiveSubscription: Bool {
        subscription?.active ?? false
    }

    @MainActor
    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let productsTask = StoreKitService.shared.loadProducts()
            async let subscriptionTask = PaymentService.subscription()
            let (p, s) = try await (productsTask, subscriptionTask)
            products = p
            subscription = s
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    @MainActor
    func purchase(_ product: PaymentProduct) async {
        guard !isPurchasing else { return }
        isPurchasing = true
        errorMessage = nil
        do {
            let result = try await StoreKitService.shared.purchase(product)
            switch result {
            case .success(let jws, let productId, _):
                _ = try await PaymentService.verifyApple(
                    signedTransaction: jws,
                    productId: productId
                )
                purchaseSuccess = true
                // 통합 entitlement 다시 확인
                subscription = try? await PaymentService.subscription()
            case .userCancelled:
                break
            case .pending:
                errorMessage = "결제 승인 대기 중입니다."
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        isPurchasing = false
    }

    @MainActor
    func restore() async {
        isPurchasing = true
        errorMessage = nil
        do {
            try await StoreKitService.shared.restore()
            subscription = try? await PaymentService.subscription()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        isPurchasing = false
    }
}
```

### `PaywallView.swift`

```swift
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
        .alert("구매 완료", isPresented: .constant(viewModel.purchaseSuccess), actions: {
            Button("확인", role: .cancel) {}
        }, message: {
            Text("프리미엄 기능을 모두 이용할 수 있어요.")
        })
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Image(systemName: "crown.fill")
                .font(.system(size: 44))
                .foregroundStyle(Color.semanticWarning)
            Text("문어CBT 프리미엄")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("광고 없이 모든 기능을 마음껏 이용하세요")
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
                Text("이미 프리미엄 이용 중")
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
            Text("구매 후 자동으로 갱신되며, 만료 24시간 전까지 취소 가능합니다. 갱신 가격은 변경될 수 있으며 변경 시 사전 안내됩니다.")
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
```

### `ProfileView.swift` 수정

기존 "구독" 섹션의 placeholder Label 교체:

```swift
Section("구독") {
    // 결제 화면은 Phase 4 별도 작업 — placeholder Label
    Label("프리미엄 보기", systemImage: "crown")
        .foregroundStyle(Color.appTextSubtle)
}
```

→

```swift
Section("구독") {
    NavigationLink {
        PaywallView()
    } label: {
        Label("프리미엄 보기", systemImage: "crown")
    }
}
```

## Validation

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -5
```

기대: `** BUILD SUCCEEDED **`

## 금지사항

- PaywallView 안에서 직접 `Product.products(for:)` 호출 금지. 이유: View 는 ViewModel 의 상태만 표시. 비동기 fetch 는 ViewModel 책임.
- 활성 구독 상태일 때 구매 버튼을 회색 비활성으로만 보이게 하지 마라. 본 step 은 아예 `productsSection` 자체를 안 그리고 `activeStateCard` 만 보여 사용자 혼란 방지.
- "복원" 버튼을 페이월 본문 안에 큰 액션으로 두지 마라. 이유: 매번 신규 구매를 유도해야 함. 복원은 toolbar 또는 작은 보조 액션.
- 약관/개인정보 링크를 페이월에서 누락 금지. 이유: App Store Guideline 3.1.2 위반 → 심사 거부 (출시 단계에서 봐도 본 phase 에서 미리 넣어둬야 후속 작업 안 발생).
