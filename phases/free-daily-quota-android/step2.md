# Step 2 — Quota API + 탭 헤더 표시 + 미니 진입 검토

## 배경

`GET /api/quota` 호출로 사전 표시. MockExamTab + SolveTab 진입 시 "오늘 18 / 30" 표시. 활성 구독자(limit null)는 숨김.

**미니 진입 경로**: Android 탭은 SqldpassNav.kt 기준 확인. 미니 회차 진입이 가능한 경로가 없으면 MockExamTab 안에 섹션 추가.

## 작업 디렉터리

```
mobile/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `mobile/app/src/main/java/com/sqldpass/app/data/remote/SqldpassApi.kt` | `getQuota()` 메서드 추가 |
| `mobile/app/src/main/java/com/sqldpass/app/data/ApiModels.kt` | `QuotaResponse` 데이터 클래스 추가 |
| `mobile/app/src/main/java/com/sqldpass/app/data/AppRepository.kt` | `fetchQuota()` 노출 |
| 신규 `mobile/app/src/main/java/com/sqldpass/app/ui/common/QuotaBadge.kt` | Compose 컴포넌트 |
| 수정 `mobile/app/src/main/java/com/sqldpass/app/ui/mockexam/MockExamTab.kt` | 헤더에 QuotaBadge(kind=Mock) |
| 수정 `mobile/app/src/main/java/com/sqldpass/app/ui/solve/SolveTab.kt` | 헤더에 QuotaBadge(kind=Question) |
| 검토 `mobile/app/src/main/java/com/sqldpass/app/ui/mockexam/` 및 `nav/SqldpassNav.kt` | 미니 진입 경로 확인 |

## API + 모델

```kotlin
// ApiModels.kt
data class QuotaResponse(
    val questionUsed: Int,
    val questionLimit: Int?,
    val mockUsed: Int,
    val mockLimit: Int?,
    val resetAt: String,    // KST naive
)

// SqldpassApi.kt
@GET("api/quota")
suspend fun getQuota(): QuotaResponse
```

## QuotaBadge Compose

```kotlin
enum class QuotaKind(val label: String) { Question("문제"), Mock("모의고사") }

@Composable
fun QuotaBadge(kind: QuotaKind, viewModel: AppViewModel) {
    val quota by viewModel.quota.collectAsState()
    val limit = when (kind) {
        QuotaKind.Question -> quota?.questionLimit
        QuotaKind.Mock -> quota?.mockLimit
    } ?: return  // null = 활성 구독자 → 숨김
    val used = when (kind) {
        QuotaKind.Question -> quota?.questionUsed ?: 0
        QuotaKind.Mock -> quota?.mockUsed ?: 0
    }
    Text("오늘 $used / $limit ${kind.label}", style = AppTypography.body)
}
```

`AppViewModel` 에 `val quota: StateFlow<QuotaResponse?>` + 탭 진입 시 `refreshQuota()` 호출.

## 미니 진입 경로 검토

`SqldpassNav.kt` 또는 MockExamTab 코드 확인:
- 백엔드 `/api/mock-exams/mini` 호출이 어디서 일어나는지
- 호출 없으면 `MockExamTab` 안에 "미니 모의고사" 섹션 추가 (정규 회차와 분리)
- 호출 있으면 현재 구조 유지

## 검증

```powershell
cd mobile
.\gradlew.bat assembleDebug
```

에뮬레이터:
1. 무료 계정 → MockExamTab → "오늘 0 / 1 모의고사" 표시
2. SolveTab → "오늘 0 / 30 문제" 표시
3. 미니 회차 진입 가능 경로 있음
4. 활성 구독 시드 → 배지 숨김

## Acceptance Criteria

1. SqldpassApi, ApiModels, AppRepository 3 파일에 quota API 추가.
2. QuotaBadge 신규 + MockExamTab, SolveTab 헤더 마운트.
3. limit null 시 표시 숨김.
4. 미니 진입 경로 확인/보강.
5. assembleDebug 성공.

## 금지 사항

- 자체 카운터 만들지 마라. 이유: 서버 단일 진실 소스.
- 색 계열 변경 금지.
- PastExamTab 에 배지 마운트 금지. 이유: 기출 무제한.
- BottomTabBar 또는 SqldpassNav 의 탭 구성 변경 금지. 이유: 정책 변경 아님.

## Status 규칙

- 성공: step 2 `completed`. phase 완료.
- 실패: 3회 후 `error`.
