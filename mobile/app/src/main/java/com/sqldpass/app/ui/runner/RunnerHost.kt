package com.sqldpass.app.ui.runner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sqldpass.app.ui.AppUiState

/**
 * 모의고사·기출복원·문제풀기·오답 탭이 공유하는 풀이 화면 호스트.
 *
 * - 풀이 중이면 QuestionRunnerScreen (타이머 + 점프 + 즐겨찾기 + 신고)
 * - 채점 결과가 있으면 QuestionResultScreen
 * - 둘 다 아니면 placeholder 컴포저블 호출
 *
 * 인증·즐겨찾기·신고 콜백은 ViewModel 메서드를 호출자가 넘긴다.
 */
@Composable
fun RunnerHost(
    state: AppUiState,
    mode: RunnerMode,
    onSubmitAnswers: (List<RunnerAnswerDraft>) -> Unit,
    onCancelRunner: () -> Unit,
    onDismissResult: () -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onReport: (type: String, questionId: Long?, content: String, onDone: (Boolean) -> Unit) -> Unit,
    onRestart: (() -> Unit)? = null,
    list: @Composable () -> Unit,
) {
    val runner = state.runner
    val result = state.runnerResult

    val runnerMatches = runner != null && runner.mode == mode
    val resultMatches = when (result) {
        is RunnerResult.Solve -> result.mode == mode
        is RunnerResult.PastExam -> mode == RunnerMode.PAST_EXAM
        null -> false
    }

    var reportingId by remember { mutableStateOf<Long?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }

    when {
        runnerMatches -> {
            QuestionRunnerScreen(
                title = runner!!.title,
                questions = runner.questions,
                onCancel = onCancelRunner,
                onSubmit = onSubmitAnswers,
                submitting = state.runnerSubmitting,
                durationSeconds = runner.durationSeconds,
                bookmarkedIds = state.runnerBookmarks,
                onToggleBookmark = if (state.nickname != null) onToggleBookmark else null,
                onReport = if (state.nickname != null) { qid -> reportingId = qid } else null,
            )
            val qid = reportingId
            if (qid != null) {
                ReportDialog(
                    questionId = qid,
                    submitting = reportSubmitting,
                    onDismiss = { if (!reportSubmitting) reportingId = null },
                    onSubmit = { type, content ->
                        reportSubmitting = true
                        onReport(type, qid, content) { ok ->
                            reportSubmitting = false
                            if (ok) reportingId = null
                        }
                    },
                )
            }
        }
        resultMatches && result != null -> {
            QuestionResultScreen(
                result = result,
                onClose = onDismissResult,
                onRestart = onRestart,
            )
        }
        else -> list()
    }
}
