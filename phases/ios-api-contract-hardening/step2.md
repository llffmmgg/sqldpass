# Step 2 - Answer Type UI

## Scope

- `ios/Sqldpass/Features/Solve/SolveView.swift`
- `ios/Sqldpass/Features/Solve/SolveViewModel.swift`
- `ios/Sqldpass/Features/Solve/Components/OMRAnswerGrid.swift`
- `ios/Sqldpass/Features/Solve/Components/AnswerReviewRow.swift`
- `ios/Sqldpass/Features/WrongAnswers/WrongAnswerRetrySheet.swift`
- `ios/Sqldpass/Models/WrongAnswer.swift`
- `ios/Sqldpass/Services/WrongAnswerService.swift`
- `backend/src/main/java/com/sqldpass/persistent/solve/WrongAnswerProjection.java`
- `backend/src/main/java/com/sqldpass/persistent/solve/SolveAnswerRepository.java`
- `backend/src/main/java/com/sqldpass/controller/wronganswer/dto/WrongAnswerResponse.java`
- `backend/src/main/java/com/sqldpass/controller/wronganswer/dto/WrongAnswerPreviewResponse.java`

## Result

Solve and wrong-answer retry screens now branch by `questionType`.

- MCQ: OMR-style 1-4 buttons submit `selectedOption`.
- SHORT_ANSWER/DESCRIPTIVE/TEXT: text input submits `answerText`.

Wrong-answer list and preview responses now include `questionType` so native clients do not guess the answer UI.

## Validation

- Static API scan: no remaining iOS `chosenAnswer` submit usage.
- Backend service test updated for `questionType`.
