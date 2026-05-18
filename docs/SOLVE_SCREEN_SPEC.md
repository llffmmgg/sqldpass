# Solve Screen — Mobile Cross-Platform Spec

Android(`mobile/`) 와 iOS(`ios/`) 의 풀이 화면이 따라야 할 단일 사양. 본 문서가 진실 원천이며 양 플랫폼 코드는 이 사양을 미러링한다.

핵심 원칙: **UX 100% 동일, UI 는 플랫폼 컨벤션(iOS sheet/SwiftUI, Android ModalBottomSheet/Compose)으로 다르게.**

## 0. 디자인 토큰

진실 원천: **`ios/Sqldpass/Core/DesignSystem/Spacing.swift` 와 `Radius.swift`**. Android `mobile/.../ui/theme/Spacing.kt` 와 `Radius.kt` 는 본 값을 미러링한다.

### Spacing

| 토큰 | 값 | iOS | Android |
|---|---|---|---|
| xxs | 2 | `Spacing.xxs` | `SqldSpacing.xxs` |
| xs | 4 | `Spacing.xs` | `SqldSpacing.xs` |
| sm | 8 | `Spacing.sm` | `SqldSpacing.sm` |
| md | 12 | `Spacing.md` | `SqldSpacing.md` |
| base | 16 | `Spacing.base` | `SqldSpacing.base` |
| lg | 24 | `Spacing.lg` | `SqldSpacing.lg` |
| xl | 32 | `Spacing.xl` | `SqldSpacing.xl` |
| xxl | 48 | `Spacing.xxl` | `SqldSpacing.xxl` |
| xxxl | 64 | `Spacing.xxxl` | `SqldSpacing.xxxl` |

### Radius

| 토큰 | 값 | 용도 |
|---|---|---|
| sm | 6 | 버튼, 인풋 (Supabase 사양) |
| md | 8 | 보조 카드 |
| lg | 16 | 메인 카드 |
| xl | 16 | (lg 와 동일 — 점진 정렬 중) |
| xxl | 20 | 큰 모달, 시트 |
| full | 9999 | 칩, 뱃지 |

매직 넘버는 본 토큰 외 사용 금지. 새 값이 필요하면 토큰부터 검토.

## 1. 정보 위계 (위에서 아래로)

```
[상단 헤더]
  ├ 종료(X) 좌측
  ├ 진행도 "3 / 10" 중앙
  └ 북마크·신고 우측
[진행 바] 얇은 line (3dp/pt), primary 색, 200ms easeOut 전환
[비회원 한도 칩] 비로그인 + 한도 조회 성공 시만 노출
[오프라인 큐 칩] pendingSolveCount > 0 일 때만 노출 (좌측 정렬, warning 톤)
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

## 2. 상태 다이어그램

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

## 3. 인터랙션 매트릭스

| 입력 | 동작 |
|---|---|
| 옵션 1회 탭 | 선택 (다른 옵션 선택 해제). 햅틱 light |
| 옵션 더블탭 (revealed 전) | 선택 + 즉시 채점 한 번에 |
| "정답 확인" 버튼 탭 | 즉시 채점 (선택된 옵션 또는 입력 텍스트 기준) |
| 정답 시 | 햅틱 success(iOS .success / Android CONFIRM). 정답 옵션 success border + ✓ + 0.25s reveal 애니메이션 |
| 오답 시 | 햅틱 warning(iOS .warning / Android REJECT). 선택 옵션 danger border + ✗ + shake-x(0.3s). 정답 옵션 success border + ✓ |
| "다음 문제" 버튼 탭 | 다음 문항 로드. 진행 바 200ms ease 갱신 |
| 좌측 엣지 스와이프(iOS) / 시스템 뒤로(Android) | 종료 확인 다이얼로그 |
| 옵션 비활성 영역 탭 (revealed 후) | 무시. cursor: default |

## 4. 햅틱 매트릭스

| 상황 | iOS | Android |
|---|---|---|
| 옵션 선택 | `UIImpactFeedbackGenerator(style: .light)` | `HapticFeedbackType.TextHandleMove` |
| 정답 공개 | `UINotificationFeedbackGenerator().notificationOccurred(.success)` | `HapticFeedbackConstants.CONFIRM` (API 30+) / 폴백 `LongPress` |
| 오답 공개 | `.warning` | `HapticFeedbackConstants.REJECT` / 폴백 `LongPress` |
| 제출 (마지막 문항 결과 보기) | `.success` | `CONFIRM` |
| 종료 확인 destructive 탭 | `.warning` | `REJECT` |

## 5. 오프라인 큐잉 정책

1. 모든 풀이 제출(`POST /api/solves`)은 우선 로컬 큐에 enqueue.
2. enqueue 성공 후 비차단으로 서버 호출 시도. 200/201 응답 → 큐 row 의 `synced=true, serverSolveId=res.id` 갱신.
3. 네트워크 실패(timeout, no connection, 5xx) → 큐 row `synced=false` 유지. UI 는 정상 진행(다음 문제로).
4. 풀이 화면 좌하단 "오프라인 — 답안 보관 중" 미니 인디케이터 노출(미동기화 row > 0).
5. 네트워크 복귀(Android `ConnectivityManager`, iOS `NWPathMonitor`) → `OfflineSyncManager.tryDrain()` 호출. 미동기화 row 를 createdAt asc 순으로 재전송. 멱등키 `clientSubmissionId` 로 서버 측 중복 방지.
6. 동기화 완료 후 UI 인디케이터 자동 해제.

### 멱등키 형식

- Android: `"android-${UUID}"` (기존 `AppRepository.kt:119` 패턴)
- iOS: `"ios-${UUID().uuidString}"` (Android 와 prefix 만 다르고 형식 동일)
- 백엔드 `SolveRequest.clientSubmissionId` (max 64) 는 nullable — 두 형식 모두 통과

## 6. 본문 렌더 정책

문제 본문(`content` 필드) 은 다음 4가지가 섞인다:

- **Markdown 텍스트** (`**bold**`, `### 헤딩`, 리스트 등)
- **Inline SVG** (`<svg>...</svg>`)
- **GFM Table** (`| col | col |\n|---|---|`) 또는 **HTML `<table>`** (혼재 — 백엔드가 정규화 안 함)
- **코드블록** (` ```sql\n... ``` `) 또는 **HTML `<pre><code>`** (혼재)

