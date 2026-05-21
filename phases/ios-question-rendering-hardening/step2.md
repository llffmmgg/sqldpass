# Step 2 — Image, SVG, Photo Rendering

## Workdir
`ios/`

## Scope
| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Solve/Components/InlineSVGView.swift` | Give inline SVG graphs deterministic sizing and full-width scaled fit. |
| `ios/Sqldpass/Features/Solve/Components/RemoteQuestionImageView.swift` | Add a reusable renderer for remote images, data URI images, SVG payloads, placeholders, and failures. |
| `ios/Sqldpass/Features/Solve/Components/QuestionContentView.swift` | Use the reusable image renderer for image segments. |

## Acceptance Criteria
- Inline SVG graphs do not collapse or misalign.
- Markdown/HTML images and photos fit the content width without clipping.
- Data URI and SVG image sources show a useful fallback if decoding fails.

## Validation
Simulator screenshots for inline SVG, remote image, data image, and failed image.

## Forbidden
- Do not introduce non-token colors. Reason: light/dark design tokens must remain the source of truth.
