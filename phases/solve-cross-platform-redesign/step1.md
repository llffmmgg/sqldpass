# Step 1 — 공통 디자인 토큰 + 풀이 화면 UX 스펙 (shared-tokens-and-spec)

## 배경

본 phase 가 양 플랫폼(`mobile/`, `ios/`) 의 풀이 화면을 처음부터 새로 짜려면 **단일 진실 원천(spec + 토큰)** 이 먼저 필요하다. 현재 상태:

- Android `mobile/app/.../ui/theme/` 에 `Color.kt`, `Theme.kt`, `Type.kt` 만 있고 **Spacing/Radius 가 없어 매직 넘버 산재**(`QuestionRunnerScreen.kt` 의 `8.dp`, `10.dp`, `12.dp`, `14.dp`, `16.dp`, `20.dp`, `28.dp`, `48.dp`, `64.dp` 등).
- iOS `ios/Sqldpass/Core/DesignSystem/` 에 `Spacing.swift`, `Radius.swift`, `Typography.swift` 가 이미 있음 → 값 검증만.
- 웹 `frontend/src/app/globals.css` + `docs/UI_GUIDE.md` 가 디자인 시스템 본진. 모바일은 이걸 미러링.
- `docs/SOLVE_SCREEN_SPEC.md` 는 없음 — step 2~6 이 참조할 단일 스펙.

본 step 은 코드 변경 최소(Android 토큰 파일 2개 신설 + iOS 토큰 값 점검) + 문서 1개 신설.

## 작업 디렉터리

- `mobile/app/src/main/java/com/sqldpass/app/ui/theme/`
- `ios/Sqldpass/Core/DesignSystem/` (읽기 전용 검증만)
- `docs/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `mobile/app/src/main/java/com/sqldpass/app/ui/theme/Spacing.kt` | 신규 — `object SqldSpacing { xs=4, sm=8, md=12, base=16, lg=20, xl=24, xxl=32, huge=48 }` (단위 `.dp`) |
| `mobile/app/src/main/java/com/sqldpass/app/ui/theme/Radius.kt` | 신규 — `object SqldRadius { xs=4, sm=8, md=10, base=12, lg=14, xl=16, pill=999 }` (단위 `.dp`) |
| `docs/SOLVE_SCREEN_SPEC.md` | 신규 — 풀이 화면 양 플랫폼 공통 UX 사양 (아래 본문) |
| `ios/Sqldpass/Core/DesignSystem/Spacing.swift` | 검증만. 값이 위 SqldSpacing 과 다르면 step description 에 차이 기록 후 사용자 결정 대기 |
| `ios/Sqldpass/Core/DesignSystem/Radius.swift` | 검증만. 위와 동일 |

## docs/SOLVE_SCREEN_SPEC.md 본문

다음 5개 섹션을 포함한다. 본문은 양 플랫폼 step 이 참조할 단일 사양.

### 1. 정보 위계 (위에서 아래로)

```
[상단 헤더]
  ├ 종료(X) 좌측
  ├ 진행도 "3 / 10" 중앙
  └ 북마크·신고 우측
[진행 바] 얇은 line (3dp/pt), primary 색, 200ms easeOut 전환
[비회원 한도 칩] 비로그인 + 한도 조회 성공 시만 노출
[문제 카드]
  ├ 과목 라벨 (caption, brandPrimary)
  ├ 본문 (Markdown + SVG + Table + Code, parseQuestionContent 결과)
  └ 옵션 4개 (MCQ) / 텍스트 입력 (SHORT/DESCRIPTIVE)
[하단 액션 바] safeArea 안쪽
  ├ 미답 상태: [이전] [정답 확인 (primary, disabled if no answer)]
  └ 공개 상태: [이전] [다음 문제 (primary)] — 마지막 문항이면 "결과 보기"
[정답 공개 상태 추가 영역] (옵션 4개 아래에 등장)
  ├ 정답/오답 배너 (success/danger border + ✓/✗)
  ├ 모범답안 카드 (SHORT/DESCRIPTIVE 만)
  ├ 채점 키워드 칩 (SHORT 만)
  └ 해설 카드 (primary 라벨)
```

### 2. 상태 다이어그램

```
loading → question-loaded (idle)
  ↓ 옵션 탭
selected (옵션 1개 선택됨)
  ↓ "정답 확인" 탭 또는 더블탭
submitting (1초 미만, optimistic)
  ↓ 응답
revealed (correct / incorrect 분기)
  ↓ "다음 문제"
question-loaded (다음 문항) 또는 → session-complete (마지막 끝)

