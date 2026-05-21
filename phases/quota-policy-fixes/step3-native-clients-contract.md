# Step 3: Native Clients Contract

## Scope

- `mobile/app/src/main/java/com/sqldpass/app/data/remote/SqldpassApi.kt`
- `mobile/app/src/main/java/com/sqldpass/app/data/AppRepository.kt`
- `ios/Sqldpass/Services/ExamService.swift`
- `ios/Sqldpass/Features/MockExams/MockExamDetailView.swift`
- `ios/Sqldpass/Features/Solo/SoloSolveView.swift`

## Changes

- Android and iOS mock exam start flows call `POST /api/mock-exams/{id}/start`.
- Android does not fall back to cached mock exam data when the server returns quota exceeded.
- Android does not enqueue quota-exceeded solve submissions into offline pending queues.
- iOS quota paywall purchase CTAs open the real paywall instead of dismissing the current screen.

## Validation

Run from `mobile/`:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

iOS build validation requires macOS/Xcode.
