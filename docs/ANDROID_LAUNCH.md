# 안드로이드 앱 출시 가이드

이 문서는 sqldpass 안드로이드 앱(Capacitor 외부 URL 모드)을 Play Store 에 올리기까지의 외부 셋업을 단계별로 정리한 체크리스트다. 코드 작업은 거의 끝났고, 남은 일은 대부분 외부 콘솔에서 누르는 절차.

**전제**: `feat(android)` 와 `feat(payment)` 커밋들이 main 에 들어간 상태. `mobile/` 워크스페이스가 존재하고 `npm install` 까지 완료.

---

## 체크리스트 개요

| 단계 | 누가 | 비용 | 소요 |
|---|---|---|---|
| 1. Capacitor Play Billing 플러그인 결정 | 사용자 + 코드 | - | 0.5~2일 |
| 2. 로컬 빌드 환경 (JDK 17, Android Studio) | 사용자 | 무료 | 1~2시간 |
| 3. Google Play Console 셋업 | 사용자 | $25 일회 | 0.5~1일 |
| 4. Google Cloud OAuth 클라이언트 | 사용자 | 무료 | 30분 |
| 5. 키스토어 + 첫 빌드 검증 | 사용자 + 코드 | - | 1~2시간 |
| 6. Play Store 리스팅 자료 | 사용자 | - | 1일 |
| 7. 내부 테스트 → 비공개 → 프로덕션 | 사용자 | - | 1주~2주 (Google 심사) |

**FCM 푸시는 의도적으로 제외** — 출시 후 v1.1 업데이트로 추가.

---

## 1. Capacitor Play Billing 플러그인 결정

현재 코드는 `window.Capacitor.Plugins.Billing.purchase()` 를 호출하도록 작성돼 있고 (`frontend/src/lib/payment.ts`), 인터페이스는 `frontend/src/lib/platform.ts:CapacitorBillingPlugin` 에 정의돼 있다. 이 인터페이스를 만족하는 native 구현체를 mobile/ 에 붙여야 결제가 실제로 동작한다.

**옵션 A — 커스텀 Capacitor 플러그인 자체 작성 (Cap 7 유지, 권장)**
- mobile/android/app/src/main/java/com/sqldpass/app/BillingPlugin.kt 작성
- Google Play Billing Library v6 직접 호출
- 기존 코드 변경 없음, 가장 깔끔
- 작업량: 1~2일 (Java/Kotlin 익숙하면 0.5일)
- 참고: https://developer.android.com/google/play/billing/integrate

**옵션 B — Capacitor 6 으로 다운그레이드 + `@codetrix-studio/capacitor-google-auth`**
- mobile/package.json 의 `@capacitor/*` 를 ^6.0.0 으로
- Google Sign-In 도 같이 묶어서 처리 가능
- 단 Play Billing 전용 플러그인은 별도로 찾아야 함 (예: `cordova-plugin-purchase` wrapper)
- 작업량: 0.5일 + 호환성 검증

**옵션 C — Capacitor 8 업그레이드 + `@capacitor-firebase/authentication` + Play Billing 플러그인**
- 최신 스택. Firebase 의존 시작.
- Cap 8 의 breaking change 검토 필요
- 작업량: 1일 + 검증

**추천**: 옵션 A. 커스텀 플러그인이 한 번 만들면 의존성 없고, 우리가 쓰는 인터페이스만 정확히 구현하면 됨.

---

## 2. 로컬 빌드 환경

### 2-1. JDK 17 설치 (필수 — 현재 11)

Windows:
- https://adoptium.net 에서 Temurin 17 (LTS) MSI 다운로드 → 설치
- 환경변수 `JAVA_HOME` 을 JDK 17 경로로 갱신
- 확인: PowerShell 에서 `java -version` 이 `openjdk 17` 이 나와야 함

Capacitor 7 + Android Gradle Plugin 8 은 JDK 17 이 강제. 11 로 빌드하면 부팅 단계에서 에러.

### 2-2. Android Studio 설치

- https://developer.android.com/studio 에서 다운로드
- 설치 시 **Android SDK Platform 34 (Android 14)** + **Android SDK Build-Tools** 같이 체크
- 첫 실행 시 SDK 경로 메모해둘 것 (예: `C:\Users\admin\AppData\Local\Android\Sdk`)

### 2-3. 환경변수

```
ANDROID_HOME=C:\Users\admin\AppData\Local\Android\Sdk
PATH 에 %ANDROID_HOME%\platform-tools 추가
```

확인: `adb --version` 이 동작하면 OK.

---

## 3. Google Play Console 셋업

### 3-1. 개발자 계정 등록 ($25 일회 결제)

- https://play.google.com/console
- 본인 인증 (여권 또는 신분증)
- 결제 후 등록 완료까지 최대 48시간 (보통 즉시)

