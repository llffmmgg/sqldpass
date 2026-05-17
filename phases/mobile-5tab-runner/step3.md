# Step 3 — MainActivity 5탭 + 홈 탭

## 배경

기존 5탭은 `홈/모의고사/오프라인/기록/내 정보`. 사용자가 원하는 새 셋업은 `홈/모의고사/기출복원/문제풀기/대시보드`. "내 정보" 탭이 사라지므로 로그인/로그아웃은 홈 헤더 우측 아이콘으로 옮긴다. PASS+ 구매 카드는 홈에 남기고 대시보드에도 한 번 더 노출.

전제: Step 1·2 완료.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app`

## 변경 대상

- 수정: `MainActivity.kt`
- 신규: `ui/home/HomeScreen.kt` (MainActivity 의 홈 코드 분리)

## 변경 내용

`MainActivity.kt`:
- `tabs` 를 5개로 교체:
  - `홈` (Icons.Outlined.Home)
  - `모의고사` (Icons.Outlined.Quiz)
  - `기출복원` (Icons.Outlined.History)
  - `문제풀기` (Icons.Outlined.EditNote 또는 Edit)
  - `대시보드` (Icons.Outlined.Insights 또는 BarChart)
- 분기 `when (selected)` 에 5개 케이스: HomeScreen / MockExamTab / PastExamTab / SolveTab / DashboardTab. 후속 step 에서 채워질 탭은 본 step 에선 `PlaceholderTab(title)` 컴포저블로 1줄 자리만 차지.
- 기존 `OfflineScreen`, `HistoryScreen`, `MyScreen` 컴포저블 제거(또는 HomeScreen 통합 후 삭제).

`ui/home/HomeScreen.kt`:
- `BrandHeader` + 우측 정렬 IconButton(`Outlined.Login` 또는 `AccountCircle`) → 로그인 안 됨 시 클릭 = `onLogin()`, 로그인됨 시 long-press 또는 같은 버튼 클릭 = 메뉴(로그아웃).
- 기존 ActionCard 2개(`오늘 바로 풀기` → `onSync()`, `PASS+ 모의고사` → `onPurchase("iap_one_month")`) 유지.
- `state.message` 의 StatusCard 유지.

## Acceptance Criteria

1. `:app:assembleDebug` 통과.
2. 앱 실행 시 하단 탭 라벨이 정확히 `홈 / 모의고사 / 기출복원 / 문제풀기 / 대시보드`.
3. 홈 헤더 우측 아이콘으로 로그인/로그아웃 가능.
4. 기출복원·문제풀기·대시보드는 placeholder 라도 빈 화면 진입 가능(NPE 없이).

## 금지 사항

- 로그인을 별도 모달 다이얼로그로 만들지 마라. 이유: Compose dialog 까지 도입할 필요 없음 — 기존 `launcher.launch(authManager.signInIntent())` 흐름 그대로 헤더 아이콘에 연결.
- 라이브러리 추가 금지(material-icons-extended 는 이미 dep 에 있음).

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 3 `completed`.
- 실패: 3회 시도 후 `error` + `error_message`.
