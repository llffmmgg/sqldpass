package com.sqldpass.app.ui.solve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.SubjectResponse
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.runner.QuestionResultScreen
import com.sqldpass.app.ui.runner.QuestionRunnerScreen
import com.sqldpass.app.ui.runner.RunnerAnswerDraft
import com.sqldpass.app.ui.runner.RunnerMode
import com.sqldpass.app.ui.runner.RunnerResult

private val CardCorner = 14.dp

@Composable
fun SolveTab(
    state: AppUiState,
    onLoadSubjects: () -> Unit,
    onStartPractice: (Long) -> Unit,
    onSubmitAnswers: (List<RunnerAnswerDraft>) -> Unit,
    onCancelRunner: () -> Unit,
    onDismissResult: () -> Unit,
    onNextSet: () -> Unit,
) {
    val runner = state.runner
    val result = state.runnerResult
    when {
        runner != null && runner.mode == RunnerMode.PRACTICE ->
            QuestionRunnerScreen(
                title = runner.title,
                questions = runner.questions,
                onCancel = onCancelRunner,
                onSubmit = onSubmitAnswers,
                submitting = state.runnerSubmitting,
            )
        result is RunnerResult.Solve && result.mode == RunnerMode.PRACTICE ->
            QuestionResultScreen(
                result = result,
                onClose = onDismissResult,
                onRestart = onNextSet,
            )
        else -> SubjectPicker(
            state = state,
            onLoadSubjects = onLoadSubjects,
            onStartPractice = onStartPractice,
        )
    }
}

@Composable
private fun SubjectPicker(
    state: AppUiState,
    onLoadSubjects: () -> Unit,
    onStartPractice: (Long) -> Unit,
) {
    LaunchedEffect(Unit) { onLoadSubjects() }
    val grouped = state.subjects.groupBy { it.parentName ?: "기타" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("문제풀기", style = MaterialTheme.typography.headlineSmall) }
        item {
            Text(
                "과목을 골라 10문제 세트를 받습니다. 풀이가 끝나면 바로 다음 세트로 넘어갈 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            state.subjectsLoading && state.subjects.isEmpty() ->
                item { LoadingText("과목을 불러오는 중…") }
            state.subjects.isEmpty() ->
                item { LoadingText("과목 정보를 가져오지 못했습니다. 잠시 후 다시 시도하세요.") }
            else -> grouped.forEach { (parent, children) ->
                item { SubjectGroupCard(parent = parent, children = children, onStart = onStartPractice) }
            }
        }
    }
}

@Composable
private fun LoadingText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectGroupCard(
    parent: String,
    children: List<SubjectResponse>,
    onStart: (Long) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(parent, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                children.forEach { subject ->
                    AssistChip(
                        onClick = { onStart(subject.id) },
                        label = { Text(subject.name) },
                    )
                }
            }
        }
    }
}
