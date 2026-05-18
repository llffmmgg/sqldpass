# Step 4 — iOS StoreKitService + PaymentService + Models

## Background

iOS 클라이언트의 결제 인프라. StoreKit 2 의 async/await API 활용. Step 5 의 PaywallView 가 본 step 의 Service 메서드를 호출.

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Models/PaymentProduct.swift` | 신규 — Product wrapper |
| `ios/Sqldpass/Services/StoreKitService.swift` | 신규 — StoreKit 2 API 래퍼 |
| `ios/Sqldpass/Services/PaymentService.swift` | 신규 — 백엔드 결제 검증/구독 조회 |

## Implementation

### `Models/PaymentProduct.swift`

```swift
import Foundation
import StoreKit

/// StoreKit 2 Product 의 View-friendly wrapper.
struct PaymentProduct: Identifiable, Equatable {
    let id: String              // productId (예: iap_one_month)
    let displayName: String
    let description: String
    let displayPrice: String    // 로케일 통화 포함 (예: ₩9,900)
    let underlying: Product

    init(_ product: Product) {
        self.id = product.id
        self.displayName = product.displayName
        self.description = product.description
        self.displayPrice = product.displayPrice
        self.underlying = product
    }

    static func == (lhs: PaymentProduct, rhs: PaymentProduct) -> Bool {
        lhs.id == rhs.id
    }
}
```

### `Services/StoreKitService.swift`

```swift
import Foundation
import StoreKit

/// StoreKit 2 통합. PaywallViewModel 이 호출.
///
/// - `loadProducts(ids:)` — App Store Connect 에 등록된 상품 메타 조회
/// - `purchase(_:)` — 구매 시작 → JWS 영수증 반환
/// - `restore()` — 기기에 활성 구독 있는지 동기화
/// - `Transaction.updates` 감시 → 자동 갱신 등 백그라운드 transaction
final class StoreKitService {
    static let shared = StoreKitService()

    private var updateTask: Task<Void, Never>?

    /// 백엔드 등록된 productId 목록. 실제 App Store Connect SKU 와 일치해야 함.
    static let knownProductIds: [String] = [
        "iap_three_day",
        "iap_focus",
        "iap_one_month",
        "iap_unlimited"
    ]

    private init() {}

    func startListening(onNewTransaction: @escaping @Sendable (Transaction) async -> Void) {
        updateTask?.cancel()
        updateTask = Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await onNewTransaction(transaction)
                    await transaction.finish()
                }
            }
        }
    }

    func stopListening() {
        updateTask?.cancel()
        updateTask = nil
    }

    /// App Store Connect 의 상품 메타 조회.
    func loadProducts(ids: [String] = StoreKitService.knownProductIds) async throws -> [PaymentProduct] {
        let products = try await Product.products(for: ids)
        return products
            .sorted { $0.price < $1.price }
            .map(PaymentProduct.init)
    }

    /// 구매. 성공 시 JWS signedTransaction 문자열 반환.
    enum PurchaseResult {
        case success(jws: String, productId: String, transactionId: UInt64)
        case userCancelled
        case pending
    }

    func purchase(_ product: PaymentProduct) async throws -> PurchaseResult {
        let result = try await product.underlying.purchase()
        switch result {
        case .success(let verification):
            switch verification {
            case .verified(let transaction):
                let jws = verification.jwsRepresentation
                let res = PurchaseResult.success(
                    jws: jws,
                    productId: transaction.productID,
                    transactionId: transaction.id
                )
                await transaction.finish()
                return res
            case .unverified(_, let error):
                throw error
            }
        case .userCancelled:
            return .userCancelled
        case .pending:
            return .pending
        @unknown default:
            return .pending
        }
    }

    /// 기존 구매 복원 — 사용자가 다른 기기에서 구매했거나 앱 재설치 시.
    func restore() async throws {
        try await AppStore.sync()
    }

    /// 현재 활성 entitlement 의 JWS 반환 (있으면). 백엔드 재확인용.
    func currentEntitlementJWS() async -> String? {
        for await result in Transaction.currentEntitlements {
            if case .verified = result {
                return result.jwsRepresentation
            }
        }
        return nil
    }
}
```

### `Services/PaymentService.swift`

```swift
import Foundation

enum PaymentService {
    /// GET /api/payment/subscription — 통합 entitlement 확인 (다른 플랫폼 결제 포함)
    static func subscription() async throws -> SubscriptionInfo {
        try await APIClient.shared.get("/api/payment/subscription")
    }

    /// POST /api/payment/apple/verify — 영수증 검증 + Subscription 발급
    struct AppleVerifyRequest: Encodable {
        let signedTransaction: String
        let productId: String
    }

    struct VerifyResult: Decodable {
        let paymentId: Int64
        let status: String
    }

    static func verifyApple(signedTransaction: String, productId: String) async throws -> VerifyResult {
        try await APIClient.shared.post(
            "/api/payment/apple/verify",
            body: AppleVerifyRequest(signedTransaction: signedTransaction, productId: productId)
        )
    }
}
```

## Validation

### 빌드 검증

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -5
```

기대: `** BUILD SUCCEEDED **`

### 정합성

- `Models/PaymentProduct.swift`, `Services/StoreKitService.swift`, `Services/PaymentService.swift` 가 생성됐는지.
- `import StoreKit` 정상 인식 (iOS 17.0+ 시스템 framework).

## 금지사항

- `unverified` 케이스를 그냥 success 처리하지 마라. 이유: StoreKit 2 의 verification 결과를 무시하면 위조 영수증 통과 가능. throw 로 호출자에게 위임.
- `Transaction.updates` listener 를 ViewModel 안에 두지 마라. 이유: ViewModel 라이프사이클은 화면 단위라 백그라운드 transaction 누락 위험. App 시작 시 한 번 startListening 호출하는 게 표준.
- 백엔드 호출 실패 시 finish() 호출 금지. 이유: finish 안 하면 다음 앱 시작 시 Transaction.unfinished 로 재시도 가능. 호출자가 명시적으로 처리.
