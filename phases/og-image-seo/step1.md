# Step 1 - OG background asset

## Background

OpenGraph cards need a real visual asset, but Korean text should not be baked into the generated bitmap. The bitmap is only a background; all Korean titles are rendered by Next `opengraph-image.tsx`.

## Workdir

```powershell
frontend/
```

## Scope

| File | Change |
| --- | --- |
| `frontend/public/og/og-bg-cbt.png` | Text-free GPT-generated CBT learning background for 1200x630 OG cards |

## Implementation

- Generate one landscape image with built-in image generation.
- Avoid all readable text, logos, watermarks, people, and mascot shapes.
- Leave darker negative space around the left/center so rendered Korean text remains legible.
- Save the final project asset under `frontend/public/og/og-bg-cbt.png`.

## Validation

- The file exists under `frontend/public/og/`.
- The image is suitable as a background and does not contain text that could conflict with Korean overlay copy.

## Status Rules

- Success: mark step 1 `completed` with the saved path and prompt summary.
- Failure: mark `error` after repeated generation/copy failures.
