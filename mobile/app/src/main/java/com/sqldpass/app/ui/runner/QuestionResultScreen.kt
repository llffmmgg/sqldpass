package com.sqldpass.app.ui.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.PastExamGradeResponse
import com.sqldpass.app.data.PastExamSubjectScore
import com.sqldpass.app.data.SolveResponse
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun QuestionResultScreen(
    result: RunnerResult,
    onClose: () -> Unit,
    onRestart: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (result) {
            is RunnerResult.Solve -> SolveResultCard(result.response)
            is RunnerResult.PastExam -> {
                PassBanner(result.response)
                ScoreCard(
                    correct = result.response.correctCount,
                    total = result.response.totalCount,
                )
                SubjectScoreList(result.response.subjectScores)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onRestart != null) {
                OutlinedButton(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp),
                ) { Text("다음 세트") }
            }
            Button(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) { Text("닫기") }
        }
    }
}

@Composable
private fun SolveResultCard(response: SolveResponse) {
    val total = response.totalCount.coerceAtLeast(1)
    val rate = response.correctCount * 100 / total
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("채점 결과", style = MaterialTheme.typography.titleMedium)
            Text(
                "${response.correctCount} / ${response.totalCount}",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "정답률 ${rate}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PassBanner(response: PastExamGradeResponse) {
    val semantic = LocalSqldpassSemanticColors.current
    val bg = if (response.passed) semantic.passBg else semantic.failBg
    val fg = if (response.passed) semantic.passText else semantic.failText
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (response.passed) "합격" else "불합격",
                style = MaterialTheme.typography.headlineMedium,
                color = fg,
            )
            response.passReason?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = fg)
            }
        }
    }
}

@Composable
private fun ScoreCard(correct: Int, total: Int) {
    val safeTotal = total.coerceAtLeast(1)
    val rate = correct * 100 / safeTotal
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("총점", style = MaterialTheme.typography.titleMedium)
            Text(
                "$correct / $total",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "정답률 ${rate}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubjectScoreList(scores: List<PastExamSubjectScore>) {
    if (scores.isEmpty()) return
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("과목별 결과", style = MaterialTheme.typography.titleMedium)
            scores.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.subjectName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${s.correct}/${s.total} · ${"%.1f".format(s.rate)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (s.failed) {
                        Text(
                            "과락",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
