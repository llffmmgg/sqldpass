# Step 2 — Android OkHttp timeout

## 작업 디렉터리
`mobile/`

## 배경 / Why
- `mobile/app/src/main/java/com/sqldpass/app/SqldpassApplication.kt` 의 두 OkHttp 클라이언트(`okHttp`, `refreshOkHttp`) 가 기본 timeout 사용 (각 10s 정도). 결제·인증 흐름에서 OS sleep 후 connection 살아있는 듯 보이는 잔류 socket 에서 hang 가능.

## 변경 대상

### `mobile/app/src/main/java/com/sqldpass/app/SqldpassApplication.kt`
- 두 `OkHttpClient.Builder()` 모두에 명시 timeout:
  ```kotlin
  import java.util.concurrent.TimeUnit
  ...
  OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .addInterceptor(authHeaderInterceptor)
      // ...
  ```
- 공통 helper 로 묶을 수 있으면 깔끔하지만 두 client 가 빌더 체인이 짧으니 그냥 양쪽 모두 직접 추가도 OK.

## 작업 절차
1. SqldpassApplication.kt 의 두 OkHttp 빌더에 timeout 추가.
2. java.util.concurrent.TimeUnit import.
3. `:app:assembleDebug` 통과 확인.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:assembleDebug
```

## 금지사항
- callTimeout 추가하지 말 것. 이유: callTimeout 은 인증 retry 까지 포함한 전체 시간이라 TokenAuthenticator 재발급 후 retry 가 잘려나갈 수 있음. connect/read/write 만으로 충분.
- retry-on-failure 변경 금지. OkHttp 기본값 유지.

## 산출물
- 수정 파일 (1개) + 한 줄 요약.
- `gradlew :app:assembleDebug` 결과 마지막 5줄.
