package com.sqldpass.app.ui.wronganswer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.WrongAnswerSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.common.SqldpassBadge

private const val ALL_SUBJECTS = "전체"
private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun WrongAnswerTab(
    state: AppUiState,
    onLoad: () -> Unit = {},
    onStartSession: (List<WrongAnswerSummary>, String) -> Unit = { _, _ -> },
    onLogin: () -> Unit = {},
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null && state.wrongAnswers.isEmpty() && !state.wrongAnswersLoading) {
            onLoad()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            title = "오답노트",
            subtitle = state.nickname?.let { "$it 님의 약점만 모아 풀어요." }
                ?: "로그인하면 오답이 한 자리에 모입니다.",
        )

        if (state.nickname == null) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CtaCard(
                    title = "로그인이 필요합니다",
                    meta = "Google 로 로그인하면 푼 문제 중 틀린 문제만 모아 다시 풀 수 있어요.",
                    ctaLabel = "Google 로 로그인",
                    onClick = onLogin,
                )
            }
            return
        }

        var selectedSubject by rememberSaveable { mutableStateOf(ALL_SUBJECTS) }
        val checkedIds = remember { mutableStateOf(setOf<Long>()) }

        val subjects = remember(state.wrongAnswers) {
            listOf(ALL_SUBJECTS) + state.wrongAnswers
                .mapNotNull { it.subjectName }
                .distinct()
        }
        val filtered = remember(state.wrongAnswers, selectedSubject) {
            if (selectedSubject == ALL_SUBJECTS) state.wrongAnswers
            else state.wrongAnswers.filter { it.subjectName == selectedSubject }
        }
        val selectedItems = remember(filtered, checkedIds.value) {
            filtered.filter { checkedIds.value.contains(it.questionId) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (subjects.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects, key = { it }) { name ->
                            FilterChip(
                                selected = name == selectedSubject,
                                onClick = {
                                    selectedSubject = name
                                    checkedIds.value = emptySet()
                                },
                                label = { Text(name, style = MaterialTheme.typography.labelLarge) },
                            )
                        }
                    }
                }
            }

            item {
                ActionBar(
                    totalCount = filtered.size,
                    selectedCount = selectedItems.size,
                    onStartAll = {
                        if (filtered.isNotEmpty()) {
                            val title = if (selectedSubject == ALL_SUBJECTS) "오답 전체 풀기"
                            else "오답 풀기 · $selectedSubject"
                            onStartSession(filtered, title)
                        }
                    },
                    onStartSelected = {
                        if (selectedItems.isNotEmpty()) {
                            val title = "선택한 오답 ${selectedItems.size}문제"
                            onStartSession(selectedItems, title)
                        }
                    },
                )
            }

            when {
                state.wrongAnswersLoading && state.wrongAnswers.isEmpty() ->
                    item { LoadingRow() }
                filtered.isEmpty() ->
                    item { EmptyRow(selectedSubject) }
                else -> items(filtered, key = { it.questionId }) { wrong ->
                    WrongRow(
                        item = wrong,
                        checked = checkedIds.value.contains(wrong.questionId),
                        onToggle = {
                            checkedIds.value = if (checkedIds.value.contains(wrong.questionId)) {
                                checkedIds.value - wrong.questionId
                            } else {
                                checkedIds.value + wrong.questionId
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    totalCount: Int,
    selectedCount: Int,
    onStartAll: () -> Unit,
    onStartSelected: () -> Unit,
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "선택 $selectedCount / 전체 $totalCount 문제",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onStartAll,
                    enabled = totalCount > 0,
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 44.dp),
                ) { Text("전체 다시풀기") }
                Button(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onStartSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 44.dp),
                ) { Text("선택 시작 ($selectedCount)") }
            }
        }
    }
}

@Composable
private fun WrongRow(
    item: WrongAnswerSummary,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        item.subjectName ?: "과목 미지정",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item.wrongCount >= 2) {
                        SqldpassBadge(
                            label = "${item.wrongCount}회 틀림",
                            base = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(
                    item.questionContent.trim().take(140),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            "오답을 불러오는 중…",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyRow(subject: String) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (subject == ALL_SUBJECTS) "오답이 없습니다." else "$subject 에 오답이 없습니다.",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "문제를 풀고 틀린 문제가 생기면 여기에 모입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
