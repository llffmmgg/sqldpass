# Step 1: Backend Quota Contract

## Scope

- `backend/src/main/java/com/sqldpass/controller/mockexam/MockExamController.java`
- `backend/src/main/java/com/sqldpass/controller/question/QuestionController.java`
- `backend/src/main/java/com/sqldpass/service/usage/DailyUsageService.java`
- Backend quota tests under `backend/src/test/java`

## Changes

- Keep `GET /api/mock-exams/{id}` as a detail-only read path.
- Add `POST /api/mock-exams/{id}/start` as the quota-consuming start path for non-`PAST_EXAM` mock exams.
- Charge random questions by the actual returned question count, not the requested size.
- Cap quota-exceeded `used` values at the visible plan limit.
- Update tests to assert the new detail/start split.

## Validation

Run from `backend/`:

```powershell
.\gradlew.bat test --tests com.sqldpass.service.usage.DailyUsageServiceTest --tests com.sqldpass.controller.usage.QuotaIntegrationTest
```
