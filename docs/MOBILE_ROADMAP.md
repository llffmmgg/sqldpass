# 모바일 출시 로드맵 (Phase 1 ~ 9)

목표: 현재 인증·풀이까지 동작하는 debug 상태(`com.sqldpass.app 0.1.0`)에서, 즐겨찾기·PASS+ 카탈로그·디자인 폴리시·릴리스 서명·Play Store 출시까지.

> 백엔드 통계 endpoint(합격률·일별 추이) 와 모바일 단위 테스트 인프라는 본 로드맵에서 제외 (별도 로드맵).

---

## Phase 1 — `mobile-prod-audience` (운영 백엔드 audience 등록)

**작업량**: 30분 (검증 위주, 코드 변경 가능성은 작음)

### 배경
디버그 빌드 로그인이 동작했다는 건 **운영 백엔드(`api.sqldpass.com`) 가 이미 audience 를 받아주고 있다는 강한 증거**. 다만 운영 컨테이너에 `GOOGLE_OAUTH_ANDROID_CLIENT_ID` 환경변수가 정확한 값으로 들어가 있는지 확인하고, 안 들어가 있으면 등록.

### 작업
1. 운영 서버 `docker inspect` 또는 `cd.yml` 의 `envs` 라인 확인 — `GOOGLE_OAUTH_ANDROID_CLIENT_ID` 존재 여부
2. 없으면 `.github/workflows/cd.yml` 의 `envs` 목록에 추가하고 GitHub Secret 등록 (값: Web 클라이언트 ID 와 동일)
3. 배포 트리거 후 모바일에서 로그인 정상 동작 재확인

### 산출물
- (필요 시) `cd.yml` 의 `envs` 라인 한 줄 추가 + `GOOGLE_OAUTH_ANDROID_CLIENT_ID` GitHub Secret

---

## Phase 2 — `mobile-runner-pro` (풀이 화면 보강)

**작업량**: 약 8시간. UX 임팩트 최대.

### 작업 묶음
1. **타이머** — 모의고사·기출복원 풀이 시작 시 카운트다운(시험 시간) 표시. 0 도달 시 자동 제출
2. **문제 점프 그리드** — 상단/하단에 1~N번 셀 `LazyVerticalGrid`. 답안 여부 색상 구분(미답·답함·현재). 탭 시 해당 문항으로 이동
3. **즐겨찾기 토글** — 풀이 화면 상단에 별 아이콘. `POST/DELETE /api/bookmarks/{questionId}` + 로컬 optimistic update
4. **신고 버튼** — 우상단 메뉴에서 신고 다이얼로그. `POST /api/feedback` (백엔드 endpoint·DTO 확인 필요)
5. **채점 후 정답·해설 inline** — 결과 화면에 문항별 펼침 카드. `SolveResponse` 가 정답 포함 안 하면 별도 fetch (`/api/solves/{id}/items` 같은 endpoint 확인 필요)

### Step 분할
- Step 1: 백엔드 신고 endpoint + 즐겨찾기 endpoint 호환성 점검 (read-only)
- Step 2: `RunnerSession` 에 `examDurationSeconds` 추가, `QuestionRunnerScreen` 에 타이머 UI + 자동 제출 로직
- Step 3: 문제 점프 그리드 (모달 또는 풀이 화면 상단 가로 스크롤)
- Step 4: 즐겨찾기·신고 버튼 + `BookmarkRepository`/`FeedbackRepository`
- Step 5: 결과 화면에 정답·해설 펼침 카드

### 의존성
- 백엔드 시간 제한 정보가 `MockExamDetail`/`PastExamDetail` 에 있는지 확인. 없으면 클라이언트에서 60분 default 또는 백엔드에 한 필드 추가
- 신고 endpoint 가 모바일 호환인지 확인

---

## Phase 3 — `mobile-passplus-catalog` (구매 동선)

**작업량**: 약 3시간. 수익 동선.

### 작업
- 신규 화면 `PassPlusCatalogScreen` — 4개 상품 카드 (Thunder 3,900 / Focus 2,900 / Pro 9,900 / All Pass 29,900). `BillingManager.productDetails` 에서 가격·기간 표시
- 활성 구독 카드 — `GET /api/payment/subscription` 응답을 상단에. plan + 만료일 + 자동 갱신 여부 표시
- 결제 → `BillingManager.launch(activity, productId)` → 결과 `verifyPlayBilling` 응답 → 토스트
- 진입 동선: 홈의 "PASS+ 보기" 버튼 + 대시보드 PASS+ 카드 + 잠금 회차 카드 탭

### 산출물
- `mobile/app/.../ui/passplus/PassPlusCatalogScreen.kt`
- `AppRepository.subscription()` 메서드 추가
- `MainActivity` 의 5탭에 다음 화면 풀스크린 push (탭 외 화면 첫 도입)

---

## Phase 4 — `mobile-wrong-answers` (오답노트 통합)

**작업량**: 약 2시간. 백엔드 `/api/wrong-answers` 이미 있음.

