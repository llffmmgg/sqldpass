package com.sqldpass.app.ui.mockexam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.SkeletonCard
import com.sqldpass.app.ui.theme.CertColors
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun MockExamTab(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStartExam: (Long) -> Unit,
) {
    // 첫 진입 시 비로그인 init skip 보완 — 회차 비어 있으면 자동 호출
    LaunchedEffect(Unit) {
        if (state.mockExams.isEmpty() && !state.loading) onRefresh()
    }

    val exams = state.mockExams
    // examType 기준 자격증 그룹핑 — frontend MockExamsClient 패턴
    val byCert = remember(exams) { exams.groupBy { it.examType ?: "기타" } }
    val certs = byCert.keys.toList()
    var selectedCert by remember(certs) { mutableStateOf(certs.firstOrNull()) }
    val visibleExams = selectedCert?.let { byCert[it].orEmpty() } ?: exams

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (certs.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CertTabRow(
                        certs = certs,
                        countByCert = byCert.mapValues { it.value.size },
                        selected = selectedCert,
                        onSelect = { selectedCert = it },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) { Text("새로고침") }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRefresh) { Text("새로고침") }
                }
            }
        }
        when {
            state.loading && exams.isEmpty() -> items(3) { SkeletonCard() }
            exams.isEmpty() && state.message != null ->
                item { ErrorCard(message = state.message, onRetry = onRefresh) }
            exams.isEmpty() ->
                item { EmptyHint() }
            else -> items(visibleExams, key = { it.id }) { exam ->
                ExamCard(exam = exam, onStart = onStartExam)
            }
        }
    }
}

@Composable
private fun CertTabRow(
    certs: List<String>,
    countByCert: Map<String, Int>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(certs, key = { it }) { name ->
            val isSelected = name == selected
            val dotColor = examTypeToCert(name, cert)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(name) },
                leadingIcon = {
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                },
                label = {
                    Text(
                        "${examTypeLabel(name)} ${countByCert[name] ?: 0}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/** 백엔드 ExamType enum 값을 사람 라벨로. */
private fun examTypeLabel(type: String): String = when (type.uppercase()) {
    "SQLD" -> "SQLD"
    "ENGINEER_PRACTICAL" -> "정처기 실기"
    "ENGINEER_WRITTEN" -> "정처기 필기"
    "COMPUTER_LITERACY_1" -> "컴활 1급"
    "COMPUTER_LITERACY_2" -> "컴활 2급"
    "ADSP" -> "ADsP"
    else -> type
}

private fun examTypeToCert(type: String, cert: CertColors): Color = when (type.uppercase()) {
    "SQLD" -> cert.sqld
    "ENGINEER_PRACTICAL" -> cert.engineerPractical
    "ENGINEER_WRITTEN" -> cert.engineerWritten
    "COMPUTER_LITERACY_1" -> cert.cl1
    "COMPUTER_LITERACY_2" -> cert.cl2
    "ADSP" -> cert.adsp
    else -> cert.sqld
}

@Composable
private fun EmptyHint() {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("공개된 회차가 없습니다.", style = MaterialTheme.typography.titleMedium)
            Text(
                "로그인하면 PASS+ 프리미엄 회차까지 보입니다. 새로고침을 한 번 더 시도해도 비어있으면 잠시 후 다시 와주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExamCard(exam: MockExamSummary, onStart: (Long) -> Unit) {
    val locked = exam.isPremium && !exam.purchased
    val highlight = exam.bestCorrectCount?.let {
        "최고 점수 $it/${exam.bestTotalCount ?: exam.totalQuestions}"
    }
    CtaCard(
        title = exam.name,
        meta = "${exam.examType?.let { examTypeLabel(it) } ?: "CBT"} · ${exam.totalQuestions}문제",
        highlight = highlight,
        ctaLabel = "응시하기",
        lockedCtaLabel = "PASS+ 전용",
        locked = locked,
        onClick = { onStart(exam.id) },
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
