# Step 3 — Android 단일 채점 풀이 화면 신규 (android-solo-solve-screen)

## 배경

현재 Android `mobile/app/.../ui/runner/QuestionRunnerScreen.kt` 는 **모의고사 응시 모드(여러 문항을 한꺼번에 풀고 마지막에 일괄 제출)** 만 구현돼 있고, 웹 `/solve` 의 **단일 채점 모드(1문제씩 즉시 채점 → 정답 공개 → 다음)** 가 아예 없다. 사용자는 이 흐름을 모바일에서도 원하며, 양 플랫폼에서 동일하게 동작해야 한다.

본 step 은 모의고사 풀이를 건드리지 않고 **신규 화면**(`SoloSolveScreen.kt`)을 추가한다. 두 화면이 공유하는 부분(Markwon 렌더, OptionRow 시각 등) 은 가능한 한 `ui/common/` 으로 추출하되, 본 step 범위는 단일 채점 화면과 그 부속 컴포넌트만.

`docs/SOLVE_SCREEN_SPEC.md` (step 1 산출물) 의 정보 위계 / 상태 다이어그램 / 햅틱 / 인터랙션을 그대로 따른다.

## 작업 디렉터리

```bash
cd mobile
```

## 변경 대상

| 파일 | 변경 |
|---|---|
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/SoloSolveScreen.kt` | 신규 — 단일 채점 풀이 화면 메인 |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/SoloSolveViewModel.kt` | 신규 — 상태 머신 (idle / selected / submitting / revealed) + 즉시 채점 + 다음 문제 로드 |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/components/SolveOptionRow.kt` | 신규 — 옵션 1개 (미답/선택/정답공개 정답/정답공개 오답/정답공개 무선택) 5가지 시각 상태. animateColorAsState, press scale, bounce, shake-x |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/components/SolveProgressHeader.kt` | 신규 — 상단 헤더(종료·진행도·북마크·신고) + 진행 바 |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/components/SolveExplanationCard.kt` | 신규 — 정답 공개 시 모범답안/키워드/해설 카드 묶음 |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/components/SolveBottomActionBar.kt` | 신규 — 하단 [이전][정답 확인 / 다음 문제] 액션바. navigationBarsPadding + imePadding |
| `mobile/app/src/main/java/com/sqldpass/app/ui/solve/components/AnonQuotaChip.kt` | 신규 — 비회원 일일 한도 칩 (한도 조회 성공 시만) |
| `mobile/app/src/main/java/com/sqldpass/app/nav/SqldpassNav.kt` | `SqldpassRoute.SoloSolve` 라우트 추가 (subjectId 인자) |
| `mobile/app/src/main/java/com/sqldpass/app/MainActivity.kt` | NavHost 에 SoloSolve composable 등록 + 홈 "10문제 풀기" CTA 가 본 라우트로 진입하도록 변경 |
| `mobile/app/src/main/java/com/sqldpass/app/ui/AppViewModel.kt` | `startSoloSolve(subjectId: Long)` 메서드 추가(`navController.navigate(SoloSolve)` 트리거용 이벤트 + 자격증/과목 컨텍스트 유지) |

## 상태 머신 (SoloSolveViewModel)

```kotlin
sealed interface SoloSolveState {
    data object Loading : SoloSolveState
    data class Idle(
        val question: Question,
        val solvedCount: Int,
        val correctCount: Int,
        val selectedOption: Int? = null,
        val answerText: String = "",
    ) : SoloSolveState
    data class Submitting(val prev: Idle) : SoloSolveState
    data class Revealed(
        val question: Question,
        val detail: QuestionDetail,
        val selectedOption: Int?,
        val answerText: String,
        val isCorrect: Boolean,
        val solvedCount: Int,
        val correctCount: Int,
    ) : SoloSolveState
    data class SessionComplete(
        val solvedCount: Int,
        val correctCount: Int,
        val subjectName: String,
    ) : SoloSolveState
    data class Error(val message: String, val retry: (() -> Unit)?) : SoloSolveState
}
```

핵심 액션:

- `selectOption(num: Int)` — Idle 또는 Selected 상태에서만. 햅틱 light.
- `setAnswerText(text: String)` — SHORT/DESCRIPTIVE 만.
- `submit(force: Boolean = false)` — Idle → Submitting → Revealed. 클라이언트 측 채점(`isCorrect`) 즉시 표시 + `POST /api/solves` 백그라운드 호출(실패 시 step 5 의 큐잉 활용). 햅틱 success/warning.
- `next()` — Revealed → 다음 Question fetch → Idle. solvedCount 가 10 도달하면 SessionComplete.
- `exit(confirmed: Boolean)` — 미답 + 답안 있음이면 확인 다이얼로그. 확정 시 화면 dismiss.

세트 크기는 `SET_SIZE = 10` 상수.

## 인터랙션 디테일 (SOLVE_SCREEN_SPEC.md 참조)