### 작업
- 대시보드에 "취약 과목" 섹션 + "오답 모아풀기" 버튼
- 신규 풀이 모드: `RunnerMode.WRONG_ANSWERS` — `GET /api/wrong-answers?subjectId=` 으로 받은 문제로 `QuestionRunnerScreen` 재사용
- 채점은 기존 `/api/solves` 재사용

### 산출물
- `AppRepository.wrongAnswers(subjectId)`, `wrongAnswersStats()`
- `DashboardTab` 에 취약 과목 카드 추가
- `RunnerMode` 에 `WRONG_ANSWERS` 추가

---

## Phase 5 — `mobile-design-polish`

**작업량**: 약 6시간. 디자인 마감.

### 작업 묶음
1. **TopAppBar + scrollBehavior** — 각 탭에 상단 바, 스크롤 시 축소. Compose Navigation 도입 동반
2. **Compose Navigation** — 현재 `when (selected)` 분기를 `NavHost` 로 교체, 전환 애니메이션
3. **Pretendard 폰트** — `res/font/pretendard_*.ttf` 추가, `Type.kt` 의 `Display` 한 줄 교체
4. **로딩 스켈레톤** — `CircularProgressIndicator` 대신 카드 shimmer
5. **다크모드 토글** — 시스템 따라가는 것 + 앱 내 강제 (라이트/다크/시스템) — 회원 메뉴 또는 대시보드
6. **회원 닉네임 편집** — 대시보드 회원 카드에서 편집. 백엔드 `PATCH /api/members/me` 확인 필요

### Step 분할
- Step 1: Compose Navigation 골격 (Navigation Compose 의존성, NavHost, 탭 → composable destination)
- Step 2: 각 탭에 TopAppBar (스크롤 동작 포함)
- Step 3: Pretendard 폰트 임포트
- Step 4: 스켈레톤 컴포넌트 + 적용
- Step 5: 다크모드 토글 (SettingsStore / DataStore preferences)
- Step 6: 닉네임 편집 (백엔드 확인 + UI)

---

## Phase 6 — `mobile-launcher-assets` (앱 아이콘·스플래시)

**작업량**: 30분 (자료 준비는 사용자 작업).

### 작업
- launcher 아이콘 PNG 셋 (`mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`) + adaptive icon XML (`mipmap-anydpi-v26/ic_launcher.xml`)
- Android 12+ splash screen API (`SplashScreen` 테마) — `colors.xml` + `themes.xml` 보강
- `AndroidManifest.xml` 의 `android:icon`/`android:roundIcon` 을 새 `@mipmap/ic_launcher` 로 변경

### 산출물
- `mipmap-*` 디렉토리 + adaptive icon XML
- `themes.xml` 에 splash 테마 + 색상 토큰

---

## Phase 7 — `mobile-release-keystore` (릴리스 서명 셋업)

**작업량**: 45분 (사용자 작업 동반).

### 작업 (사용자)
- 키스토어 생성:
  ```powershell
  keytool -genkeypair -v `
    -keystore $env:USERPROFILE\sqldpass-release.keystore `
    -alias sqldpass -keyalg RSA -keysize 2048 -validity 10000
  ```
- 비밀번호 + alias 비밀번호 **백업 필수** (분실 = 같은 앱 영구 재출시 불가)
- 릴리스 SHA-1 추출 후 GCP Console 의 Android OAuth 클라이언트에 "+ SHA-1 추가" 등록

### 작업 (코드)
- `mobile/app/build.gradle` 에 `signingConfigs.release { ... }` 추가 — 환경변수 기반 (`SQLDPASS_KEYSTORE_PATH/PASS/KEY_PASS/KEY_ALIAS`)
- `buildTypes.release { signingConfig signingConfigs.release; minifyEnabled true; proguardFiles ... }` 추가
- ProGuard 규칙 — Moshi/Retrofit/Compose 호환 (기존 라이브러리 의존성에 맞춘 `consumer-rules.pro` 추가 검증)

### 산출물
- `mobile/app/build.gradle` 의 signingConfigs + buildTypes.release
- `mobile/app/proguard-rules.pro`
- 로컬 환경변수 (`SQLDPASS_KEYSTORE_PATH=...` 등 셋업 안내 — `mobile/AGENTS.md` 갱신)

---

## Phase 8 — `mobile-release-cd` (Play Internal 자동 업로드)

**작업량**: 1시간 (사용자 GCP·Play 셋업 동반).

### 사전 작업 (사용자)
- Play Console 에서 앱 등록 (`com.sqldpass.app`, 한국어 기본)
- "API 액세스" 에서 서비스 계정 + JSON 키 발급
- 인앱 상품 4종 등록 (`iap_three_day` 등)

### GitHub Secrets 5개 등록
- `KEYSTORE_BASE64` — `base64 -w0 release.keystore | clip` 으로 클립보드 → Secrets
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- `PLAY_PUBLISHER_JSON` — 서비스 계정 JSON 통째로