### 3-2. 앱 생성

- Console > "앱 만들기"
- 앱 이름: `문어CBT`
- 기본 언어: 한국어
- 앱/게임: 앱
- 무료/유료: 무료 (인앱 결제는 별도)
- 정책 동의: 체크
- → 생성 후 **패키지명** 을 `com.sqldpass.app` 으로 고정 (`mobile/capacitor.config.ts` 의 `appId` 와 일치)

### 3-3. 인앱 상품 (SKU) 등록

Console > 수익 창출 > 상품 > 인앱 상품 > "상품 만들기" 3번:

| 상품 ID | 이름 | 가격 | 설명 |
|---|---|---|---|
| `iap_thunder` | 문어CBT 3일 이용권 | 3,900원 | 3일 동안 프리미엄 회차 풀이 |
| `iap_one_month` | 문어CBT 한달 이용권 | 9,900원 | 30일 동안 프리미엄 + 광고 제거 |
| `iap_unlimited` | 문어CBT 평생 무제한 이용권 | 29,900원 | 평생 + PDF 다운로드 |

상품 ID 는 환경변수로 오버라이드 가능 (`PLAY_BILLING_SKU_*`). 위 ID 와 백엔드 `application.yaml` 의 `product-id-mapping` 이 일치해야 함.

각 상품 "활성" 으로 변경.

### 3-4. 서비스 계정 발급 (영수증 검증용)

Console > 설정 > API 액세스:
1. "프로젝트 연결" → 새 GCP 프로젝트 또는 기존 sqldpass GCP 프로젝트 연결
2. "서비스 계정 만들기" → IAM 페이지로 이동 → 서비스 계정 생성 (이름 예: `sqldpass-play-billing`)
3. 역할: **Service Account User** + **Pub/Sub Publisher**
4. 키 생성 → JSON 다운로드 → 안전하게 보관 (Git 절대 금지)
5. Console 로 돌아와 서비스 계정에 권한 부여:
   - **재무 데이터 보기** (필수)
   - **앱 정보 및 다운로드 가능한 보고서 관리** (선택)

JSON 파일을 운영 백엔드 컨테이너에 마운트하고 환경변수로 경로 주입:
```
PLAY_BILLING_SA_PATH=/etc/secrets/play-billing-sa.json
```

### 3-5. RTDN (실시간 개발자 알림) 등록

환불 자동 처리를 위한 webhook.

1. GCP Console > Pub/Sub > 토픽 만들기 (이름: `play-billing-rtdn`)
2. Console > 수익 창출 > 수익 창출 설정 > 실시간 개발자 알림:
   - 토픽 이름: `projects/<gcp-project>/topics/play-billing-rtdn`
   - "테스트 알림 보내기" 로 검증
3. Pub/Sub 콘솔에서 push subscription 생성:
   - 엔드포인트: `https://api.sqldpass.com/api/webhook/play-billing/rtdn?token=<랜덤시크릿>`
   - 인증: 일단 비활성 (운영 격상 시 OIDC 토큰 검증)
4. 그 시크릿을 백엔드 환경변수로:
```
PLAY_BILLING_RTDN_SECRET=<위에서_생성한_랜덤_시크릿>
```

---

## 4. Google Cloud OAuth 클라이언트 (Android Sign-In 용)

> **이 단계는 Google 로그인을 앱에서 쓸 때만 필요.** 안 쓰면 건너뛰어도 됨.

### 4-1. 키스토어 SHA-1 미리 확인

키스토어 생성은 Step 5 에서 하지만, OAuth 등록을 위해 SHA-1 지문이 먼저 필요. 임시로 디버그 키 SHA-1 부터 등록하고, 릴리스 키 SHA-1 은 Step 5 후 추가:

```powershell
# 디버그 키 SHA-1 (Android Studio 설치 후)
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore `
  -alias androiddebugkey -storepass android -keypass android
```

### 4-2. OAuth 2.0 클라이언트 ID 등록

GCP Console > API 및 서비스 > 사용자 인증 정보 > 사용자 인증 정보 만들기 > OAuth 클라이언트 ID:

- 애플리케이션 유형: **Android**
- 패키지 이름: `com.sqldpass.app`
- SHA-1 인증서 지문: Step 4-1 에서 확인한 디버그 SHA-1
- 생성 → 클라이언트 ID 복사

백엔드 환경변수:
```
GOOGLE_OAUTH_ANDROID_CLIENT_ID=<위에서_복사한_안드로이드_클라이언트_ID>
```

릴리스 빌드용 SHA-1 은 Step 5 완료 후 같은 OAuth 클라이언트의 "SHA-1 인증서 지문 추가" 로 같이 등록 (디버그·릴리스 둘 다 등록해두면 양쪽 빌드에서 로그인 가능).

---

## 5. 키스토어 생성 + 첫 빌드 검증

### 5-1. 릴리스 키스토어 생성 (1회만)

```powershell
keytool -genkeypair -v `
  -keystore $env:USERPROFILE\sqldpass-release.keystore `
  -alias sqldpass `
  -keyalg RSA -keysize 2048 -validity 10000
```

