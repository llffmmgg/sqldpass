# Step 1 — Renderer API And Option Code Mode

## Workdir
`ios/`

## Scope
| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Solve/Components/CodeBlockView.swift` | Add card/bare surfaces while preserving horizontal scroll and syntax highlighting. |
| `ios/Sqldpass/Features/Solve/Components/QuestionContentView.swift` | Route option rendering to bare/compact code blocks. |
| `ios/Sqldpass/Core/DesignSystem/AppOptionRow.swift` | Render option text with the option variant. |

## Acceptance Criteria
- Body/explanation code blocks keep the full dark card chrome.
- Option code blocks are compact, scroll horizontally, and do not force nested heavy cards.
- No `.lineLimit` is introduced on option content.

## Validation
macOS: `xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass -destination 'platform=iOS Simulator,name=iPhone 15 Pro' -configuration Debug build`

## Forbidden
- Do not change backend/API data. Reason: this phase only renders existing content.
- Do not use WKWebView. Reason: iOS app is native SwiftUI.
