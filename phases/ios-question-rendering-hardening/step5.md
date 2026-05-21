# Step 5 — Line-Break Fixture Hardening

## Workdir
`ios/`

## Scope
| File | Change |
| --- | --- |
| `ParseQuestion.swift` | Provide normalized parsing entrypoint for all full content surfaces. |
| `QuestionBody.swift`, `OMRAnswerGrid.swift`, `SoloSolveView.swift`, result/retry surfaces | Use normalized parsing consistently. |
| `EnsureCodeFences.swift`, `MarkdownTextView.swift` | Preserve fenced code and visible line breaks. |

## Acceptance Criteria
- `①②③④`, `1./2./3./4.`, and `1)/2)/3)/4)` options display as separate choices when present in content.
- Fenced code indentation is preserved.
- General prose does not get incorrectly fenced or split.

## Validation
Manual fixture screenshots plus build.

## Forbidden
- Do not mutate problem content or API shape. Reason: all repair must happen at render-time.