비밀번호 입력 후 keystore 파일 생성. **이 파일과 비밀번호 절대 분실 금지** — 분실 시 Play Store 에 같은 앱으로 다음 버전 못 올림 (Play App Signing 사용 시 구제 가능하지만 복잡).

릴리스 SHA-1 확인:
```powershell
keytool -list -v -keystore $env:USERPROFILE\sqldpass-release.keystore -alias sqldpass
```
→ 출력된 SHA-1 을 Step 4-2 의 OAuth 클라이언트에 추가 등록.

### 5-2. mobile/android 빌드 설정

`mobile/android/app/build.gradle` 의 `android.signingConfigs` 에 release 키 추가:
```groovy
signingConfigs {
    release {
        storeFile file(System.getenv("SQLDPASS_KEYSTORE_PATH"))
        storePassword System.getenv("SQLDPASS_KEYSTORE_PASS")
        keyAlias "sqldpass"
        keyPassword System.getenv("SQLDPASS_KEY_PASS")
    }
}
buildTypes {
    release {
        signingConfig signingConfigs.release
        minifyEnabled true
        ...
    }
}
```

환경변수에 키스토어 경로/비밀번호 주입 (`.env` 또는 시스템 변수).

### 5-3. Capacitor sync + 빌드

```powershell
cd mobile
npx cap sync android
npx cap open android
```

Android Studio 가 열림. 첫 실행 시 Gradle 동기화 (5~10분). 끝나면:
- **Build > Build Bundle(s) / APK(s) > Build APK(s)** 로 디버그 APK
- **Build > Generate Signed Bundle / APK** 로 릴리스 AAB (Play Store 업로드용)

### 5-4. 에뮬레이터 또는 실기기 검증

- Android Studio > Device Manager 에서 Pixel 6 (API 34) 같은 에뮬레이터 생성
- "Run app" 으로 설치 → sqldpass.com 이 standalone 으로 로드되는지
- **첫 부트 splash + 콘텐츠 다운로드 진행률** 보임
- 비행기 모드 → 회차 진입 → 풀이 가능 검증
- Google 로그인 → JWT 발급 → 풀이 → 제출 → 백엔드 sync 확인

### 5-5. Play Billing 라이선스 테스트

- Play Console > 설정 > 라이선스 테스트 > 본인 Gmail 등록
- 내부 테스트 트랙에 AAB 업로드 (Step 7-1)
- 라이선스 계정으로 설치 → 결제 시 "테스트 카드" 옵션 노출 → 실제 청구 없이 검증

---

## 6. Play Store 리스팅 자료

### 6-1. 그래픽 자산

| 자산 | 사양 | 위치 |
|---|---|---|
| 앱 아이콘 | 512×512 PNG | Console > 메인 스토어 등록정보 |
| 피처 그래픽 | 1024×500 PNG/JPG | 같은 곳 |
| 휴대전화 스크린샷 | 16:9 비율 1080×1920, 2~8장 | 같은 곳 |

스크린샷 추천 구성:
1. 홈/대시보드 — "오늘의 문제"
2. 모의고사 목록 — 회차 카드들
3. 풀이 화면 — 4지선다 + 타이머
4. 결과 화면 — 점수 + 합격/불합격 + 과목별 점수
5. 오답노트
6. (선택) 오프라인 풀이 배지

### 6-2. 메타 데이터 (한국어)

- **앱 이름** (50자): `문어CBT - SQLD 정처기 컴활 무료 모의고사`
- **짧은 설명** (80자): `SQLD·정처기·컴활·ADsP CBT 모의고사 무료. 오프라인 풀이 지원, 자동 채점.`
- **자세한 설명** (4000자): 자격증별 콘텐츠 + 기능 + FAQ. 기존 `frontend/src/app/page.tsx` 의 SEO 카피 재활용 추천.

### 6-3. 콘텐츠 등급

Console > 정책 > 콘텐츠 등급 설문:
- 카테고리: **참고서/교육**
- 폭력·성·약물 등: 모두 "없음"
- 결과: 전체 이용가 (3+) 예상

### 6-4. 데이터 보안 양식

수집 항목 정직하게 신고 (사용자 이메일·푸시 토큰·결제 등):
- 개인 정보: 이름(닉네임), 이메일 주소
- 금융 정보: 결제 정보 (Google Play Billing 처리)
- 앱 활동: 앱 내 활동(풀이 기록), 검색 기록

### 6-5. 개인정보처리방침 + 약관

