# Step 2 — 백엔드 Google audience 에 iOS Client ID 허용

## Background

iOS 앱이 Google Sign-In 으로 발급받는 ID Token 은 `aud` 클레임에 **iOS 전용 Client ID** 가 들어있다. 현재 `GoogleOAuthClient.verifyIdToken` 은 `clientId`(웹) 또는 `androidClientId` 만 허용해 iOS 토큰을 거절한다.

`application.yaml` 에 `ios-client-id` 키를 추가하고 `GoogleOAuthClient` 가 그 값도 허용하도록 확장한다.

## Workdir

```bash
backend/
```

⚠️ 본 step 의 컴파일·테스트 검증은 **윈도우에서 `./gradlew compileJava`** 로 진행한다. macOS 셸에서는 코드 변경만 작성한다.

## Scope

| File | Change |
| --- | --- |
| `backend/src/main/resources/application.yaml` | `sqldpass.oauth.google.ios-client-id` 키 추가 |
| `backend/src/main/java/com/sqldpass/service/auth/GoogleOAuthClient.java` | `iosClientId` 필드 + `verifyIdToken` audience 검증에 iOS Client ID 허용 |

## Implementation

### `application.yaml`

`sqldpass.oauth.google` 블록에 `ios-client-id` 추가. 기존 `android-client-id` 와 같은 패턴.

기존:

```yaml
sqldpass:
  oauth:
    google:
      client-id: ${GOOGLE_OAUTH_CLIENT_ID:not-set}
      client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET:not-set}
      # 안드로이드 Capacitor 앱이 네이티브 Google Sign-In 으로 받는 ID 토큰의 audience.
      # Google Cloud Console 에서 Android 클라이언트 (package name + SHA-1) 를 따로 발급해 입력.
      android-client-id: ${GOOGLE_OAUTH_ANDROID_CLIENT_ID:}
```

→ 아래로 교체 (`ios-client-id` 한 줄 추가):

```yaml
sqldpass:
  oauth:
    google:
      client-id: ${GOOGLE_OAUTH_CLIENT_ID:not-set}
      client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET:not-set}
      # 안드로이드 Capacitor 앱이 네이티브 Google Sign-In 으로 받는 ID 토큰의 audience.
      # Google Cloud Console 에서 Android 클라이언트 (package name + SHA-1) 를 따로 발급해 입력.
      android-client-id: ${GOOGLE_OAUTH_ANDROID_CLIENT_ID:}
      # iOS 네이티브 Google Sign-In SDK 가 발급하는 ID 토큰의 audience.
      # Google Cloud Console 에서 iOS 클라이언트 (Bundle ID: com.sqldpass.app) 를 따로 발급해 입력.
      ios-client-id: ${GOOGLE_OAUTH_IOS_CLIENT_ID:}
```

운영 환경 변수 `GOOGLE_OAUTH_IOS_CLIENT_ID` 가 없으면 빈 문자열로 fallback — `verifyIdToken` 에서 빈 문자열이면 매칭 무시.

### `GoogleOAuthClient.java`

기존 구조:

```java
@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String ID_TOKEN_VERIFY_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final String clientId;
    private final String androidClientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GoogleOAuthClient(
            @Value("${sqldpass.oauth.google.client-id}") String clientId,
            @Value("${sqldpass.oauth.google.client-secret}") String clientSecret,
            @Value("${sqldpass.oauth.google.android-client-id:}") String androidClientId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.androidClientId = androidClientId;
        this.restClient = RestClient.create();
    }
```

→ `iosClientId` 필드 + 생성자 인자 추가:

```java
@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String ID_TOKEN_VERIFY_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final String clientId;
    private final String androidClientId;
    private final String iosClientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GoogleOAuthClient(
            @Value("${sqldpass.oauth.google.client-id}") String clientId,
            @Value("${sqldpass.oauth.google.client-secret}") String clientSecret,
            @Value("${sqldpass.oauth.google.android-client-id:}") String androidClientId,
            @Value("${sqldpass.oauth.google.ios-client-id:}") String iosClientId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.androidClientId = androidClientId;
        this.iosClientId = iosClientId;
        this.restClient = RestClient.create();
    }
```

그리고 `verifyIdToken` 의 audience 검증 분기에 iOS Client ID 추가. 기존:

```java
String aud = response.has("aud") ? response.get("aud").asText() : "";
if (!aud.equals(clientId)
        && !(androidClientId != null && !androidClientId.isBlank() && aud.equals(androidClientId))) {
    log.warn("Google ID token aud mismatch: aud={}", aud);
    throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
}
```

→ iOS 분기 추가:

```java
String aud = response.has("aud") ? response.get("aud").asText() : "";
boolean audMatches =
        aud.equals(clientId)
                || (androidClientId != null && !androidClientId.isBlank() && aud.equals(androidClientId))
                || (iosClientId != null && !iosClientId.isBlank() && aud.equals(iosClientId));
if (!audMatches) {
    log.warn("Google ID token aud mismatch: aud={}", aud);
    throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
}
```

기존 `iss` 검증 분기는 그대로 유지.

## Validation

본 step 의 검증은 **윈도우에서 별도로 수행**한다. macOS 셸에서는 컴파일 시도하지 말 것 — 시간 큼.

윈도우 검증 (사용자가 별도 수행):

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test --tests "*GoogleOAuthClient*"
```

배포 전 운영 환경에 `GOOGLE_OAUTH_IOS_CLIENT_ID=1026495161962-jtgphtjb90025fsmvb8e13tpnokv3edp.apps.googleusercontent.com` 환경 변수 추가 필요.

macOS 셸에서의 본 step 검증은 다음만 수행:

```bash
# 키 추가 확인
grep -c "ios-client-id" backend/src/main/resources/application.yaml
# 코드 변경 확인
grep -c "iosClientId" backend/src/main/java/com/sqldpass/service/auth/GoogleOAuthClient.java
```

두 grep 모두 ≥ 1 이어야 함.

## 금지사항

- iOS Client ID 를 application.yaml 에 **하드코딩 하지 마라**. 이유: 환경마다 다를 수 있고(테스트 vs 프로덕션), 시크릿은 환경 변수 통해 주입.
- `verifyIdToken` 의 기존 `iss` 검증을 건드리지 마라. 이유: 변경 범위 최소화. audience 만 추가.
- `androidClientId` 와 `iosClientId` 를 하나의 `nativeClientIds` 리스트로 합치는 리팩토링 금지. 이유: 변경 범위 최소화. 향후 별도 phase 에서 정리.
- macOS 셸에서 `./gradlew compileJava` 시도하지 마라. 이유: 백엔드 빌드는 윈도우 작업. macOS 에서 처음 실행 시 Gradle 캐시·의존성 다운로드로 5~10분 추가 소요 + Harness AGENTS.md 의 작업 디렉토리 규칙 위반.