- **옵션 1회 탭**: 선택. 햅틱 `HapticFeedbackType.TextHandleMove`.
- **옵션 더블탭** (revealed 전): 선택 + 즉시 `submit()`. 햅틱 `LongPress`.
- **"정답 확인" 탭**: `submit()`. 응답 옵션이 비어있으면 비활성.
- **정답 공개 시**: 정답 옵션 = `success` 색 border + `CheckCircle` 아이콘. 선택한 오답 = `danger` 색 border + `Cancel` 아이콘 + `shake-x` 애니메이션 (200ms, ±4dp). 나머지 옵션 = opacity 50%.
- **"다음 문제"**: 진행 바 200ms easeOut 전환. 새 question 카드는 `AnimatedContent` 로 좌 → 우 슬라이드 인.

## SolveOptionRow 시각 상태 표

| 상태 | container | border | content | trailing icon | scale anim |
|---|---|---|---|---|---|
| 미답 | `surface` | `outline` (1dp) | `onSurface` | `RadioButtonUnchecked` | - |
| 선택됨 (revealed 전) | `primaryContainer` | `primary` (2dp) | `onPrimaryContainer` | `CheckCircle` (primary) | press 0.97 + bounce 1.04 |
| 정답 공개 — 정답 옵션 | `successContainer` (Color.kt 의 `--success` 톤) | `success` (2dp) | `onSuccessContainer` | `CheckCircle` (success) | correct-reveal 0.25s pulse |
| 정답 공개 — 선택한 오답 | `dangerContainer` | `danger` (2dp) | `onDangerContainer` | `Cancel` (danger) | shake-x 0.3s |
| 정답 공개 — 무선택 옵션 | `surface` (opacity 50%) | `outline` (1dp) | `onSurface` muted | `RadioButtonUnchecked` | - |

`successContainer`/`dangerContainer` 토큰이 `Color.kt` 에 없으면 `success.copy(alpha = 0.12f)` 패턴(이 정도 alpha 는 MEMORY 의 "/5~/10 옅은 배경 금지" 에 해당 안 함 — 명확한 정답 시각 표시는 의미 있음. 단 `backdrop-blur` / `glow` 절대 금지).

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
2. `cd mobile; .\gradlew.bat :app:testDebugUnitTest` → 기존 테스트 통과 (신규 unit test 는 본 step 에 추가하지 않음 — UI 중심).
3. Home → "10문제 풀기" CTA 탭 → 과목 선택 시트(또는 기본 자격증 사용) → SoloSolveScreen 진입.
4. 옵션 탭 시 시각/햅틱, 더블탭 시 즉시 채점.
5. 정답 공개 후 해설 카드(모범답안/키워드/해설) 노출. SHORT/DESCRIPTIVE 인 경우만 모범답안/키워드 표시.
6. 10문제 풀이 완료 시 SessionComplete 상태 — 본 step 에서는 간단 카드(점수/정답률/"같은 10문 다시"/"새 10문")만. 세션 완료 결과 풍부화는 별도 phase.
7. 매직 넘버 없음 — 모든 dp 값은 `SqldSpacing` / `SqldRadius` 토큰 참조.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

스크린샷(에뮬레이터 또는 실기기):

- 미답 상태 1장
- 선택됨 상태 1장
- 정답 공개 (정답) 1장
- 정답 공개 (오답 + 해설 카드 펼침) 1장

`scripts/screenshots/solo-solve-*.png` 에 저장 (디렉터리 없으면 신설).

## 금지 사항

- 기존 `QuestionRunnerScreen.kt` 를 수정하지 마라. 이유: 모의고사 응시 모드 회귀 위험. 본 step 은 신규 화면만.
- 옵션 컴포넌트에 `backdrop-blur` 또는 `drop-shadow glow` 효과를 넣지 마라. 이유: MEMORY 의 `feedback_no_ai_blur_effects` 위반. Supabase 단단한 톤 유지.
- `MaterialTheme.colorScheme.primary` 이외의 자격증 액센트(amber, sky 등) 를 정답/오답 색으로 쓰지 마라. 이유: 정답/오답은 시맨틱(success/danger) 고정. 자격증 액센트는 헤더/칩에만.
- 풀이 제출(`POST /api/solves`) 응답을 기다리는 동안 옵션 비활성화하지 마라. 이유: 즉시 채점 UX 가 깨짐. 클라이언트 측 `isClientSideCorrect()` 로 즉시 표시하고 서버 호출은 백그라운드(실패 시 step 5 의 큐잉).
- `SoloSolveScreen` 안에서 `LaunchedEffect` 만으로 SET_SIZE 보다 많은 문제를 prefetch 하지 마라. 이유: 트래픽 + 무한 루프 위험. 다음 문제는 onNext 시점에만 fetch.
- 마지막 문항 채점 후 자동으로 SessionComplete 로 점프하지 마라. 이유: 사용자가 "다음 문제" 버튼을 직접 눌러서 결과 화면으로 가는 흐름이 웹과 동일.

## Status 규칙

- 성공: index.json step 3 `completed`, summary 에 빌드 결과 + 스크린샷 경로.
- 실패: 3회 시도 후 `error` + `error_message`.
