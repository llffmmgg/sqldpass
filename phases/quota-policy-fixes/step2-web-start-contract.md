# Step 2: Web Start Contract

## Scope

- `frontend/src/lib/mockExamApi.ts`
- `frontend/src/app/mock-exams/[id]/page.tsx`

## Changes

- Add a `startMockExam(id)` API call using `POST /api/mock-exams/{id}/start`.
- Use detail GET for preview/loading only.
- Consume mock quota only when the user actually starts or submits the exam.
- Preserve quota modal behavior on `402` without replacing it with a generic page error.

## Validation

Run from `frontend/`:

```powershell
npm run lint
npm run build
```
