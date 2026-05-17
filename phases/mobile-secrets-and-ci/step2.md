# Step 2 — GitHub Actions mobile-ci 워크플로우

## 배경

현재 `.github/workflows/` 에는 백엔드(ci.yml, cd.yml), 프론트엔드(frontend-cd.yml), 프록시(proxy-cd.yml) 만 있고 모바일 자동 빌드가 없다. PR 단계에서 모바일 코드 회귀를 잡지 못함.

목적: PR 또는 main push 에 `mobile/**` 변경이 있으면 `:app:assembleDebug` 를 돌려 빌드 깨짐을 자동으로 잡는다. 릴리스 서명·Play Store 업로드는 본 step 범위 밖 (별도 phase).

전제: Step 1 의 `local.properties` fallback 이 들어가 있어 CI 의 `ORG_GRADLE_PROJECT_*` env 가 정상 인식됨.

## 작업 디렉터리

저장소 루트 (`.github/workflows/`).

## 변경 대상

- 신규: `.github/workflows/mobile-ci.yml`

## 변경 내용

`mobile-ci.yml` 작성:

- **트리거**:
  - `pull_request: branches: [main]` + `paths: ['mobile/**', '.github/workflows/mobile-ci.yml']`
  - `push: branches: [main]` 도 같은 paths (회귀 즉시 감지)
- **러너**: `ubuntu-latest`, `timeout-minutes: 30`
- **단계**:
  1. `actions/checkout@v4`
  2. `actions/setup-java@v4` — `distribution: temurin`, `java-version: '17'`
  3. `gradle/actions/setup-gradle@v4` — Gradle daemon + 캐시
  4. `android-actions/setup-android@v3` — SDK 자동 설치 (compileSdk 35 + build-tools 35 + platform-tools)
  5. `chmod +x mobile/gradlew`
  6. `./gradlew :app:assembleDebug --no-daemon` (working-directory: `mobile`)
     - env: `ORG_GRADLE_PROJECT_GOOGLE_WEB_CLIENT_ID: ${{ secrets.GOOGLE_WEB_CLIENT_ID }}`
     - secret 미설정 시 빈 값으로 빌드 → 컴파일 통과 (값이 없어도 OK, 회귀 검증 용도)

추가로 PR 코멘트에 빌드 산출물 경로를 남기지 않고, GitHub Actions 의 기본 로그로 BUILD SUCCESSFUL 확인.

## 운영자 액션 (사용자 수동, 본 step 의 코드 변경 외)

GitHub repo → Settings → Secrets and variables → Actions → New repository secret:

- `GOOGLE_WEB_CLIENT_ID` = `<GCP Web 클라이언트 ID>` (값이 비어도 CI 빌드는 통과)

릴리스 키스토어·Play 서비스 계정 secret 은 별도 phase 에서 추가.

## Acceptance Criteria

1. `.github/workflows/mobile-ci.yml` 신규 파일이 유효한 YAML.
2. main 에 push 후 GitHub Actions 탭에서 `mobile-ci` 워크플로우가 `mobile/**` 또는 워크플로우 파일 변경 시에만 트리거.
3. 워크플로우가 `BUILD SUCCESSFUL` 로 끝남 (GOOGLE_WEB_CLIENT_ID secret 미설정이어도 빌드는 통과).

## 금지 사항

- assembleRelease 를 돌리지 마라. 이유: 키스토어 서명 셋업이 없는 상태 — 본 phase 범위 밖.
- Play Store 자동 업로드 단계를 추가하지 마라. 이유: 동일.
- `mobile/local.properties` 를 CI 에서 생성하지 마라. 이유: AGP 가 sdk.dir 없으면 빌드 실패하지만 `android-actions/setup-android@v3` 가 ANDROID_HOME 을 export 해서 sdk.dir 없이도 인식됨. 명시적 local.properties 생성은 불필요.
- 모든 PR 에서 무조건 돌게 하지 마라. 이유: `paths` 필터로 mobile/ 변경 시에만 실행 — 백엔드/프론트 PR 의 CI 시간 낭비 방지.

## 검증

- 로컬에서 `actionlint` 또는 `yamllint` 없으면 GitHub 의 PR 만들어서 actions 탭 확인.
- 워크플로우의 첫 push 후 Actions 탭에서 `mobile-ci` 잡이 녹색이 되는지 확인.
- 또한 본 workflow 파일 추가 PR 자체가 트리거를 발동시키는지 확인 (자기 자신 변경에 paths 매치).

## Status 규칙

- 성공: index.json step 2 `completed`.
- 실패: GitHub Actions 실패 → 로그 확인 후 yml 수정, 3회 후 `error`.
