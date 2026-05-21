# Step 1 — APIError 402 분기 + Paywall 모달

## 배경

iOS APIClient(`ios/Sqldpass/Core/Networking/APIClient.swift`) 는 현재 `clientError(status:message:)` 케이스로 4xx 를 통합 처리한다. 402 는 별도 분기가 없어 일반 clientError 로 떨어진다.

이번 step 에서:
1. `APIError` 에 `quotaExceeded(code:used:limit:resetAt:)` 케이스 추가
2. `APIClient.send` 가 402 응답을 받으면 body 디코딩 후 `quotaExceeded` throw
3. 호출 측 (SoloSolveViewModel, MockExamsViewModel 등) 에서 catch → SwiftUI sheet 로 PaywallSheet 또는 신규 QuotaPaywallView 노출

**카운팅 로직 없음.** 서버가 던지면 시트 띄우고 끝.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Core/Networking/APIError.swift` | `quotaExceeded` case 추가 |
| `ios/Sqldpass/Core/Networking/APIClient.swift` | send 안에서 402 분기, body 디코딩 후 throw |
| 신규 `ios/Sqldpass/Features/Paywall/QuotaPaywallView.swift` | 페이월 시트 (기존 PaywallViewModel 재활용 가능) |
| 호출 측 (Solve, MockExams 진입 ViewModel) | catch → `@Published var quotaPaywall: QuotaPaywallInfo?` 같은 상태로 시트 트리거 |

## APIError 수정 가이드

```swift
enum APIError: Error, LocalizedError, Equatable {
    // 기존 케이스들...
    case quotaExceeded(code: String, used: Int, limit: Int, resetAt: String)

    var errorDescription: String? {
        switch self {
        // 기존 분기...
        case .quotaExceeded(let code, _, _, _):
            return code == "DAILY_MOCK_LIMIT"
                ? "오늘 모의고사 1회 완료"
                : "오늘의 30문제 완주!"
        }
    }
}
```

## APIClient 수정 가이드

`send` 함수 안의 응답 처리 부분에서 402 분기:

```swift
if httpResponse.statusCode == 402 {
    struct QuotaBody: Decodable {
        let error: String
        let used: Int
        let limit: Int
        let resetAt: String
    }
    if let body = try? decoder.decode(QuotaBody.self, from: data) {
        throw APIError.quotaExceeded(
            code: body.error,
            used: body.used,
            limit: body.limit,
            resetAt: body.resetAt
        )
    }
    throw APIError.clientError(status: 402, message: nil)
}
```

기존 4xx 처리 위치에 추가. 기존 401 분기(tokenRefresher) 와 동급 우선순위.

## QuotaPaywallView 작성 가이드

```swift
struct QuotaPaywallInfo: Identifiable {
    let id = UUID()
    let code: String       // DAILY_QUESTION_LIMIT | DAILY_MOCK_LIMIT
    let used: Int
    let limit: Int
    let resetAt: String
}

struct QuotaPaywallView: View {
    let info: QuotaPaywallInfo
    let onClose: () -> Void
    let onPurchase: () -> Void

    var body: some View {
        VStack(spacing: Spacing.lg) {
            // 🐙 마스코트 (AppMascot)
            Text(title).appTitle()
            Text(body).appBody()
            AppButton("Focus 7일권 보기", action: onPurchase)
            Button("내일 다시 오기", action: onClose)
        }
    }

    var title: String {
        info.code == "DAILY_MOCK_LIMIT"
            ? "오늘 모의고사 1회 완료"
            : "오늘의 30문제 완주! 🐙"
    }
    var body: String {
        info.code == "DAILY_MOCK_LIMIT"
            ? "Focus 7일권으로 매일 풀 수 있어요."
            : "내일 다시 만나거나, Focus 7일권으로 끝까지 가볼까요?"
    }
}
```

기존 디자인 시스템(`AppButton`, `AppMascot`, `AppCard`, `Spacing.lg` 등) 활용.

## 호출 측 통합

`SoloSolveViewModel`, `MockExamsViewModel` 등에서:

```swift
@Published var quotaPaywall: QuotaPaywallInfo?

func loadQuestions() async {
    do {
        // 기존 호출
    } catch APIError.quotaExceeded(let code, let used, let limit, let resetAt) {
        await MainActor.run {
            self.quotaPaywall = QuotaPaywallInfo(code: code, used: used, limit: limit, resetAt: resetAt)
        }
    } catch {
        // 기존 에러 처리
    }
}
```

뷰에서 `.sheet(item: $viewModel.quotaPaywall) { QuotaPaywallView(info: $0, ...) }`.

## 검증

iOS 시뮬레이터로:
1. 무료 시드 계정 로그인 → 31번째 문제 풀이 → QuotaPaywallView 표시
2. 모의고사 2회 진입 → 표시
3. 기출복원 다회 진입 → 표시 안 됨
4. 활성 구독 시드 → 모든 호출 무제한

**CustomTabBar 회귀 주의** — 메모리 commit `f084a00`(MockExamDetail 시험시작 버튼 가림), `c5f160f`(풀이 화면 진입 시 CustomTabBar 숨김) 처럼 시트 표시 시 탭바가 잘못 노출되지 않도록.

Xcode build 통과 확인:
```bash
cd ios
./build  # 또는 scripts/ios-build.sh
```

## Acceptance Criteria

1. APIError.quotaExceeded 케이스 추가.
2. APIClient.send 402 분기 추가, body 디코딩 후 throw.
3. QuotaPaywallView 신규.
4. 최소 SoloSolveViewModel(문제) + MockExamsViewModel(모의) 두 곳에서 catch 처리.
5. iOS build 성공.

## 금지 사항

- 자체 카운터 (`@AppStorage("dailyCount")` 같은) 만들지 마라. 이유: 서버 단일 진실 소스.
- backdrop blur, opacity pulse 사용 금지. 이유: feedback_no_ai_blur_effects.
- CustomTabBar 구조를 변경하지 마라. 이유: 최근 회귀 이력. 이번 작업은 시트 표시만.
- 기출복원 화면에서 catch 핸들러 추가하지 마라. 이유: 기출은 정책상 무제한, 서버가 402 던지지 않음.

## Status 규칙

- 성공: step 1 `completed`.
- 실패: 3회 후 `error`.