### 작업 (코드)
- `mobile/build.gradle` 에 `gradle-play-publisher` 플러그인 의존성
- `mobile/app/build.gradle` 에 `play { ... }` 블록 — Internal 트랙, draft, 한국어 우선
- `.github/workflows/mobile-cd.yml` 신규:
  - trigger: main push (or release tag)
  - keystore base64 디코드 → `release.keystore` 파일
  - service account JSON 디코드 → 파일
  - versionCode = `github.run_number`
  - `./gradlew :app:publishReleaseBundle --no-daemon`

### 산출물
- `mobile/build.gradle` plugin 선언
- `mobile/app/build.gradle` play 블록
- `.github/workflows/mobile-cd.yml`

---

## Phase 9 — `mobile-play-listing` (스토어 리스팅)

**작업량**: 사용자 작업 1~2일. 코드 작업 거의 없음.

### 자료 (사용자 준비)
- **그래픽**: 앱 아이콘 512×512, 피처 그래픽 1024×500, 휴대전화 스크린샷 2~8장 (1080×1920)
- **메타** (한국어):
  - 앱 이름 50자: `문어CBT - SQLD 정처기 컴활 ADsP 무료 모의고사`
  - 짧은 설명 80자
  - 자세한 설명 4000자 (기존 `frontend/src/app/page.tsx` SEO 카피 재활용)
- **법적**:
  - 개인정보처리방침 URL: `https://www.sqldpass.com/privacy`
  - 약관 URL: `https://www.sqldpass.com/terms`
  - 콘텐츠 등급 설문 — 카테고리 "참고서/교육", 모든 항목 "없음"
  - 데이터 보안 양식 — 이름·이메일 수집, 결제(Google Play 처리), 앱 활동(풀이 기록)

### 출시 단계
1. **내부 테스트 트랙** — Phase 8 의 mobile-cd 가 자동 업로드. 본인 폰에서 테스트 링크로 받아 동작 확인
2. **비공개 테스트** — 20명 모아 1~2주 (선택)
3. **프로덕션** — Google 심사 1~7일 (첫 출시 14일도)
4. **Staged rollout** — 1% → 5% → 20% → 100%

---

# Phase 의존성 그래프

```
Phase 1 (audience)  ──┐
                       │
Phase 2 (runner-pro) ──┼─► (병렬 가능)
Phase 3 (passplus)   ──┤
Phase 4 (wrong-answers)┘
       │
       ▼
Phase 5 (design-polish)  ← Phase 2·3·4 결과 위에 폴리시
       │
       ▼
Phase 6 (launcher-assets)
       │
       ▼
Phase 7 (keystore + signingConfigs)
       │
       ▼
Phase 8 (mobile-cd + Play upload)
       │
       ▼
Phase 9 (스토어 리스팅 + rollout)
```

---

# 권장 진행 순서 + 누적 작업량

| 순번 | Phase | 추정 | 누적 | 비고 |
|---|---|---|---|---|
| 1 | Phase 1 audience | 0.5h | 0.5h | 검증만, 빠름 |
| 2 | Phase 2 runner-pro | 8h | 8.5h | UX 임팩트 최대 |
| 3 | Phase 3 passplus | 3h | 11.5h | 수익 동선 |
| 4 | Phase 4 wrong-answers | 2h | 13.5h | 빠른 가치 추가 |
| 5 | Phase 5 design-polish | 6h | 19.5h | 마감 톤 |
| 6 | Phase 6 launcher-assets | 0.5h | 20h | 시각 자료 |
| 7 | Phase 7 keystore | 0.75h | 20.75h | 출시 준비 |
| 8 | Phase 8 mobile-cd | 1h | 21.75h | 자동 출시 |
| 9 | Phase 9 listing | (사용자 1~2일) | — | 콘텐츠 + Google 심사 |

총 코드 작업 ≈ **22시간**. 추가로 Phase 9 의 사용자 콘텐츠 작업 1~2일 + Google 심사 대기 1~7일.

---

# 각 Phase 의 결정 사항 (작업 시작 전 확인 필요)

| Phase | 결정 |
|---|---|
| 2 | 시험 시간 제한이 백엔드 `MockExamDetail` 에 있는지 / 없으면 추가 vs 클라이언트 default |
| 2 | 신고 endpoint 는 `POST /api/feedback`? 다른 경로? 모바일 호환 DTO? |
| 2 | 채점 응답에 문항별 정답·해설이 있는지 / 없으면 `GET /api/solves/{id}/items` 같은 별도 endpoint |
| 5 | Compose Navigation 도입 — 탭 셋업 자체를 NavHost 로 바꿀지, 탭 안에서만 사용할지 |
| 5 | 다크모드 토글의 저장 위치 — DataStore 도입 vs SharedPreferences 단순 사용 |
| 7 | ProGuard 활성화 — `minifyEnabled true` 가 Moshi reflection 깨지 않게 keep 규칙 추가 |
| 8 | versionName 정책 — semver(`1.0.0`) 수동 관리 vs CI 자동 증가 |
| 9 | 스토어 리스팅의 짧은 설명 80자·자세한 설명 4000자 카피 — 마케팅 협의 필요 시 별도 작업 |
