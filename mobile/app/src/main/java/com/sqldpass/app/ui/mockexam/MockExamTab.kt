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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.SkeletonCard
import com.sqldpass.app.ui.theme.CertColors
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

private val CardCorner = 14.dp

/**
 * 모의고사 + 기출복원 통합 탭. 상단 세그먼트로 컨텍스트 분기.
 * 모의고사 모드: examType 으로 그룹핑(기존 동작 유지).
 * 기출복원 모드: certSlug 단위 회차 카드(이전 PastExamTab 흡수).
 */
@Composable
fun MockExamTab(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStartExam: (Long) -> Unit,
    onSelectCert: (String) -> Unit = {},
    onStartPastExam: (Long, String) -> Unit = { _, _ -> },
) {
    LaunchedEffect(Unit) {
        if (state.mockExams.isEmpty() && !state.loading) onRefresh()
    }

    var segment by rememberSaveable { mutableStateOf(Segment.MOCK) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentBar(
            segment = segment,
            onSegmentChange = { segment = it },
        )
        when (segment) {
            Segment.MOCK -> MockExamList(
                state = state,
                onRefresh = onRefresh,
                onStartExam = onStartExam,
            )
            Segment.PAST -> PastExamList(
                state = state,
                onSelectCert = onSelectCert,
                onStartExam = onStartPastExam,
            )
        }
    }
}

private enum class Segment { MOCK, PAST }

@Composable
private fun SegmentBar(segment: Segment, onSegmentChange: (Segment) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
    ) {
        SegmentedButton(
            selected = segment == Segment.MOCK,
            onClick = { onSegmentChange(Segment.MOCK) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("모의고사") }
        SegmentedButton(
            selected = segment == Segment.PAST,
            onClick = { onSegmentChange(Segment.PAST) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("기출복원") }
    }
}

@Composable
private fun MockExamList(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStartExam: (Long) -> Unit,
) {
    val exams = state.mockExams
    val byCert = remember(exams) { exams.groupBy { it.examType ?: "기타" } }
    val certs = byCert.keys.toList()
    var selectedCert by remember(certs) { mutableStateOf(certs.firstOrNull()) }
    val visibleExams = selectedCert?.let { byCert[it].orEmpty() } ?: exams

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (certs.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExamTypeChipRow(
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
                item { EmptyHint("공개된 회차가 없습니다.", "로그인하면 PASS+ 프리미엄 회차까지 보입니다.") }
            else -> items(visibleExams, key = { it.id }) { exam ->
                ExamCard(exam = exam, onStart = onStartExam)
            }
        }
    }
}

@Composable
private fun PastExamList(
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CertSlugChipRow(
                certs = state.certSlugs,
                countByCert = state.pastExamsByCert.mapValues { it.value.size },
                selected = state.selectedCertSlug,
                onSelect = onSelectCert,
            )
        }
        when {
            state.pastExamsLoading && exams.isEmpty() -> items(3) { SkeletonCard() }
            exams.isEmpty() ->
                item {
                    EmptyHint(
                        "${slugLabel(state.selectedCertSlug)} 기출 회차가 없습니다.",
                        "곧 회차가 추가될 예정이에요. 다른 자격증을 선택해보세요.",
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
private fun ExamTypeChipRow(
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

@Composable
private fun CertSlugChipRow(
    certs: List<String>,
    countByCert: Map<String, Int>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(certs, key = { it }) { slug ->
            val isSelected = slug == selected
            val dotColor = slugToCertColor(slug, cert)
            val count = countByCert[slug]?.takeIf { it > 0 }
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(slug) },
                leadingIcon = {
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                },
                label = {
                    Text(
                        listOfNotNull(slugLabel(slug), count?.toString()).joinToString(" "),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Composable
private fun EmptyHint(title: String, body: String) {
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
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
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
private fun PastExamCard(exam: PastExamSummary, onStart: () -> Unit) {
    val meta = buildString {
        exam.examYear?.let { append("${it}년") }
        exam.examRound?.let {
            if (isNotEmpty()) append(" · ")
            append("${it}회")
        }
        if (isNotEmpty()) append(" · ")
        append("${exam.totalQuestions}문제")
    }
    val highlight = exam.bestCorrectCount?.let {
        "최고 점수 $it/${exam.bestTotalCount ?: exam.totalQuestions}"
    }
    CtaCard(
        title = exam.name,
        meta = meta,
        highlight = highlight,
        ctaLabel = "기출 풀기",
        onClick = onStart,
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

/** backend PastExamPublicService.certSlugFromExamType 의 6개 slug → 사람 라벨. */
private fun slugLabel(slug: String): String = when (slug) {
    "sqld" -> "SQLD"
    "engineer" -> "정처기 실기"
    "engineer-written" -> "정처기 필기"
    "computer-literacy-1" -> "컴활 1급"
    "computer-literacy-2" -> "컴활 2급"
    "adsp" -> "ADsP"
    else -> slug
}

private fun slugToCertColor(slug: String, cert: CertColors): Color = when (slug) {
    "sqld" -> cert.sqld
    "engineer" -> cert.engineerPractical
    "engineer-written" -> cert.engineerWritten
    "computer-literacy-1" -> cert.cl1
    "computer-literacy-2" -> cert.cl2
    "adsp" -> cert.adsp
    else -> cert.sqld
}
