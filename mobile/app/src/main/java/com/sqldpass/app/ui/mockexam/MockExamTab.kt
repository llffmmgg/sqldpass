package com.sqldpass.app.ui.mockexam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.QuotaBadge
import com.sqldpass.app.ui.common.QuotaKind
import com.sqldpass.app.ui.common.SkeletonCard
import com.sqldpass.app.ui.theme.CertColors
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 모의고사 탭 — 회차 카탈로그 단일 책임.
 *
 * 기존에 흡수했던 기출복원 세그먼트는 phase mobile-ux-restructure 에서 별도 탭(PastExam)으로
 * 다시 분리. 본 탭은 모의고사 회차만 자격증 칩 필터로 노출한다.
 */
@Composable
fun MockExamTab(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStartExam: (Long) -> Unit,
    onLoadQuota: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        if (state.mockExams.isEmpty() && !state.loading) onRefresh()
        // 일일 한도 사전 표시 — 비로그인이면 ViewModel 측에서 무시.
        onLoadQuota()
    }

    val exams = state.mockExams
    val byCert = remember(exams) { exams.groupBy { it.examType ?: "기타" } }
    val certs = byCert.keys.toList()
    var selectedCert by remember(certs) { mutableStateOf(certs.firstOrNull()) }
    val visibleExams = selectedCert?.let { byCert[it].orEmpty() } ?: exams

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SqldSpacing.lg - 4.dp,
            end = SqldSpacing.lg - 4.dp,
            top = SqldSpacing.base,
            bottom = SqldSpacing.lg - 4.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        // 일일 한도 배지 — 무료 회원에 한해 "오늘 N / 1 모의고사" 표시(활성 구독자는 null → 숨김).
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuotaBadge(quota = state.quota, kind = QuotaKind.Mock)
            }
        }
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
                    AppButton(
                        text = "새로고침",
                        onClick = onRefresh,
                        variant = AppButtonVariant.Tertiary,
                        size = AppButtonSize.Compact,
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    AppButton(
                        text = "새로고침",
                        onClick = onRefresh,
                        variant = AppButtonVariant.Tertiary,
                        size = AppButtonSize.Compact,
                    )
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
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        contentPadding = PaddingValues(vertical = SqldSpacing.xs),
    ) {
        items(certs, key = { it }) { name ->
            val isSelected = name == selected
            val dotColor = examTypeToCert(name, cert)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.xs + 2.dp),
            ) {
                Box(modifier = Modifier.size(SqldSpacing.sm).background(dotColor, CircleShape))
                AppChip(
                    label = "${examTypeLabel(name)} ${countByCert[name] ?: 0}",
                    selected = isSelected,
                    onClick = { onSelect(name) },
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(title: String, body: String) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.None) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
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
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card, accent = AppCardAccent.Danger) {
        Text(
            "불러오기 실패",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.Tertiary,
            size = AppButtonSize.Compact,
        )
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
