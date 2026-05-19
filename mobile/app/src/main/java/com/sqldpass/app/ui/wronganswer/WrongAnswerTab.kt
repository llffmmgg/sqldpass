package com.sqldpass.app.ui.wronganswer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.WrongAnswerSummary
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.AppCodeBlockSurface
import com.sqldpass.app.ui.common.AppQuestionContent
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.common.SqldpassBadge
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

private const val ALL_SUBJECTS = "전체"

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
                modifier = Modifier.padding(SqldSpacing.lg - SqldSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
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
            contentPadding = PaddingValues(SqldSpacing.lg - SqldSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.md),
        ) {
            if (subjects.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
                        items(subjects, key = { it }) { name ->
                            AppChip(
                                label = name,
                                selected = name == selectedSubject,
                                onClick = {
                                    selectedSubject = name
                                    checkedIds.value = emptySet()
                                },
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
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.md - SqldSpacing.xxs),
        ) {
            Text(
                "선택 $selectedCount / 전체 $totalCount 문제",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm)) {
                AppButton(
                    text = "전체 다시풀기",
                    onClick = onStartAll,
                    variant = AppButtonVariant.Secondary,
                    enabled = totalCount > 0,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "선택 시작 ($selectedCount)",
                    onClick = onStartSelected,
                    variant = AppButtonVariant.Primary,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                )
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
    val palette = LocalSqldpassPalette.current
    AppCard(
        surface = AppCardSurface.Card,
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
        ) {
            AppCheckControl(
                checked = checked,
                onToggle = onToggle,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SqldSpacing.xs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        item.subjectName ?: "과목 미지정",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item.wrongCount >= 2) {
                        SqldpassBadge(
                            label = "${item.wrongCount}회 틀림",
                            base = palette.danger,
                        )
                    }
                }
                AppQuestionContent(
                    text = item.questionContent.trim(),
                    textSizeSp = 14f,
                    codeBlockSurface = AppCodeBlockSurface.Bare,
                )
            }
        }
    }
}

@Composable
private fun AppCheckControl(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Checkbox,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (checked) palette.accent else palette.elevated,
                    RoundedCornerShape(SqldRadius.sm),
                )
                .border(
                    BorderStroke(1.dp, if (checked) palette.accent else palette.borderStrong),
                    RoundedCornerShape(SqldRadius.sm),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = palette.accentFg,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Text(
            "오답을 불러오는 중…",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun EmptyRow(subject: String) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (subject == ALL_SUBJECTS) "오답이 없습니다." else "$subject 에 오답이 없습니다.",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            Text(
                "문제를 풀고 틀린 문제가 생기면 여기에 모입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
        }
    }
}
