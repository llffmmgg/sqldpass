# Step 3 — Frontend validation

## Background

Step 1과 2의 변경은 모두 frontend 한정. 백엔드/API 계약 변경 없음.

## Workdir

```powershell
frontend/
```

## Validation

```powershell
cd frontend
npm run lint
npm run build
```

- `npm run lint`: 기존 baseline warning 외 신규 error/warning 없어야 함.
- `npm run build`: Next.js 16 타입 체크 포함, 통과 필수.

## Status Rules

- Success: step 3 `completed`로 표시, summary에 lint/build 결과 요약.
- Failure: lint/build 에러는 step 1·2로 돌아가 수정. 동일 에러 3회 시 `error`.
