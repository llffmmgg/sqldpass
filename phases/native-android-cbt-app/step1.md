# Step 1 - Backend Mobile Contracts

## Goal
Add the minimum backend contracts needed by the native Android CBT app without changing existing web client behavior.

## Work
- Add optional `clientSubmissionId` to `POST /api/solves`.
- Persist `client_submission_id` on `solve`.
- Make duplicate `(member, clientSubmissionId)` submissions idempotent.
- Add `GET /api/mobile/content/snapshot` with ETag support.
- Filter mobile snapshot premium exams by active premium access or purchased mock exam.

## Acceptance Criteria
- Existing web solve submissions that omit `clientSubmissionId` still compile and pass tests.
- Existing `/api/content/snapshot` behavior remains available.
- New mobile snapshot endpoint returns the same DTO shape as the existing snapshot.
- Flyway migration adds the new nullable solve column and uniqueness guard.
- Backend targeted tests or compile validation pass.

## Validation
```powershell
.\gradlew.bat test --tests com.sqldpass.service.solve.SolveServiceTest
.\gradlew.bat test --tests com.sqldpass.controller.solve.SolveControllerTest
.\gradlew.bat compileJava
```

