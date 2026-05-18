# Step 1 - Solve API Contract

## Scope

- `ios/Sqldpass/Services/SolveService.swift`
- `ios/Sqldpass/Models/SolveAnswerEntry.swift`
- `ios/Sqldpass/Models/Solve.swift`
- `backend/src/main/java/com/sqldpass/domain/solve/SolveAnswer.java`
- `backend/src/main/java/com/sqldpass/persistent/solve/SolveMapper.java`
- `backend/src/main/java/com/sqldpass/controller/solve/dto/SolveAnswerResponse.java`

## Result

iOS no longer sends `chosenAnswer`. MCQ submissions use `selectedOption`, while short-answer/descriptive submissions use `answerText`.

Solve answer responses no longer require an artificial `id`, and backend no longer converts non-MCQ nullable options into `0`.

## Validation

- Backend tests: `./gradlew test --tests com.sqldpass.service.wronganswer.WrongAnswerServiceTest`
- iOS build: macOS/Xcode required by `ios/AGENTS.md`
