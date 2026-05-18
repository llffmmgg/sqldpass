# Step 1 — Shared Question Renderer + 결과 화면 해설 fix

## 배경

현재 Android 의 질문 본문/해설 렌더는 **3개 코드 경로로 분산** + **결과 화면 해설은 plain Text 로 깨짐**:

| 위치 | 현재 | 위험 |
|---|---|---|
| `ui/common/SoloMarkdownContent.kt` | Solo 풀이 본문/해설 | 정상 |
| `ui/runner/QuestionRunnerScreen.kt:455-501` (private `MarkdownContent`+`MarkwonTextView`) | Runner 본문 | 정상이지만 중복 |
| **`ui/runner/QuestionResultScreen.kt:322` (`Text(exp)`)** | **결과 화면 해설** | **CRITICAL: markdown/code/표/SVG 깨짐** |

본 step 의 결과는 다른 step 들이 참조하는 단일 진실 원천이 됨.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ui/common/AppQuestionContent.kt` (신규) | 단일 렌더러. `splitMarkdownSegments(ensureCodeFences(text))` → Markdown/CodeBlock/InlineSvg/Image 분기. AppCodeBlockSurface(Card/Bare) enum. |
| `ui/common/SoloMarkdownContent.kt` | **삭제**. 호출처 모두 AppQuestionContent 로 교체 |
| `ui/runner/QuestionRunnerScreen.kt` | private `MarkdownContent`/`MarkwonTextView` (line 455-501 부근) 제거. `Text` 본문 → `AppQuestionContent(codeBlockSurface = AppCodeBlockSurface.Card)` |
| `ui/runner/QuestionResultScreen.kt:322` | `Text(exp)` → `AppQuestionContent(exp)` — **CRITICAL FIX** |
| `ui/solve/SoloSolveScreen.kt:443` | `SoloMarkdownContent(...)` → `AppQuestionContent(...)` |
| `ui/solve/components/SoloExplanationCard.kt:113` | 동일 |

## AppQuestionContent 시그니처

```kotlin
@Composable
fun AppQuestionContent(
    text: String,
    modifier: Modifier = Modifier,
    textSizeSp: Float = 16f,
    codeBlockSurface: AppCodeBlockSurface = AppCodeBlockSurface.Card,
)

enum class AppCodeBlockSurface { Card, Bare }
```

내부 흐름:
1. `ensureCodeFences(text)` 로 HTML 정규화 (기존 함수 재사용 — `text/EnsureCodeFences.kt`)
2. `splitMarkdownSegments(normalized)` (기존 — `text/MarkdownSegments.kt`)
3. `Column(verticalArrangement = Arrangement.spacedBy(SqldSpacing.sm))` 로 세그먼트 순차 렌더:
   - `Markdown` → private `AppMarkwonTextView(text, textSizeSp)` — AndroidView TextView + Markwon spannable + `LocalSqldpassPalette.current.textPrimary` 색
   - `CodeBlock` → surface=Card 면 기존 `ui/runner/CodeBlockCard(language, code)` 재사용, surface=Bare 면 mono Text
   - `InlineSvg` → 기존 `ui/common/InlineSvgView`
   - `Image` → 기존 `ui/common/RemoteImageView`

## Acceptance Criteria

1. `AppQuestionContent.kt` 신설, `SoloMarkdownContent.kt` 삭제
2. `QuestionRunnerScreen.kt` 의 private MarkdownContent/MarkwonTextView 제거
3. `QuestionResultScreen.kt:322` 의 plain Text(exp) 교체 — 결과 화면 해설이 markdown/code/표 렌더
4. 모든 변경 호출처가 `AppQuestionContent` 사용
5. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

수동 시나리오 (macOS 또는 실기기):
- 모의고사 50문 응시 → 마지막 제출 → **결과 화면 해설** 에 코드블록 포함된 문제가 있다면 그 코드가 mono + indent 보존 + 가로 스크롤 동작
- Solo 풀이 → 정답 공개 → 해설 카드의 코드/표 정상
- Runner 본문의 SVG 도 정상 (회귀 X)

## 금지 사항

- `AppOptionRow` 의 옵션 텍스트에 markdown 적용 금지. 이유: 회귀 위험. 별 phase.
- `WrongAnswerTab.kt:245` 의 140자 plain 미리보기 변경 금지. 이유: 정책상 미리보기는 plain, 상세 모달에서만 full 렌더. 별 phase.
- Markwon 인스턴스 새로 만들지 마라. 이유: `SqldpassMarkwon.get(context)` 싱글톤 재사용.
- 기존 `ensureCodeFences`/`splitMarkdownSegments` 로직 수정 금지. 이유: 검증된 동작 — 본 step 은 렌더러 통합만.

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄.
- 실패: 3회 후 `error`.
