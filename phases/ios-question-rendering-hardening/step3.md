# Step 3 — Table Parser And Layout Alignment

## Workdir
`ios/`

## Scope
| File | Change |
| --- | --- |
| `ios/Sqldpass/Core/Content/ParseQuestionContent.swift` | Preserve escaped pipes, validate delimiter rows, normalize empty cells and widths. |
| `ios/Sqldpass/Features/Solve/Components/MarkdownTableView.swift` | Use stable cell widths, visible horizontal scroll, header/body separation, and safe wrapping. |

## Acceptance Criteria
- GFM tables with `\\|` render as a literal pipe inside the cell.
- Tables inside fenced code are not parsed as tables.
- Wide tables scroll horizontally on small screens.

## Validation
Simulator screenshot for GFM table, HTML table converted by `EnsureCodeFences`, and escaped-pipe cell.

## Forbidden
- Do not parse table-like lines inside code fences. Reason: code samples must remain code.
