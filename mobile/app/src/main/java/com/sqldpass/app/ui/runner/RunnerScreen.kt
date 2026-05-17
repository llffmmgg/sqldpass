package com.sqldpass.app.ui.runner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sqldpass.app.ui.AppUiState

/**
 * NavHost 의 runner 단일 destination 진입점.
 * state.runner / state.runnerResult 에 따라 QuestionRunnerScreen 또는 QuestionResultScreen.
 * 둘 다 null 이면 화면이 의미 없어진 것 → exit 콜백으로 popBackStack.
 */
@Composable
fun RunnerScreen(
    state: AppUiState,
    onSubmitAnswers: (List<RunnerAnswerDraft>) -> Unit,
    onCancelRunner: () -> Unit,
    onDismissResult: () -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onReport: (type: String, questionId: Long?, content: String, onDone: (Boolean) -> Unit) -> Unit,
    onRestart: (() -> Unit)? = null,
    onExitScreen: () -> Unit,
) {
    val runner = state.runner
    val result = state.runnerResult

    // 둘 다 사라지면 popBackStack — 사용자가 결과 닫기 누를 때 자연스럽게 list 로 복귀.
    LaunchedEffect(runner, result) {
        if (runner == null && result == null) onExitScreen()
    }

    var reportingId by remember { mutableStateOf<Long?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }

    when {
        runner != null -> {
            QuestionRunnerScreen(
                title = runner.title,
                questions = runner.questions,
                onCancel = onCancelRunner,
                onSubmit = onSubmitAnswers,
                submitting = state.runnerSubmitting,
                durationSeconds = runner.durationSeconds,
                bookmarkedIds = state.runnerBookmarks,
                onToggleBookmark = if (state.nickname != null) onToggleBookmark else null,
                onReport = if (state.nickname != null) { qid -> reportingId = qid } else null,
            )
            reportingId?.let { qid ->
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
        result != null -> {
            QuestionResultScreen(
                result = result,
                onClose = onDismissResult,
                onRestart = onRestart,
            )
        }
    }
}