추가 가능 전이:
- selected → selected (다른 옵션으로 변경, revealed 전까지만)
- revealed → previous (이전 문제로 이동, optional)
- error → retry (제출 실패 시 큐잉으로 갈무리 → idle 유지)
```

### 3. 인터랙션 매트릭스

| 입력 | 동작 |
|---|---|
| 옵션 1회 탭 | 선택 (다른 옵션 선택 해제). 햅틱 light |
| 옵션 더블탭 (revealed 전) | 선택 + 즉시 채점 한 번에 |
| "정답 확인" 버튼 탭 | 즉시 채점 (선택된 옵션 또는 입력 텍스트 기준) |
| 정답 시 | 햅틱 success(iOS .success / Android CONFIRM). 정답 옵션 success border + ✓ + animate-correct-reveal(0.25s ease) |
| 오답 시 | 햅틱 warning(iOS .warning / Android REJECT). 선택 옵션 danger border + ✗ + shake-x(0.3s). 정답 옵션 success border + ✓ |
| "다음 문제" 버튼 탭 | 다음 문항 로드. 진행 바 200ms ease 갱신 |
| 좌측 엣지 스와이프(iOS) / 시스템 뒤로(Android) | 종료 확인 다이얼로그 |
| 옵션 비활성 영역 탭 (revealed 후) | 무시. cursor: default |

### 4. 햅틱 매트릭스

| 상황 | iOS | Android |
|---|---|---|
| 옵션 선택 | `UIImpactFeedbackGenerator(style: .light)` | `HapticFeedbackType.TextHandleMove` |
| 정답 공개 | `UINotificationFeedbackGenerator().notificationOccurred(.success)` | `HapticFeedbackConstants.CONFIRM` (API 30+) / 폴백 `LongPress` |
| 오답 공개 | `.warning` | `HapticFeedbackConstants.REJECT` / 폴백 `LongPress` |
| 제출 (마지막 문항 결과 보기) | `.success` | `CONFIRM` |
| 종료 확인 destructive 탭 | `.warning` | `REJECT` |

### 5. 오프라인 큐잉 정책

1. 모든 풀이 제출(`POST /api/solves`)은 우선 로컬 큐에 enqueue.
2. enqueue 성공 후 비차단으로 서버 호출 시도. 200/201 응답 → 큐 row 의 `synced=true, serverSolveId=res.id` 갱신.
3. 네트워크 실패(timeout, no connection, 5xx) → 큐 row `synced=false` 유지. UI 는 정상 진행(다음 문제로).
4. 풀이 화면 좌하단(또는 토스트) "오프라인 — 답안 보관 중" 미니 인디케이터 노출(미동기화 row > 0).
5. 네트워크 복귀(Android `ConnectivityManager`, iOS `NWPathMonitor`) → `OfflineSyncManager.tryDrain()` 호출. 미동기화 row 를 createdAt asc 순으로 재전송. 멱등키 `clientSubmissionId` 로 서버 측 중복 방지.
6. 동기화 완료 후 UI 인디케이터 자동 해제.

### 6. 본문 렌더 정책

문제 본문(`content` 필드) 은 다음 4가지가 섞인다:

- **Markdown 텍스트** (`**bold**`, `### 헤딩`, 리스트 등)
- **Inline SVG** (`<svg>...</svg>`)
- **GFM Table** (`| col | col |\n|---|---|`) 또는 **HTML `<table>`** (혼재 — 백엔드가 정규화 안 함)
- **코드블록** (` ```sql\n... ``` `) 또는 **HTML `<pre><code>`** (혼재)

클라이언트 전처리:
1. `ensureCodeFences`: HTML `<pre><code>` → 펜스 markdown, HTML `<table>` → markdown table, inline `<code>` → 백틱.
2. `splitMarkdownSegments`: 펜스 코드 / inline SVG / `<img>` / 나머지 markdown 으로 분리.
3. 각 세그먼트 전용 렌더:
   - Markdown → Android Markwon TextView / iOS swift-markdown AttributedString
   - 코드블록 → Android CodeBlockCard / iOS CodeBlockView (mono font, 가로 스크롤)
   - Inline SVG → Android Coil+SvgDecoder / iOS SVGKit `SVGKFastImageView`
   - Image → Android Coil AsyncImage / iOS AsyncImage
   - Table → markdown 표만 처리 (HTML 표는 ensureCodeFences 에서 markdown 으로 변환됨)

Android 는 이미 위 흐름이 구현돼 있음(`text/MarkdownSegments.kt`, `text/EnsureCodeFences.kt`, `text/SqldpassMarkwon.kt`).
iOS 는 step 2 에서 구현.

## Acceptance Criteria

1. `mobile/app/src/main/java/com/sqldpass/app/ui/theme/Spacing.kt` 와 `Radius.kt` 가 신규로 존재하고, 값이 위 명세와 정확히 일치.
2. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL — 새 토큰 파일이 잘못된 import 없이 컴파일 통과(아직 사용처 없음, 단순 정의).
3. `docs/SOLVE_SCREEN_SPEC.md` 가 신규로 존재하고, 위 6개 섹션이 그대로 포함.
4. iOS `Spacing.swift`, `Radius.swift` 의 값을 본 step description 의 표와 비교한 결과를 step summary 에 기록(차이 있으면 후속 step 에서 정렬).

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## 금지 사항

- `SqldSpacing` / `SqldRadius` 를 본 step 에서 기존 화면에 적용하지 마라. 이유: 본 step 은 토큰 정의만, 사용은 step 3 부터. 한 step 의 영향 범위 폭증 방지.
- `Color.kt` 의 값을 본 step 에서 수정하지 마라. 이유: MEMORY 의 `feedback_color_token_changes` — 색 계열 변경 금지.
- `docs/SOLVE_SCREEN_SPEC.md` 의 정보 위계/햅틱/큐잉 정책을 본 step 에서 다른 값으로 바꾸지 마라. 이유: 후속 step 이 이 문서를 absolute spec 으로 참조하며, 차이가 발생하면 양 플랫폼 비대칭이 생긴다.
- iOS 토큰 파일을 본 step 에서 수정하지 마라(검증만). 이유: 검증만 하는 step 인데 코드 변경하면 macOS 빌드 필요해지고 Windows 진행 불가.

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄(Android 토큰 파일 2개 신설, 빌드 통과, iOS 토큰 값 비교 결과, 스펙 문서 작성).
- 실패: 3회 시도 후 `error` + `error_message`.