### 클라이언트 전처리

1. `ensureCodeFences`: HTML `<pre><code>` → 펜스 markdown, HTML `<table>` → markdown table, inline `<code>` → 백틱.
2. `splitMarkdownSegments` (또는 iOS 의 `parseQuestionContent`): 펜스 코드 / inline SVG / `<img>` / markdown table / 나머지 markdown 으로 분리.
3. 각 세그먼트 전용 렌더:

| 세그먼트 | Android | iOS |
|---|---|---|
| Markdown | Markwon TextView | `AttributedString(markdown:)` |
| 코드블록 | `CodeBlockCard` (mono, 가로 스크롤) | `CodeBlockView` (mono, 가로 스크롤) |
| Inline SVG | `Coil` + `SvgDecoder` | `SVGKit` `SVGKFastImageView` |
| Image | `Coil AsyncImage` | SwiftUI `AsyncImage` |
| Table | Markwon `TablePlugin` | `MarkdownTableView` (`ScrollView(.horizontal)` + `Grid`) |

Android 흐름은 이미 `text/MarkdownSegments.kt`, `text/EnsureCodeFences.kt`, `text/SqldpassMarkwon.kt` 에 구현돼 있음.
iOS 는 step 2 에서 구현.

## 7. 정답 옵션 시각 상태

5가지 상태 모두 양 플랫폼 동일 의미·색·아이콘.

| 상태 | container | border | 아이콘 | 모션 |
|---|---|---|---|---|
| 미답 | surface | outline 1dp | radio empty | — |
| 선택됨 (revealed 전) | primary container | primary 2dp | check filled | press 0.97 + bounce 1.04 (140ms) |
| 정답 공개 — 정답 옵션 | success tint (alpha 0.12) | success 2dp | check filled | reveal pulse 0.25s |
| 정답 공개 — 선택한 오답 | danger tint (alpha 0.12) | danger 2dp | xmark filled | shake-x 0.3s ±4dp |
| 정답 공개 — 무선택 옵션 | surface (opacity 0.5) | outline 1dp | radio empty | — |

success/danger 의 alpha 0.12 는 명시적 정답 표시 의미라 MEMORY 의 "/5~/10 옅은 배경 금지" 와 충돌 안 함. 단 `backdrop-blur`, glow `drop-shadow`, opacity pulse 는 금지.

## 8. 풀이 모드 분리

본 화면("Solo Solve") 과 모의고사 응시 화면(`MockExamRunner`) 은 **별도 화면**이다.

| 항목 | Solo Solve | Mock Exam |
|---|---|---|
| 풀이 단위 | 1문제씩 즉시 채점 | 50문제 한 번에 제출 |
| 정답 공개 | 옵션 별 즉시 시각 피드백 | 결과 화면에서 일괄 |
| 타이머 | 없음 | 있음 (분 단위 + 5분 이하 danger 색) |
| OMR 점프 | 없음 (1문제씩이라 불필요) | 있음 |
| 백그라운드 진입 | 자유롭게 | 타이머 계속 흐름, 답안 자동 저장 |
| 본 phase 범위 | ✅ 신규 구현 | ❌ 건드리지 않음 |

## 변경 이력

| 날짜 | 버전 | 변경 |
|---|---|---|
| 2026-05-18 | 1.0 | 최초 작성 (phase `solve-cross-platform-redesign` step 1) |
