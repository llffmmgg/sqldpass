# Step 2 — Quota 사전 표시 + 미니 진입 경로 확인

## 배경

`GET /api/quota` 응답을 받아 모의고사·실전 문제 탭 진입 시 "오늘 0 / 30 문제" 표시. 활성 구독자(limit nil)는 숨김.

**iOS 탭 정리 확인**: 기존 iOS CustomTabBar 는 `홈 / 모의고사 / 기출복원 / 실전문제 / 내정보` 5개 탭. "미니모의고사"가 별도 탭이 없으나, **백엔드에 `/api/mock-exams/mini` 가 이미 존재**하고 `MockExamKind.MINI` 회차도 `/api/mock-exams/{id}` 같은 단일 엔드포인트로 진입. 즉 사용자가 미니에 진입하는 경로(예: 모의고사 탭 안 섹션, 홈 카드 등)가 있는지 확인하고, **없으면 모의고사 탭 안에 "미니" 섹션을 추가**해 미니 회차도 진입 가능하도록 한다.

미니 진입 경로 자체가 없으면 정책상 "미니+모의 합산 1회" 가 무의미 — 미니가 실행 안 되니까 모의만 1회로 동작. 그래도 정책 일관성 위해 진입 경로는 마련.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| 신규 `ios/Sqldpass/Services/QuotaService.swift` | GET /api/quota fetch |
| 신규 `ios/Sqldpass/Models/Quota.swift` | Decodable struct |
| 신규 `ios/Sqldpass/Core/DesignSystem/AppQuotaBadge.swift` | "오늘 18 / 30" 컴포넌트 |
| 수정 `ios/Sqldpass/Features/Solo/SoloSolveViewModel.swift` 또는 진입 뷰 | Quota fetch + 헤더에 AppQuotaBadge 마운트 |
| 수정 `ios/Sqldpass/Features/MockExams/MockExamsViewModel.swift` 또는 뷰 | 동일 |
| 검토 `ios/Sqldpass/Features/MockExams/*` | 미니 회차 진입 경로 확인. 없으면 섹션 추가 |

## Quota 모델 + 서비스

```swift
struct Quota: Decodable {
    let questionUsed: Int
    let questionLimit: Int?
    let mockUsed: Int
    let mockLimit: Int?
    let resetAt: String   // KST naive — UI 표시용
}

final class QuotaService {
    private let apiClient: APIClient
    init(apiClient: APIClient) { self.apiClient = apiClient }

    func fetchQuota() async throws -> Quota {
        try await apiClient.get("/api/quota")
    }
}
```

## AppQuotaBadge 컴포넌트

```swift
struct AppQuotaBadge: View {
    let kind: QuotaKind  // .question | .mock
    @State private var quota: Quota?

    var body: some View {
        if let q = quota, let limit = kind == .question ? q.questionLimit : q.mockLimit {
            let used = kind == .question ? q.questionUsed : q.mockUsed
            HStack { Text("오늘 \(used) / \(limit) \(kind.label)") }
                .appBodyStyle()
        } else {
            EmptyView()
        }
    }
}
```

탭 진입마다 `.task { quota = try? await quotaService.fetchQuota() }`.

## 미니 진입 경로 검토

`ios/Sqldpass/Features/MockExams/` 디렉토리 안 코드를 읽고:

- `MockExamRoutes.swift`, `MockExamsViewModel.swift` 가 미니(`/api/mock-exams/mini`) 를 호출하는지 확인
- 호출 없으면: MockExamsViewModel 에 `loadMini()` 추가 + 뷰에 "미니 모의고사" 섹션 추가 (정규 섹션과 분리)
- 호출 있으면: 현재 구조 유지

## 검증

iOS 시뮬레이터:
1. 무료 계정 로그인 → 모의고사 탭 진입 → "오늘 0 / 1 모의고사" 표시
2. 실전 문제 탭 진입 → "오늘 0 / 30 문제" 표시
3. 미니 회차 진입 가능 (UI 경로 있음)
4. 활성 구독 시드 → 모든 배지 숨김

## Acceptance Criteria

1. QuotaService, Quota, AppQuotaBadge 신규.
2. 모의고사 탭 + 실전 문제 탭 진입 헤더에 배지 노출.
3. 활성 구독자는 배지 숨김.
4. 미니 진입 경로 확인/추가 (있으면 유지, 없으면 모의고사 탭 내 섹션 추가).
5. iOS build 성공.

## 금지 사항

- 자체 카운터 만들지 마라. 이유: 서버 단일 진실 소스.
- CustomTabBar 의 탭 5개 구성을 바꾸지 마라. 이유: 최근 회귀 이력 + 정책상 미니 별도 탭은 불필요.
- 기출복원 화면에 배지 마운트 금지. 이유: 기출은 무제한, 배지가 항상 0/limit 표시되어 혼란.
- 색 계열 변경 금지(feedback_color_token_changes).

## Status 규칙

- 성공: step 2 `completed`. phase 완료.
- 실패: 3회 후 `error`.
