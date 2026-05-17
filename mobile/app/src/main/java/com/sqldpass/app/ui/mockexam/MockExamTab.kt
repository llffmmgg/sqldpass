package com.sqldpass.app.ui.mockexam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.runner.RunnerAnswerDraft
import com.sqldpass.app.ui.runner.RunnerHost
import com.sqldpass.app.ui.runner.RunnerMode

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun MockExamTab(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStartExam: (Long) -> Unit,
    onSubmitAnswers: (List<RunnerAnswerDraft>) -> Unit,
    onCancelRunner: () -> Unit,
    onDismissResult: () -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onReport: (type: String, questionId: Long?, content: String, onDone: (Boolean) -> Unit) -> Unit,
) {
    RunnerHost(
        state = state,
        mode = RunnerMode.MOCK_EXAM,
        onSubmitAnswers = onSubmitAnswers,
        onCancelRunner = onCancelRunner,
        onDismissResult = onDismissResult,
        onToggleBookmark = onToggleBookmark,
        onReport = onReport,
    ) {
        MockExamList(state = state, onRefresh = onRefresh, onStart = onStartExam)
    }
}

@Composable
private fun MockExamList(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStart: (Long) -> Unit,
) {
    val exams = state.mockExams
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("모의고사", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onRefresh) { Text("새로고침") }
            }
        }
        when {
            state.loading && exams.isEmpty() ->
                item { StatusText("회차를 불러오는 중…") }
            exams.isEmpty() && state.message != null ->
                item { ErrorCard(message = state.message, onRetry = onRefresh) }
            exams.isEmpty() ->
                item { StatusText("공개된 회차가 없습니다. 잠시 후 다시 시도하세요.") }
            else -> items(exams, key = { it.id }) { exam ->
                ExamCard(exam = exam, onStart = onStart)
            }
        }
    }
}

@Composable
private fun ExamCard(exam: MockExamSummary, onStart: (Long) -> Unit) {
    val locked = exam.isPremium && !exam.purchased
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(exam.name, style = MaterialTheme.typography.titleMedium)
                if (locked) Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "PASS+ 잠금",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${exam.examType ?: "CBT"} · ${exam.totalQuestions}문제",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            exam.bestCorrectCount?.let {
                Text(
                    "최고 점수 $it/${exam.bestTotalCount ?: exam.totalQuestions}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = { onStart(exam.id) },
                enabled = !locked,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text(if (locked) "PASS+ 전용" else "풀이 시작")
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("불러오기 실패", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text("다시 시도") }
        }
    }
}