이미 운영 중 — `https://www.sqldpass.com/privacy`, `https://www.sqldpass.com/terms` 그대로 등록.

### 6-6. 인앱 결제 신고

Console > 정책 > 앱 콘텐츠 > "광고": 광고 표시함 (AdSense)
Console > 정책 > 앱 콘텐츠 > "타깃층 및 콘텐츠": 13세 이상

---

## 7. 출시 단계

### 7-1. 내부 테스트 트랙

Console > 테스트 > 내부 테스트:
- 새 출시 만들기 > AAB 업로드 (Step 5-3 산출물)
- 테스터 그룹 만들기 (본인 + 신뢰할 수 있는 1~5명)
- 검토 시작 → 보통 2~24시간

본인 Gmail 로 가입한 안드로이드 폰에서 테스트 링크로 설치 → 위 5-4, 5-5 검증 다시.

### 7-2. 비공개 테스트 (선택)

신뢰 그룹을 20명 이상 모아 1~2주 테스트. Play 의 "프로덕션 출시 전 안정성 권장 단계".

### 7-3. 프로덕션 출시

- 모든 정책 양식 완료 + 자산 등록 + 결제 SKU 활성 확인
- 출시 만들기 > 모든 사용자 대상 100% rollout
- Google 심사 보통 1~7일 (첫 출시는 길게 걸림, 14일 사례도 있음)
- 심사 통과 시 Play Store 검색에 노출됨

---

## 백엔드 환경변수 종합 (운영 배포 시 주입)

```bash
# Google OAuth (안드로이드 네이티브 ID 토큰)
GOOGLE_OAUTH_ANDROID_CLIENT_ID=<Step 4-2 에서 발급>

# Play Billing
PLAY_BILLING_PACKAGE_NAME=com.sqldpass.app
PLAY_BILLING_SA_PATH=/etc/secrets/play-billing-sa.json   # Step 3-4 JSON 마운트 경로
PLAY_BILLING_RTDN_SECRET=<Step 3-5 에서 생성한 랜덤 시크릿>
# SKU 매핑 (기본값 그대로면 생략 가능)
PLAY_BILLING_SKU_THREE_DAY=iap_thunder
PLAY_BILLING_SKU_ONE_MONTH=iap_one_month
PLAY_BILLING_SKU_UNLIMITED=iap_unlimited
```

프론트엔드 빌드 시:
```bash
NEXT_PUBLIC_PLAY_BILLING_SKU_THREE_DAY=iap_thunder
NEXT_PUBLIC_PLAY_BILLING_SKU_ONE_MONTH=iap_one_month
NEXT_PUBLIC_PLAY_BILLING_SKU_UNLIMITED=iap_unlimited
```

---

## 막힐 만한 포인트

| 증상 | 원인 | 해결 |
|---|---|---|
| Gradle build fails: "JDK 17 required" | JDK 11 로 빌드 | Step 2-1 |
| 앱이 빈 화면 + 로그에 ERR_CONNECTION_REFUSED | server.url 도달 불가 | sqldpass.com 운영 확인 |
| Google Sign-In "Error 10 DEVELOPER_ERROR" | OAuth 클라이언트의 SHA-1 누락 | Step 4-2, 5-1 SHA-1 둘 다 등록 |
| Play Billing "ITEM_UNAVAILABLE" | SKU 미활성 또는 트랙 미배포 | Step 3-3 활성, 7-1 내부 테스트 트랙 배포 |
| RTDN webhook 안 옴 | Pub/Sub topic 권한 | Step 3-4 서비스 계정에 Pub/Sub Publisher 부여 |
| `verifyPlayBilling` 실패 | SA JSON 권한 / 토큰 만료 | Step 3-4 의 권한 + JSON 만료 (1년) 확인 |

---

## 부록: FCM 푸시 추가 (v1.1+)

출시 후 추가하려면:

1. Firebase Console 에서 새 프로젝트 → Android 앱 추가 (패키지명 + SHA-1)
2. `google-services.json` 다운로드 → `mobile/android/app/` 에 배치 (Git 무시)
3. mobile/ 에 `@capacitor/push-notifications` 설치
4. 백엔드:
   - `firebase-admin` 의존성 추가
   - `device_token` 테이블 + 마이그레이션
   - `DeviceController` (`/api/devices/register`, `/api/devices/unregister`)
   - `FcmSender` 서비스 + 발송 hook (`AdminMockExamController` 신규 회차 등록 시 등)
5. 프론트엔드:
   - 앱 부팅 시 권한 요청 + 토큰 등록
   - 알림 데이터 페이로드의 딥링크 처리

푸시는 사용자 재방문률을 1.5~2배로 올리는 것으로 알려져 있어, MVP 안정화 후 1순위로 추가하면 좋다.
