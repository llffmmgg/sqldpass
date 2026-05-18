package com.sqldpass.app.ui.runner

import com.sqldpass.app.data.PastExamGradeResponse
import com.sqldpass.app.data.SolveResponse

enum class RunnerMode { MOCK_EXAM, PAST_EXAM, PRACTICE, WRONG_ANSWERS }

data class RunnerQuestion(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val questionType: String?,
) {
    /** content 를 ①②③④ 기준으로 본문/보기 분리한 결과. lazy — 첫 참조 시만 파싱. */
    val parsed: com.sqldpass.app.text.ParsedQuestion by lazy {
        com.sqldpass.app.text.parseQuestion(content)
    }
}

data class RunnerSession(
    val mode: RunnerMode,
    val title: String,
    val originId: Long,
    val questions: List<RunnerQuestion>,
    val certSlug: String? = null,
    val subjectId: Long? = null,
    /** 시험 시간(초). 0 이면 무제한. */
    val durationSeconds: Int = 0,
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
