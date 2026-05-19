# iOS Release

This app is intentionally wired to stop before TestFlight upload by default.

> 🆕 **최초 Secret 셋업은 [`RELEASE_SETUP.md`](RELEASE_SETUP.md) 참조** (인증서·프로파일·API Key 발급·base64 변환·등록 단계별 가이드).

## Pre-release Remediation Snapshot

Most recent remediation pass (see `~/.claude/plans/you-are-a-senior-rippling-pony.md`) addressed:

- **Contract fixes** — bookmarks list/exists/add, solves history, payment verify, subjects tree all aligned to backend response shapes.
- **Past-exam runner** — replaced placeholder tab with full runner + grade flow (`GET /api/public/past-exams/{id}`, `POST /grade`).
- **History detail** — taps now fetch `GET /api/solves/{id}` and (for mock attempts) `GET /api/mock-exams/{id}` for question stems, so review rows are populated.
- **Mock-exam submit** — emits `clientSubmissionId` for idempotency and falls back to SwiftData offline queue on network failure.
- **Paywall** — disclaimer rewritten to describe non-renewing time-limited access (matches `Sqldpass.storekit` declaration); active subscription card shows real plan + entitlement matrix + Apple Subscriptions deep link.
- **Profile** — `SubscriptionBadge` reflects real state; 결제·청구 row shows only when active (links to Apple subscriptions); 테마/알림 placeholder rows removed; 피드백 row opens in-app `POST /api/feedback` compose sheet.
- **Solo solve report (신고)** — wired to the same compose sheet, pre-filled with `questionId` and `QUESTION_ERROR` type.
- **PrivacyInfo.xcprivacy** — added with conservative declarations (User ID, Name, Product Interaction, Purchase History — all linked, none used for tracking).
- **Info.plist / project.yml** — `ITSAppUsesNonExemptEncryption=false` added.
- **Dead code** — removed unreachable `DashboardView`/`InsightsView`; the underlying viewmodel was renamed to `HomeViewModel` and moved into `Features/Home/`.

## After pulling this branch

Run xcodegen so the regenerated pbxproj picks up new files (`Features/Feedback/*`, `Features/PastExams/PastExamRunner*`, `Features/PastExams/PastExamResultView`, `Features/History/HistoryDetailView`, `Models/PastExamDetail`, `Models/PastExamGrade`, `Services/FeedbackService`, `Features/Home/HomeViewModel`, `PrivacyInfo.xcprivacy`) and drops the deleted `Features/Dashboard/` and `Features/Insights/` folders:

```bash
cd ios
bash scripts/bootstrap_ci.sh
bash scripts/build_debug.sh
```

Then re-run on a real device:

1. Sign in via Apple → land on Home with nickname visible.
2. Mock exam tab → pick exam → answer all → submit (try airplane mode mid-submit to verify the offline queue) → result with stems.
3. History tab → tap a row → review rows now render real question stems for mock attempts.
4. 기출복원 tab → pick a SQLD round → answer 5 → submit → pass/fail banner + subject breakdown + per-question review.
5. 실전 문제 tab → six 자격증 chips visible (SQLD / 정처기 실기·필기 / 컴활 1·2 / ADsP) → tap subject → 10 questions appear.
6. Bookmarks → list renders rich rows with subject + content preview; swipe-delete persists; ★ in Solo solve stays toggled across reload.
7. Paywall → sandbox purchase → "구매 완료" alert → restart app → entitlement card with plan + Apple subscriptions link.
8. Profile → 피드백 → fill text → submit → backend admin receives the row.

## GitHub Actions

`iOS CI` runs on PR and main pushes for `ios/**` changes:

```bash
bash ios/scripts/bootstrap_ci.sh
bash ios/scripts/build_debug.sh
```

`iOS Release` is manual (`workflow_dispatch`). It imports signing assets, archives the Release app, exports an IPA, and uploads the IPA as a workflow artifact.

The `upload_to_testflight` input defaults to `false`. Leave it false until the IPA has been checked on macOS/Xcode.

## Required Secrets

- `APPLE_TEAM_ID`
- `IOS_CERTIFICATE_P12_BASE64`
- `IOS_CERTIFICATE_PASSWORD`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `IOS_PROVISIONING_PROFILE_NAME`
- `IOS_KEYCHAIN_PASSWORD`

For TestFlight upload:

- `APP_STORE_CONNECT_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`

## Optional Variables

- `IOS_BUNDLE_ID`, default `com.sqldpass.app`
- `IOS_EXPORT_METHOD`, default `app-store-connect`

If Xcode rejects `app-store-connect` for `method`, set `IOS_EXPORT_METHOD` to `app-store` in repository variables.

## Local macOS Check

From `ios/`:

```bash
bash scripts/bootstrap_ci.sh
bash scripts/build_debug.sh
```

To export an IPA locally, provide the same environment variables used by GitHub Actions and run:

```bash
bash scripts/archive_app_store.sh
```

To upload after checking the IPA:

```bash
bash scripts/upload_testflight.sh build/export/*.ipa
```

## Known Gaps / Manual Follow-ups

- **`Sqldpass.storekit` placeholders** — `_developerTeamID: "TEAMIDXXXX"` and `_applicationInternalID: "1234567890"` are simulator-only; update before wiring App Store Connect.
- **Backend Apple webhooks (ASSN v2)** — `RTDN/ASSN` handlers are stubbed (project memory `project_payment_webhooks_stubbed`). Refund / expiry / cancel won't propagate without backend work.
- **Past-exam grade idempotency** — `PastExamGradeRequest` has no `clientSubmissionId` on backend; retry can produce duplicate Solve rows. Tracking limitation, not blocking.
- **Backend `LocalDateTime`** is serialized KST-naive (project memory `project_kst_naive_serialization`); iOS treats these fields as `String?` and renders them verbatim. Adding a proper `DateFormatter` localized pass is future work.
- **Subjects DTO mismatch with Android** — backend `/api/subjects` is tree-only. iOS now consumes the tree natively. Android (`mobile/`) still expects a flat list per `ApiModels.kt` — out of scope of this iOS pass.
- **No XCTest target** — only `xcodebuild build` validates compilation. Behavioral verification is manual on Simulator/device.
