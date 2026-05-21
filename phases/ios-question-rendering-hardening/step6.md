# Step 6 — iOS Build And Visual Verification

## Workdir
`ios/` on macOS only.

## Commands
```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

## Screenshots
- Problem body with SQL/code block.
- Option with code block, inline code, table, and multiline text.
- Explanation with code/table.
- Result review expanded body/options/explanation.
- Wrong-answer retry sheet.
- Bookmark detail.
- Inline SVG graph.
- Markdown/HTML image and photo.
- Light mode, dark mode, Dynamic Type enlarged.

## Acceptance Criteria
- Build succeeds.
- Code and tables scroll horizontally instead of clipping.
- SVG/images do not collapse.
- Text remains readable with Dynamic Type.

## Status Rules
- Success: mark completed with build output and screenshot list.
- macOS unavailable: mark blocked with reason.
