package com.sqldpass.app.ui.pastexam

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.SkeletonCard

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun PastExamTab(
    state: AppUiState,
    onSelectCert: (String) -> Unit,
    onStartExam: (Long, String) -> Unit,
) {
    LaunchedEffect(state.selectedCertSlug) {
        if (state.pastExamsByCert[state.selectedCertSlug] == null) {
            onSelectCert(state.selectedCertSlug)
        }
    }
    val exams = state.pastExamsByCert[state.selectedCertSlug].orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("기출복원", style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.certSlugs.forEach { slug ->
                    val selected = state.selectedCertSlug == slug
                    AssistChip(
                        onClick = { onSelectCert(slug) },
                        label = { Text(slug) },
                        colors = if (selected) AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) else AssistChipDefaults.assistChipColors(),
                    )
                }
            }
        }
        when {
            state.pastExamsLoading && exams.isEmpty() -> items(3) { SkeletonCard() }
            exams.isEmpty() ->
                item {
                    Text(
                        "이 자격증의 기출 회차가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            else -> items(exams, key = { it.id }) { exam ->
                PastExamCard(
                    exam = exam,
                    onStart = { onStartExam(exam.id, state.selectedCertSlug) },
                )
            }
        }
    }
}

@Composable
private fun PastExamCard(exam: PastExamSummary, onStart: () -> Unit) {
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
            Text(exam.name, style = MaterialTheme.typography.titleMedium)
            val meta = buildString {
                exam.examYear?.let { append("${it}년") }
                exam.examRound?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it}회")
                }
                if (isNotEmpty()) append(" · ")
                append("${exam.totalQuestions}문제")
            }
            Text(
                meta,
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
                onClick = onStart,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) { Text("풀이 시작") }
        }
    }
}
