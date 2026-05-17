# Step 1 — build.gradle local.properties fallback + 안내

## 배경

`mobile/app/build.gradle:7` 의 `googleWebClientId` 는 `project.findProperty("GOOGLE_WEB_CLIENT_ID")` 만 읽음. Gradle 의 `findProperty` 는 (1) `-P` CLI 인자, (2) `mobile/gradle.properties` 커밋되는 파일, (3) `~/.gradle/gradle.properties` 글로벌, (4) `ORG_GRADLE_PROJECT_*` 환경변수 만 봄. **`mobile/local.properties` 는 안 읽힘**(AGP 표준은 `sdk.dir` 만 자동).

기존 사용자 가이드에서 `local.properties` 에 적으라고 안내했지만 실제로는 적어도 무시되는 상태. 두 가지로 해결:

1. `build.gradle` 가 `local.properties` 도 fallback 으로 읽도록 보강 (개발자 친화)
2. `mobile/gradle.properties` 에 placeholder 주석 추가 (값은 비움, 커밋되는 파일)
3. `mobile/AGENTS.md` 의 셋업 섹션을 정확한 위치 기준으로 갱신

릴리스 키스토어/서명 셋업은 본 step 범위 밖 (별도 phase).

## 작업 디렉터리

`mobile/`

## 변경 대상

- `mobile/app/build.gradle`
- `mobile/gradle.properties`
- `mobile/AGENTS.md`

## 변경 내용

### `mobile/app/build.gradle`

```groovy
def googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: "").toString()
```

→ 다음으로 교체:

```groovy
def googleWebClientId = ""
def fromGradle = project.findProperty("GOOGLE_WEB_CLIENT_ID")
if (fromGradle != null && fromGradle.toString().trim()) {
    googleWebClientId = fromGradle.toString().trim()
} else {
    def localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        def props = new Properties()
        localProps.withInputStream { props.load(it) }
        def fromLocal = props.getProperty("GOOGLE_WEB_CLIENT_ID")
        if (fromLocal != null && fromLocal.trim()) {
            googleWebClientId = fromLocal.trim()
        }
    }
}
```

읽기 우선순위: `-P` 인자 → `gradle.properties` → `~/.gradle/gradle.properties` → `ORG_GRADLE_PROJECT_*` env → **`local.properties` (신규 fallback)**.

### `mobile/gradle.properties`

기존 4줄 끝에 한 단락 추가:

```properties
# Google OAuth Web Client ID — Android Sign-In ID 토큰 audience.
# 이 파일은 커밋되므로 실제 값은 여기 두지 말 것.
# 개발자 머신은 mobile/local.properties (gitignore) 또는 ~/.gradle/gradle.properties 에,
# CI 는 GitHub Secrets 의 ORG_GRADLE_PROJECT_GOOGLE_WEB_CLIENT_ID 환경변수로 주입.
# 비어있으면 빌드는 통과하지만 Google 로그인이 즉시 RESULT_CANCELED 로 튕긴다.
# GOOGLE_WEB_CLIENT_ID=
```

### `mobile/AGENTS.md`

`## Commands` 와 `## Coding Rules` 사이에 신규 섹션 `## Local secrets` 추가. 내용: GOOGLE_WEB_CLIENT_ID 셋업 위치 우선순위, 권장 위치(local.properties 또는 ~/.gradle/gradle.properties), CI 의 환경변수 패턴, 키스토어는 별도 phase 라는 한 줄 안내.

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL (값 없이도 통과).
2. `mobile/local.properties` 에 `GOOGLE_WEB_CLIENT_ID=xxx` 한 줄 넣으면 APK 안 `res/values/strings.xml` 의 `google_web_client_id` 에 그 값이 들어감 (수동 검증 시).
3. `mobile/gradle.properties` 에는 placeholder 주석만, 실제 값 없음(커밋되는 파일).

## 금지 사항

- `local.properties` 를 git 에 add 하지 마라. 이유: `mobile/.gitignore:5` 가 이미 제외 중. 실수로 staging 되면 OAuth ID 가 공개 git 히스토리에 남음.
- `gradle.properties` 에 실제 OAuth ID 값을 적지 마라. 이유: 커밋되는 파일. 식별자 자체는 SHA-1 매칭으로 보호되지만 공개를 의도한 적 없음 — 키스토어 잠금이 풀려있는 디버그 빌드라면 의미가 있을 수 있어 보호하는 게 위생적.
- Kotlin DSL 로 변환하지 마라. 이유: 현재 프로젝트가 Groovy DSL 일관. 변환은 본 phase 범위 밖.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄.
- 실패: 3회 시도 후 `error` + `error_message`.
