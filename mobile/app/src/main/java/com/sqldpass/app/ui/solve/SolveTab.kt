package com.sqldpass.app.ui.solve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.sqldpass.app.data.SubjectResponse
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.SkeletonCard
import com.sqldpass.app.ui.theme.CertColors
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors

private val CardCorner = 14.dp

@Composable
fun SolveTab(
    state: AppUiState,
    onLoadSubjects: () -> Unit,
    onStartPractice: (Long) -> Unit,
) {
    // /api/subjects 는 로그인 필수 — 비로그인이면 401. 가드.
    val loggedIn = state.nickname != null
    LaunchedEffect(loggedIn) {
        if (loggedIn) onLoadSubjects()
    }

    if (!loggedIn) {
        LoginRequiredHint()
        return
    }

    val grouped = state.subjects.groupBy { it.parentName ?: "기타" }
    val parents = grouped.keys.toList()
    var selectedCert by remember(parents) { mutableStateOf(parents.firstOrNull()) }
    val visibleGrouped = if (selectedCert == null) grouped
    else mapOf(selectedCert!! to grouped[selectedCert].orEmpty())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "자격증을 고르면 10문제 세트를 받습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 자격증 탭바 (frontend SolveClient.tsx:512-540 패턴)
        if (parents.isNotEmpty()) {
            item {
                CertTabRow(
                    parents = parents,
                    countByParent = grouped.mapValues { it.value.size },
                    selected = selectedCert,
                    onSelect = { selectedCert = it },
                )
            }
        }
        when {
            state.subjectsLoading && state.subjects.isEmpty() ->
                items(items = (0..1).toList(), key = { "skel-$it" }) { SkeletonCard() }
            state.subjects.isEmpty() ->
                item {
                    Text(
                        "과목 정보를 가져오지 못했습니다. 잠시 후 다시 시도하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            else -> visibleGrouped.forEach { (parent, children) ->
                item {
                    SubjectGroupCard(
                        parent = parent,
                        children = children,
                        onStart = onStartPractice,
                    )
                }
            }
        }
    }
}

@Composable
private fun CertTabRow(
    parents: List<String>,
    countByParent: Map<String, Int>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(parents) { name ->
            val isSelected = name == selected
            val dotColor = parentNameToCert(name, cert)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape),
                    )
                },
                label = {
                    Text(
                        "${shortenCertName(name)} ${countByParent[name] ?: 0}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/**
 * parentName → cert palette 매핑. frontend SolveClient 와 동일 규칙.
 * subject 의 parentName 이 자격증 이름일 가능성 (예: "SQLD", "정보처리기사 실기", "ADsP").
 */
private fun parentNameToCert(name: String, cert: CertColors): Color = when {
    name.contains("SQLD", ignoreCase = true) -> cert.sqld
    name.contains("실기") -> cert.engineerPractical
    name.contains("필기") && !name.contains("컴퓨터") -> cert.engineerWritten
    name.contains("컴퓨터활용능력 1") || name.contains("컴활 1") -> cert.cl1
    name.contains("컴퓨터활용능력 2") || name.contains("컴활 2") -> cert.cl2
    name.contains("ADsP", ignoreCase = true) -> cert.adsp
    else -> cert.sqld
}

/** 긴 한국어 자격증 명을 짧은 라벨로. */
private fun shortenCertName(name: String): String = when {
    name.contains("SQLD", ignoreCase = true) -> "SQLD"
    name.contains("정보처리기사 실기") -> "정처기 실기"
    name.contains("정보처리기사 필기") -> "정처기 필기"
    name.contains("컴퓨터활용능력 1") -> "컴활 1급"
    name.contains("컴퓨터활용능력 2") -> "컴활 2급"
    name.contains("ADsP", ignoreCase = true) -> "ADsP"
    else -> name
}

@Composable
private fun LoginRequiredHint() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
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
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("로그인이 필요합니다", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Google 로 로그인하면 자격증 과목을 선택해 10문제 세트를 받을 수 있어요. 홈 상단의 로그인 아이콘을 눌러주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectGroupCard(
    parent: String,
    children: List<SubjectResponse>,
    onStart: (Long) -> Unit,
) {
    val cert = LocalSqldpassSemanticColors.current.cert
    val accent = parentNameToCert(parent, cert)
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, CircleShape),
                )
                Text(shortenCertName(parent), style = MaterialTheme.typography.titleMedium)
            }
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
