# Step 2 - OG template and metadata

## Background

The current site has a root OG image route, but key SEO landing pages do not explicitly attach large preview images. `/cbt-mock-exam/[cert]` also needs certificate-specific title text for SQLD CBT, 정보처리기사 CBT, 컴활 CBT, and ADsP CBT searches.

## Workdir

```powershell
frontend/
```

## Scope

| File | Change |
| --- | --- |
| `frontend/src/app/opengraph-image.tsx` | Render the generated background with default CBT overlay copy |
| `frontend/src/app/cbt-mock-exam/[cert]/opengraph-image.tsx` | Render certificate-specific OG cards |
| `frontend/src/lib/ogImageTemplate.tsx` | Shared OG card template and background loader |
| `frontend/src/app/page.tsx` | Add `openGraph.images` and `twitter.images` |
| `frontend/src/app/mock-exams/layout.tsx` | Add `openGraph.images` and `twitter.images` |
| `frontend/src/app/cbt-mock-exam/page.tsx` | Add `openGraph.images` and `twitter.images` |
| `frontend/src/app/cbt-mock-exam/[cert]/page.tsx` | Add certificate-specific `openGraph.images` and `twitter.images` |

## Implementation

- Use the generated bitmap only as a background.
- Render all Korean text in code through `ImageResponse`.
- Default OG copy:
  - `SQLD · 정보처리기사 · 컴활`
  - `무료 CBT 모의고사`
  - `문어CBT`
- Certificate route copy:
  - SQLD: `SQLD CBT 무료 모의고사`
  - 정보처리기사 실기: `정보처리기사 실기 CBT`
  - 정보처리기사 필기: `정보처리기사 필기 CBT`
  - 컴활 1급: `컴활 1급 CBT`
  - 컴활 2급: `컴활 2급 CBT`
  - ADsP: `ADsP CBT`

## Validation

```powershell
cd frontend
npm run lint
npm run build
```

## Forbidden

- Do not change visible page UI. Reason: this phase is SEO/share-card only.
- Do not embed Korean text inside the GPT-generated image. Reason: image generation can distort exact Korean copy.
- Do not change backend APIs. Reason: no API contract is needed.

## Status Rules

- Success: mark step 2 `completed` with the changed route/meta files.
- Failure: mark `error` if the OG route or metadata cannot build.
