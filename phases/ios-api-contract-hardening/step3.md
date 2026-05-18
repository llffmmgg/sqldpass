# Step 3 - StoreKit And Environment

## Scope

- `ios/Sqldpass/Services/StoreKitService.swift`
- `ios/Sqldpass/Features/Paywall/PaywallViewModel.swift`
- `ios/Sqldpass/Features/Paywall/PaywallView.swift`
- `ios/Sqldpass/App/SqldpassApp.swift`
- `ios/Sqldpass/Core/Networking/APIEnvironment.swift`
- `ios/Sqldpass/Core/Auth/AuthStore.swift`

## Result

StoreKit transactions are finished only after backend verification succeeds. Purchase, restore, and `Transaction.updates` now share that policy.

`APIEnvironment` follows `ios/AGENTS.md`: Debug uses `http://localhost:8080`, Release uses `https://www.sqldpass.com`, and `SQLDPASS_BACKEND_URL` still overrides both.

`AuthStore` no longer prints keychain errors.

## Validation

- Static scan: no remaining `print(` under `ios/Sqldpass`.
- iOS runtime validation requires macOS/Xcode and a StoreKit configuration.
