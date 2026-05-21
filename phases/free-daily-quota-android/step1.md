# Step 1 — OkHttp 인터셉터 + 페이월 Bottom Sheet

## 배경

백엔드는 무료 회원이 일일 한도를 넘기면 HTTP 402 + body `{ error, used, limit, resetAt }` 반환. Android 클라이언트는 OkHttp 인터셉터(또는 Retrofit Response 분기)에서 402 를 잡고 Compose Bottom Sheet 로 페이월 노출.

**카운팅 로직 없음.** 서버 단일 진실 소스.

## 작업 디렉터리

```
mobile/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| 기존 OkHttp 설정 위치 (auth 관련, `mobile/app/src/main/java/com/sqldpass/app/auth/` 또는 `data/remote/`) 확인 | 402 인터셉터 추가 |
| 신규 `mobile/app/src/main/java/com/sqldpass/app/data/QuotaExceededException.kt` | 예외 클래스 |
| 신규 `mobile/app/src/main/java/com/sqldpass/app/ui/common/QuotaPaywallSheet.kt` | Compose Bottom Sheet |
| 수정 `mobile/app/src/main/java/com/sqldpass/app/ui/AppViewModel.kt` 또는 SqldpassNav | 전역 상태로 quota 시트 표시 |

먼저 OkHttp Client 설정 위치 확인:

```
mobile/app/src/main/java/com/sqldpass/app/data/remote/
mobile/app/src/main/java/com/sqldpass/app/auth/TokenAuthenticator.kt
```

`TokenAuthenticator` 가 401 처리. 그 옆 또는 OkHttpClient 빌더에 인터셉터 추가.

## QuotaExceededException

```kotlin
package com.sqldpass.app.data

data class QuotaInfo(
    val code: String,        // DAILY_QUESTION_LIMIT | DAILY_MOCK_LIMIT
    val used: Int,
    val limit: Int,
    val resetAt: String,
)

class QuotaExceededException(val info: QuotaInfo) : RuntimeException(info.code)
```

## Interceptor 작성

```kotlin
class QuotaInterceptor(
    private val moshi: Moshi,        // 기존 주입 패턴 따름
    private val onQuotaExceeded: (QuotaInfo) -> Unit,
) : Interceptor {
    private val adapter by lazy {
        moshi.adapter(QuotaInfo::class.java)  // 또는 별도 응답 DTO
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 402) {
            val body = response.peekBody(Long.MAX_VALUE).string()
            runCatching {
                val payload = adapter.fromJson(body)
                if (payload != null) {
                    onQuotaExceeded(payload)
                    throw QuotaExceededException(payload)
                }
            }
        }
        return response
    }
}
```

OkHttpClient 빌더에 `.addInterceptor(QuotaInterceptor(...))` 추가. 기존 TokenAuthenticator 와 함께.

## AppViewModel 전역 상태

```kotlin
class AppViewModel(...) : ViewModel() {
    private val _quotaPaywall = MutableStateFlow<QuotaInfo?>(null)
    val quotaPaywall: StateFlow<QuotaInfo?> = _quotaPaywall

    fun showQuotaPaywall(info: QuotaInfo) { _quotaPaywall.value = info }
    fun dismissQuotaPaywall() { _quotaPaywall.value = null }
}
```

QuotaInterceptor 의 `onQuotaExceeded` 콜백을 `AppViewModel::showQuotaPaywall` 에 바인딩.

## QuotaPaywallSheet 컴포넌트

```kotlin
@Composable
fun QuotaPaywallSheet(
    info: QuotaInfo,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(...) {
            // 🐙 마스코트
            Text(title)
            Text(body)
            AppButton("Focus 7일권 보기", onClick = onPurchase)
            TextButton(onClick = onDismiss) { Text("내일 다시 오기") }
        }
    }
}
```

문구 (확정):
- `DAILY_QUESTION_LIMIT`: "오늘의 30문제 완주! 🐙" / "내일 다시 만나거나, Focus 7일권으로 끝까지 가볼까요?"
- `DAILY_MOCK_LIMIT`: "오늘 모의고사 1회 완료" / "Focus 7일권으로 매일 풀 수 있어요."

`AppBottomSheet`, `AppButton` 등 기존 디자인 시스템 재사용.

## SqldpassNav 마운트

`SqldpassNav.kt` 또는 MainActivity 최상위에서:

```kotlin
val quotaInfo by appViewModel.quotaPaywall.collectAsState()
quotaInfo?.let { info ->
    QuotaPaywallSheet(
        info = info,
        onDismiss = { appViewModel.dismissQuotaPaywall() },
        onPurchase = { /* 결제 화면 네비게이션 */ }
    )
}
```

## 검증

```powershell
cd mobile
.\gradlew.bat assembleDebug
```

에뮬레이터로:
1. 무료 시드 → 31번째 문제 → Bottom Sheet 표시
2. 모의고사 2회 진입 → 표시
3. 기출복원 다회 → 표시 안 됨
4. 활성 구독 시드 → 무제한

## Acceptance Criteria

1. QuotaInterceptor, QuotaExceededException, QuotaPaywallSheet 신규.
2. OkHttpClient 에 인터셉터 등록.
3. AppViewModel 전역 상태 + SqldpassNav 마운트.
4. assembleDebug 성공.

## 금지 사항

- 자체 카운터 (Room DB 일일 카운트 등) 만들지 마라. 이유: 서버 단일 진실 소스.
- 색 계열 변경 금지(feedback_color_token_changes).
- backdrop blur, glow, opacity pulse 사용 금지(feedback_no_ai_blur_effects).
- 기존 TokenAuthenticator(401 처리) 동작을 변경하지 마라. 이유: 본 작업은 402 추가만.
- 미니/모의/기출 진입 호출 측에 try/catch 분산 추가 금지. 이유: 인터셉터에서 단일 처리.

## Status 규칙

- 성공: step 1 `completed`.
- 실패: 3회 후 `error`.
