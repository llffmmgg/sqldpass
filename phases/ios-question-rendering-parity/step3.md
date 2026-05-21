# Step 3 — WrongAnswerRetrySheet 가 공통 QuestionContentView 를 쓰도록 교체

## 배경

iOS 의 다른 화면(`QuestionBody`, `AppOptionRow`, `SoloExplanationCard`, `AnswerReviewRow`) 은 모두 `Features/Solve/Components/QuestionContentView.swift` 를 통과해 마크다운/코드블록/표/이미지가 정상 렌더된다.

오답노트 다시풀기 시트 `ios/Sqldpass/Features/WrongAnswers/WrongAnswerRetrySheet.swift` 는 두 군데서 plain `Text(...)` 를 직접 쓰고 있어 마크다운/코드블록/표가 모두 무시된다:
- 라인 31: `Text(item.questionContent)` — 문제 본문.
- 라인 165: `Text(explanation)` — 채점 결과 해설.

사용자가 검증 항목으로 "오답노트" 를 명시했으므로 한 줄 누락이 사용자 가시 차이에 직접 기여.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Features/WrongAnswers/WrongAnswerRetrySheet.swift` | line 31 의 `Text(item.questionContent).font(...).foregroundStyle(...).fixedSize(...)` → `QuestionContentView(text: item.questionContent)` 1 줄. line 165-168 의 `Text(explanation).font(...).foregroundStyle(...).fixedSize(...)` → `QuestionContentView(text: explanation)` 1 줄. |

## 구현 상세

### 라인 31 교체 (문제 본문)

현재:
```swift
Text(item.subjectName)
    .font(AppType.caption.weight(.semibold))
    .foregroundStyle(Color.brandPrimary)
Text(item.questionContent)
    .font(AppType.body)
    .foregroundStyle(Color.appTextPrimary)
    .fixedSize(horizontal: false, vertical: true)
```

변경:
```swift
Text(item.subjectName)
    .font(AppType.caption.weight(.semibold))
    .foregroundStyle(Color.brandPrimary)
QuestionContentView(text: item.questionContent)
```

`QuestionContentView` 가 내부에서 `.frame(maxWidth: .infinity, alignment: .leading)` 적용하므로 외곽 modifier 불필요.

### 라인 161-168 교체 (해설)

현재:
```swift
if let explanation = r.explanation, !explanation.isEmpty {
    Divider()
    Text("해설")
        .font(AppType.bodyEmph)
    Text(explanation)
        .font(AppType.body)
        .foregroundStyle(Color.appTextMuted)
        .fixedSize(horizontal: false, vertical: true)
}
```

변경:
```swift
if let explanation = r.explanation, !explanation.isEmpty {
    Divider()
    Text("해설")
        .font(AppType.bodyEmph)
    QuestionContentView(text: explanation)
}
```

해설 본문 톤이 기존엔 `appTextMuted` 였지만 `QuestionContentView` 의 기본 톤은 `appTextPrimary` (해설은 본문과 동일 가독성 톤이 사용자가 명시한 "문제 가독성/렌더링 정확성 최우선" 원칙에 부합) — 톤 변경 OK.

## 검증

- 컴파일 에러 0 (QuestionContentView 는 같은 모듈, import 추가 불필요).
- Windows 셸에서 build 검증 불가. Step 6 시뮬레이터 스크린샷에서 오답노트 다시풀기 시트의 본문/해설이 다른 화면과 동일 렌더링인지 확인.

## Acceptance Criteria

1. `WrongAnswerRetrySheet.swift` 의 `Text(item.questionContent)` 와 `Text(explanation)` 가 각각 `QuestionContentView(text:)` 호출로 교체됨.
2. 외곽 `Text` modifier 들(font/foregroundStyle/fixedSize) 가 제거됨.
3. 다른 부분(과목명 라벨, 선택지 버튼, 결과 아이콘/메시지/정답 표시, "오답노트에서 제거" 안내문) 무변경.
4. 회귀 없음 — 사용자가 시트 열 때 마크다운/코드블록을 포함한 본문이 정상 렌더 (Step 6 확인).

## 금지 사항

- `QuestionContentView` 자체를 변경하지 마라. **이유**: 본 step 은 호출 측만 다룸.
- 시트 레이아웃(`NavigationStack` / `ScrollView` / `VStack` 구조) 을 바꾸지 마라. **이유**: 회귀 위험 + 본 step 범위 밖.
- `WrongAnswer` 모델/`WrongAnswerService` 를 건드리지 마라. **이유**: API 계약 정책.
- "해설" 라벨 위 `Divider()` 를 제거하지 마라. **이유**: 결과 카드 안 시각 구분.

## Status 규칙

- 성공: `completed` + summary "WrongAnswerRetrySheet 본문/해설 2 곳을 QuestionContentView 로 교체. 외곽 Text modifier 제거".
- 실패: 3회 재시도 후 `error`.
