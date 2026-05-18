package com.sqldpass.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors
import java.time.LocalDate
import java.time.ZoneId

private val CardCorner = 14.dp

/**
 * 홈 탭 — 오늘 상태 + 다음 행동 추천.
 *
 * 위→아래:
 *  1) 인사말 헤더 (HeroHeader)
 *  2) 스트릭 카드 — 위험 톤 분기(오늘 미풀이 + lastSolvedDate ≤ 어제)
 *  3) 자격증 6종 수평 캐러셀 → 카드 탭 시 CertInfoSheet 모달
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1.
 * 이전 phase 에서 있던 "오늘 바로 풀기 CTA"·"PASS+ 모의고사" 카드는 제거됨.
 * 풀이 정문은 실전 문제 탭, PASS+ 진입은 내정보 메뉴 또는 자격증 시트 안 CTA.
 * 이어풀기 카드(`ContinueLastCard`)는 별 phase 에서 lastCert/lastMode 통합 후 추가 예정.
 */
@Composable
fun HomeScreen(
    nickname: String?,
    message: String?,
    streak: StreakResponse? = null,
    onOpenPassPlus: () -> Unit,
    heroActions: @Composable () -> Unit = {},
) {
    var sheetCert by remember { mutableStateOf<CertInfo?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            title = "문어CBT",
            subtitle = nickname?.let { "$it 님, 오늘도 한 회차 풀어볼까요?" }
                ?: "Google 로 로그인하면 학습 기록이 쌓여요.",
            actions = heroActions,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (streak != null && nickname != null) {
                item { StreakCard(streak) }
            }
            item {
                CertCarousel(onCertTap = { sheetCert = it })
            }
            message?.let { item { StatusCard(it) } }
        }
    }

    sheetCert?.let { info ->
        CertInfoSheet(
            info = info,
            onDismiss = { sheetCert = null },
            onOpenPassPlus = {
                sheetCert = null
                onOpenPassPlus()
            },
        )
    }
}

/**
 * 스트릭 카드 — 일반 톤(amber 권장) vs 위험 톤(warning) 분기.
 * 위험 = 현재 스트릭 > 0 이고 lastSolvedDate 가 오늘이 아님 (= 어제 풀고 오늘 안 풂).
 */
@Composable
private fun StreakCard(streak: StreakResponse) {
    val atRisk = isStreakAtRisk(streak)
    val semantic = LocalSqldpassSemanticColors.current
    val accent = if (atRisk) semantic.state.warning else semantic.cert.sqld

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val softBg = accent.copy(alpha = if (isDark) 0.18f else 0.10f)

    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = softBg,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (atRisk) Icons.Outlined.Warning else Icons.Outlined.Whatshot,
                contentDescription = null,
                tint = accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    atRisk -> "오늘 자정이 끝나면 연속 일수가 끊겨요"
                    streak.currentStreak >= 1 -> "연속 학습 ${streak.currentStreak}일째"
                    else -> "오늘 풀이로 streak 을 시작해요"
                }
                val body = when {
                    atRisk -> "한 문제만 풀어도 ${streak.currentStreak}일이 이어집니다."
                    streak.currentStreak >= 1 -> "꾸준함이 합격을 만듭니다."
                    else -> "한 문제만 풀어도 streak 1일."
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun isStreakAtRisk(streak: StreakResponse): Boolean {
    if (streak.currentStreak <= 0) return false
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val lastSolved = streak.lastSolvedDate?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: return false
    return lastSolved.isBefore(today)
}

/**
 * 홈 우상단 로그인/계정 아이콘. 기존 동작 유지.
 */
@Composable
fun HomeAccountMenu(
    nickname: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = LocalContentColor.current)
    if (nickname == null) {
        IconButton(
            onClick = onLogin,
            colors = iconColors,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = "Google 로그인")
        }
    } else {
        IconButton(
            onClick = { menuOpen = true },
            colors = iconColors,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = "계정 메뉴")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("로그아웃") },
                onClick = {
                    menuOpen = false
                    onLogout()
                },
            )
        }
    }
}

@Composable
fun StatusCard(message: String) {
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
