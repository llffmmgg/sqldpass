# Step 4 — 빌드 & 시뮬레이터/실기기 스크린샷 검증 (macOS 수동)

## 배경

Step 1~3 의 코드 변경은 Windows 셸 환경에서 작성됨. iOS 빌드/시뮬레이터/실기기 검증은 macOS Xcode 에서만 가능. 본 step 은 사용자 수동 검증 안내.

## 작업 디렉터리

```
ios/  (macOS 셸에서)
```

## 절차

### 1. 프로젝트 재생성 (CustomTabBar.swift 새 파일 인식)

```bash
cd ios
~/bin/xcodegen generate
```

### 2. 빌드

```bash
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

성공 시 `** BUILD SUCCEEDED **`.

### 3. 시뮬레이터 실행

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
```

### 4. 8 체크포인트 (라이트 모드 기준)

1. **하단 탭바 직사각형 풀폭** — 알약(pill) 모양 없음. 좌→우 5 탭 균등.
2. **흰 띠 0** — 탭바 위/아래 어색한 띠 없음. 전체 흰색 연속.
3. **선택 색** — 활성 brandPrimary 에메랄드, 비활성 appTextMuted 회색.
4. **HomeView 상단 초록 헤더** — brandPrimary 가 status bar 위까지 확장 (기존 동작 유지).
5. **카드 분리** — 페이지 흰색 + 카드 흰색 동색이지만 appBorder 보더로 분리 유지.
6. **다크 모드 회귀** — 다크 전환 시 탭바 #121212, 카드 #2e2e2e, 분리 유지.
7. **탭 전환** — 5 탭 모두 정상 진입, NavigationStack (`pastExams`/`soloSolve`) push 정상.
8. **각 탭 화면 회귀** — HomeView KPI / 자격증 carousel, MockExamsListView, ProfileView KPI / NavigationLink, PastExamsListView, SoloHubView 모두 정상.

스크린샷 명령:
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-tabbar-${TAB}.png
```

### 5. 실기기 (사용자 디바이스) 확인

본 변경의 직접 동기였던 사용자 실기기에서도:
- floating pill 사라지고 직사각형 풀폭 바.
- 탭 위 흰 띠 없음.

## Acceptance Criteria

1. `xcodebuild ... build` `** BUILD SUCCEEDED **`.
2. 8 체크포인트 모두 만족.
3. 실기기에서 사용자 확인.

## 금지

- 검증 중 발견된 회귀를 본 step 안에서 수정 금지. 발견 시 Step 1~3 해당 step `error` + 신규 fix step.

## Status 규칙

- 성공: `completed` + summary "macOS 빌드 OK + 8 체크포인트 + 실기기 확인 OK".
- 실패: 사용자 보고에 따라 Step 1~3 중 해당 step `error` + 신규 fix step.
- 사용자 개입 필요 시: `blocked` + `blocked_reason`.
