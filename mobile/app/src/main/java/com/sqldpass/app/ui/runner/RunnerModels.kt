package com.sqldpass.app.ui.runner

import com.sqldpass.app.data.PastExamGradeResponse
import com.sqldpass.app.data.SolveResponse

enum class RunnerMode { MOCK_EXAM, PAST_EXAM, PRACTICE }

data class RunnerQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
)

data class RunnerSession(
    val mode: RunnerMode,
    val title: String,
    val originId: Long,
    val questions: List<RunnerQuestion>,
    val certSlug: String? = null,
    val subjectId: Long? = null,
)

sealed interface RunnerResult {
    data class Solve(val response: SolveResponse, val mode: RunnerMode) : RunnerResult
    data class PastExam(val response: PastExamGradeResponse) : RunnerResult
}

data class RunnerAnswerDraft(
    val questionId: Long,
    val selectedOption: Int? = null,
    val answerText: String? = null,
) {
    val isAnswered: Boolean
        get() = selectedOption != null || !answerText.isNullOrBlank()
}
