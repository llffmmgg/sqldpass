# Step 2 — 공통 풀이 화면 (QuestionRunnerScreen)

## 배경

세 탭(모의고사·기출복원·문제풀기)이 동일한 "한 문제씩 보기 → 1~4 선택 → 제출" 흐름을 공유. 화면을 한 번 만들어 재사용한다. 백엔드 question `content` 는 HTML/마크다운 한 덩어리(보기 포함)이므로 `AndroidView` + `TextView` + `HtmlCompat.fromHtml` 로 1차 렌더.

전제: Step 1 의 DTO·Repository 메서드가 들어와 있다.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/runner`

## 변경 대상

- 신규: `ui/runner/QuestionRunnerScreen.kt`
- 신규: `ui/runner/QuestionResultScreen.kt`
- 신규: `ui/runner/RunnerModels.kt` (RunnerQuestion, RunnerMode, RunnerResult sealed class)

## 변경 내용

`RunnerModels.kt`:
- `enum class RunnerMode { MOCK_EXAM, PAST_EXAM, PRACTICE }`
- `data class RunnerQuestion(id, displayOrder, content, questionType)` — 호출자가 백엔드 응답을 변환해 넘김.
- `sealed interface RunnerResult` — `SolveResult(SolveResponse)` 또는 `PastExamResult(PastExamGradeResponse)`.

`QuestionRunnerScreen(questions, mode, onCancel, onSubmit: (answers) -> Unit)`:
- 상단: 진행률 바 + "n / 총 m" 표시 + 닫기 버튼.
- 본문: `AndroidView` 안에 `TextView` 만들어 `HtmlCompat.fromHtml(content, FROM_HTML_MODE_COMPACT)` 적용. 폰트 사이즈 16sp, 줄간격 8dp.
- `questionType in {"MCQ", null}` → 4개 선택지 카드(`Card` + `RadioButton` + 라벨 "①/②/③/④"). 빈 라벨로 둠 — 실제 보기 텍스트는 `content` 에 이미 포함.
- `questionType in {"SHORT_ANSWER","DESCRIPTIVE"}` → `OutlinedTextField`.
- 하단: `이전 / 다음` 버튼. 마지막 문항에선 `다음` 대신 `제출`. 제출 시 `List<SolveAnswerRequest>` 로 변환해 `onSubmit` 콜백.

`QuestionResultScreen(result, onClose, onRestart? = null)`:
- `SolveResult` 모드: 큰 점수(`correctCount / totalCount`), 정답률 %, "다시 풀기" + "닫기" 버튼.
- `PastExamResult` 모드: 합격/불합격 배너(`passed` true → `Color(0xFF22C55E)` 배경, false → `Color(0xFFEF4444)`), `passReason` 텍스트, `subjectScores` 표(과목명 + 정답률 + 과락 표시). 하단에 닫기 버튼.

## Acceptance Criteria

1. `:app:assembleDebug` 통과.
2. `QuestionRunnerScreen` 은 호출자가 `RunnerMode` 와 `questions` 만 넘기면 동작. ViewModel 의존 없음.
3. HTML 렌더가 `<pre>/<code>/<table>` 같은 태그도 깨지지 않고 적어도 텍스트는 보이는 상태(스타일링은 후속).

## 금지 사항

- WebView 도입 금지. 이유: `mobile/AGENTS.md` 의 "Do not reintroduce Capacitor or WebView" 규칙.
- HTML 렌더링 라이브러리 신규 의존성 추가 금지. 이유: 기본 `HtmlCompat` 으로 1차 만족. 라이브러리는 후속 phase.
- Compose Navigation 도입 금지. 이유: 이번 범위에서는 탭 내부 state 로 풀이 화면 진입/이탈 처리(`var stack` 등).

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 2 `completed`.
- 실패: 3회 시도 후 `error` + `error_message`.
