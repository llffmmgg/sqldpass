# Step 4 — Screen Adoption Audit

## Workdir
`ios/`

## Scope
| File | Change |
| --- | --- |
| `AnswerReviewRow.swift` | Expand result rows as body + parsed option rows + text-answer details. |
| `SolveResultView.swift` | Pass available answer/explanation metadata into review rows. |
| `PastExamResultView.swift` | Pass answer, submitted text, keywords, explanation into review rows. |
| `WrongAnswerRetrySheet.swift` | Show actual parsed option text with `AppOptionRow`. |
| `WrongAnswersView.swift`, `BookmarksView.swift` | Use a normalized plain preview helper instead of raw markdown/code text. |

## Acceptance Criteria
- Result screens show options with selected/correct state.
- Wrong-answer retry shows real choices, not only `1번`~`4번`.
- List previews no longer expose raw markdown/code/HTML noise.

## Validation
Simulator screenshots for result review, wrong-answer retry, wrong-answer list, and bookmark list/detail.

## Forbidden
- Do not make list preview cards fully rich by default. Reason: preview rows should remain compact and scannable.
