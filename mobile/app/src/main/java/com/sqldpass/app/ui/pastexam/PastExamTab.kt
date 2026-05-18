package com.sqldpass.app.ui.pastexam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.SkeletonCard
import com.sqldpass.app.ui.theme.CertColors
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldSpacing

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
        contentPadding = PaddingValues(SqldSpacing.lg - 4.dp),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        item {
            CertTabRow(
                certs = state.certSlugs,
                countByCert = state.pastExamsByCert.mapValues { it.value.size },
                selected = state.selectedCertSlug,
                onSelect = onSelectCert,
            )
        }
        when {
            state.pastExamsLoading && exams.isEmpty() -> items(3) { SkeletonCard() }
            exams.isEmpty() ->
                item { EmptyHint(state.selectedCertSlug) }
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
private fun CertTabRow(
    certs: List<String>,
    countByCert: Map<String, Int>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        contentPadding = PaddingValues(vertical = SqldSpacing.xs),
    ) {
        items(certs, key = { it }) { slug ->
            val isSelected = slug == selected
            val dotColor = slugToCertColor(slug, cert)
            val count = countByCert[slug]?.takeIf { it > 0 }
            val label = listOfNotNull(slugLabel(slug), count?.toString()).joinToString(" ")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs + 2.dp),
            ) {
                Box(modifier = Modifier.size(SqldSpacing.sm).background(dotColor, CircleShape))
                AppChip(
                    label = label,
                    selected = isSelected,
                    onClick = { onSelect(slug) },
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(slug: String) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.None) {
        Text(
            "${slugLabel(slug)} 기출 회차가 없습니다.",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Text(
            "곧 회차가 추가될 예정이에요. 다른 자격증을 선택해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
    }
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
