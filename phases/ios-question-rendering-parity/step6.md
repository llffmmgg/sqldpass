# Step 6 — 빌드 & 시뮬레이터 스크린샷 검증 (macOS 수동)

## 배경

Step 1~5 의 코드 변경은 Windows 셸 환경에서 작성되었지만 iOS 빌드/시뮬레이터 실행은 macOS Xcode 환경에서만 가능하다 (AGENTS.md: "iOS 작업은 macOS 셸에서만 가능하다"). 본 step 은 사용자가 macOS 환경에서 직접 수행하는 검증 안내.

## 작업 디렉터리

```
ios/  (macOS 셸에서)
```

## 검증 절차

### 1. (필요 시) 프로젝트 재생성

`SimpleSyntaxHighlighter.swift` 같은 신규 파일이 xcodegen 글롭에 자동 포함되지 않는 경우만:

```bash
cd ios
~/bin/xcodegen generate
sed -i '' 's/objectVersion = 77;/objectVersion = 56;/' Sqldpass.xcodeproj/project.pbxproj
```

(Xcode 16.2 이상이면 sed 패치 생략 가능)

### 2. 빌드

```bash
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

성공 시 `** BUILD SUCCEEDED **`.

빌드 실패 시 가장 흔한 원인:
- Step 4 의 `Color.appCodeInline` / `appCodeInlineFG` 토큰이 Step 2 코드에서 참조되는데 토큰 정의가 누락 → `Color+Tokens.swift` 확인.
- Step 5 의 `SimpleSyntaxHighlighter` 파일이 xcodegen 글롭에 안 잡힌 경우 → `~/bin/xcodegen generate` 재실행.
- Swift 버전 차이로 `AttributedString.runs[range].font` set 문법 미지원 → 빌드 출력 확인 후 fallback.

### 3. 시뮬레이터 실행 + 스크린샷

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
```

### 4. 화면별 검증 (각각 스크린샷 저장)

#### 4.1 문제 화면 (SQL 코드블록 포함 문제)
- 펜스(```sql) 가 있는 문제 → 코드블록이 다크 톤 배경에 SELECT/FROM 등 emerald, `'문자열'` amber, `--` 주석 muted, 숫자 blue 로 색 분리되어 표시.
- 가로 80 자 넘는 한 줄 SQL → 잘리지 않고 좌→우 스와이프로 가로 스크롤.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-1-question-codeblock.png
```

#### 4.2 보기(선택지) 화면
- 보기 안 `` `member_id` `` 인라인 코드가 본문과 시각적으로 분리 (모노 + 다크 배경 + amber 톤).
- 정처기 보기처럼 보기 안에 여러 라인 코드가 있는 경우 줄바꿈 보존.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-2-options-inline.png
```

#### 4.3 해설 화면 (Solo)
- 해설 안 마크다운 표가 가로 스크롤 가능.
- 해설 안 코드블록도 문제 화면과 동일 톤.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-3-solo-explanation.png
```

#### 4.4 결과 화면 (AnswerReviewRow 펼침)
- 결과 검토 화면에서 문제 카드 펼침 시 같은 코드블록이 동일 톤으로 렌더.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-4-result-review.png
```

#### 4.5 오답노트 다시풀기 시트 (WrongAnswerRetrySheet)
- 시트 열었을 때 본문이 plain text 가 아닌 마크다운/코드블록 정상 렌더.
- 채점 후 해설도 동일.
- 펜스 없는 평문 SQL 문제도 자동 펜싱되어 코드블록으로 보임 (Step 1 휴리스틱).
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-5-wrong-answer-sheet.png
```

#### 4.6 보기 줄바꿈 4 케이스 회귀 체크
- (a) `①첫번째\n②두번째\n③세번째\n④네번째` — 기존 정상 케이스 그대로 4 줄.
- (b) 한 보기 안에 여러 라인 코드 — Step 2 의 \n 보존으로 줄바꿈 유지.
- (c) `1. 첫번째 2. 두번째 3. 세번째 4. 네번째` 한 줄 — Step 1 의 normalizeOptionMarkers 로 4 줄로 분리.
- (d) 본문 끝에 `①` 가 바로 이어 붙은 경우 — Step 1 의 `①` 단독 매치 정규화로 분리.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-6-option-newlines.png
```

#### 4.7 다크 모드 회귀
- 시뮬레이터 Appearance → Dark 전환.
- 코드블록 톤은 라이트와 동일 (모드 불변 정책).
- 본문/보기/해설/표는 `appTextPrimary` 가 다크 대비로 자동 적응.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-7-dark-mode.png
```

#### 4.8 Dynamic Type 회귀
- 시뮬레이터 Settings → Accessibility → Display & Text Size → Larger Text +2 단계 키움.
- 코드블록 라인이 크기 따라 커지고 페이지 너비 좁아져도 가로 스크롤로 흡수.
```bash
xcrun simctl io booted screenshot /tmp/sqldpass-8-dynamic-type.png
```

### 5. 종합 확인

위 8 스크린샷 + 사용자 육안 확인:
- 가독성: 코드블록 다크톤 + 키워드 색 분리로 긴 SELECT 문이 시각적으로 파싱됨.
- 가로 스크롤: 모든 코드블록/표가 잘리지 않고 좌우 스와이프 가능.
- 줄바꿈: 보기 4 케이스 모두 줄바꿈 유지.
- 오답노트: 마크다운/코드블록 정상 렌더.
- 다크 모드/Dynamic Type: 회귀 없음.

## Acceptance Criteria

1. `xcodebuild ... build` `** BUILD SUCCEEDED **`.
2. 시뮬레이터 앱 정상 실행.
3. 8 스크린샷 모두 의도된 렌더링 — 사용자 육안 확인.
4. 회귀 0 (기존 정상 화면들 무변경).

## 금지 사항

- 검증 중 발견된 문제를 본 step 안에서 코드 수정으로 해결하지 마라. **이유**: Step 6 은 검증 전용. 문제 발견 시 Step 1~5 의 해당 step `error` + 사용자에게 보고 후 신규 fix step 발행.
- `xcrun simctl io booted recordVideo` 같은 무거운 캡처를 본 검증에 사용하지 마라. **이유**: 스크린샷만으로 충분.

## Status 규칙

- 성공: `completed` + summary "macOS 빌드 OK + 8 스크린샷 검증 (문제/보기/해설/결과/오답노트/줄바꿈/다크/Dynamic Type) 모두 정상".
- 실패: 사용자 보고에 따라 Step 1~5 중 해당 step `error` 표기 + 신규 fix step 발행.
- 사용자 개입 필요 시: `blocked` + `blocked_reason` (예: "iOS 시뮬레이터에서 보기 인라인 코드 배경이 box 모양 대신 underline 으로 표시됨 — iOS 17/18 차이").
