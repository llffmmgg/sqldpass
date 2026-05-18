# Step 1 — Android test source set + TokenAuthenticator 단위 테스트

## 작업 디렉터리
`mobile/`

## 배경 / Why
- 현재 `mobile/app/src/` 에 `test/`, `androidTest/` 디렉터리 부재. release 직전에 회귀 검증 불가.
- TokenAuthenticator 는 Mutex 기반 single-flight + 무한 루프 가드 + 세션 만료 처리가 핵심 로직 — 가장 미세한 변경이 결제·인증 흐름 전체를 깨뜨릴 수 있어 테스트 우선순위 1순위.

## 변경 대상

### 1. `mobile/app/build.gradle` — 테스트 의존성 추가
- 기존 dependencies 블록에 다음이 있는지 확인:
  - `junit:junit:4.13.2` 또는 `org.junit.jupiter:junit-jupiter:5.x`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0` (이미 메인에 1.9.0 있음 — test variant)
  - `io.mockk:mockk:1.13.x` — Kotlin Mockito 보다 lambda/coroutine 친화적
  - `com.squareup.okhttp3:mockwebserver:4.12.0` — OkHttp Authenticator 테스트에 적합
- 없으면 추가:
  ```gradle
  testImplementation "junit:junit:4.13.2"
  testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
  testImplementation "io.mockk:mockk:1.13.13"
  testImplementation "com.squareup.okhttp3:mockwebserver:4.12.0"
  ```

### 2. 디렉터리 신설
- `mobile/app/src/test/java/com/sqldpass/app/auth/`

### 3. `mobile/app/src/test/java/com/sqldpass/app/auth/TokenAuthenticatorTest.kt`
테스트 시나리오 (실제 OkHttp 동작 매칭 위해 `MockWebServer` 권장):

1. **첫 401 + refresh 성공 → 새 토큰으로 retry 요청 전송**
   - MockWebServer 가 첫 요청에 401, 두 번째 요청에 200 응답.
   - Authenticator 의 refreshApiProvider 가 새 토큰 반환하도록 mock.
   - 결과 응답이 200 이고, 두 번째 요청 Authorization 헤더가 새 토큰 포함.
   - tokenStore 가 새 토큰으로 업데이트됨.

2. **두 번째 401 (이미 retry 후) → 더 retry 안 함, sessionLost 콜백 호출**
   - MockWebServer 가 양 요청 모두 401.
   - tokenStore 가 clear 됨.
   - onSessionLost 콜백이 정확히 1번 호출됨.

3. **원본 요청에 Authorization 헤더 없음 → authenticate() 가 null 반환 (retry 안 함)**
   - 직접 Authenticator.authenticate(null, response) 호출 — Authorization 없는 Request 의 response.
   - 반환값 null 검증, refresh 호출 안 됨.

4. **Refresh API 자체가 throw → tokenStore clear + onSessionLost**
   - refreshApiProvider 가 IOException throw.
   - tokenStore.clear() 호출됨, onSessionLost 1번.

5. **단일 비행 (single-flight) — 동시 2개 401 → refresh 1번만**
   - 2개 코루틴이 동시에 authenticate 진입.
   - refreshApiProvider 가 단 1번만 호출됨.
   - 둘 다 같은 새 토큰 받음.
   - Mutex + tokenStore.token 변화 확인 로직 검증.

각 테스트는 MockK 로 TokenStore, AuthRefreshApi mock + Spy 패턴.

### 4. tokenStore mock 동작
- `TokenStore` 는 EncryptedSharedPreferences 사용 → 안드로이드 의존성. test source set 은 unit (`src/test/`) 이므로 인스턴스 직접 못 만듦.
- Mock 사용: `mockk<TokenStore>()` + `every { tokenStore.token } returns ...` / `every { tokenStore.token = any() } just runs`.

## 작업 절차
1. build.gradle 의존성 보강 + 동기화.
2. test 디렉터리 생성 + TokenAuthenticatorTest.kt 작성.
3. `gradlew :app:testDebugUnitTest --tests "com.sqldpass.app.auth.TokenAuthenticatorTest"` 통과 확인.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:testDebugUnitTest --tests "com.sqldpass.app.auth.TokenAuthenticatorTest"
```

## 금지사항
- 실제 EncryptedSharedPreferences 인스턴스 만들기 시도하지 말 것. 이유: unit test source set 은 Android Framework 없이 JVM 만 — 실패. Mock 만 사용.
- `runBlocking` 외 다른 코루틴 scope 로 테스트 작성 금지. 이유: Authenticator 의 authenticate() 가 동기 호출이라 테스트도 동기 패턴 유지.
- Robolectric 도입 금지. 이유: 본 phase 의 단위 테스트 영역이 안드로이드 framework 의존 안 함 — pure JVM 으로 충분.

## 산출물
- build.gradle 의존성 변경.
- 신규 test 파일 + 시나리오 수.
- gradle 테스트 결과 마지막 5줄.
