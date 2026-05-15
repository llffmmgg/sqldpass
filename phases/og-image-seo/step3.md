# Step 3 - Frontend validation

## Background

This phase affects Next metadata and special OG image routes. It must pass the normal frontend validation path before completion.

## Workdir

```powershell
frontend/
```

## Validation

```powershell
npm run lint
npm run build
```

## Acceptance Criteria

1. `frontend/public/og/og-bg-cbt.png` exists.
2. Root OG image route builds with the shared template.
3. `/cbt-mock-exam/[cert]/opengraph-image.tsx` builds and resolves all supported certificate slugs.
4. `/`, `/mock-exams`, `/cbt-mock-exam`, and `/cbt-mock-exam/[cert]` include `openGraph.images` and `twitter.images`.
5. No visible UI layout or styling changes are introduced.

## Status Rules

- Success: mark step 3 and phase `completed` with lint/build results.
- Failure: mark `blocked` only if validation cannot run because of local environment constraints.
