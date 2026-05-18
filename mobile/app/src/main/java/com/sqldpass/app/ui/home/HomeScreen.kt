package com.sqldpass.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppDropdown
import com.sqldpass.app.ui.common.AppDropdownItem
import com.sqldpass.app.ui.common.AppHero
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing
import java.time.LocalDate
import java.time.ZoneId

/**
 * 홈 탭 — 오늘 상태 + 다음 행동 추천.
 *
 * iOS HomeView (`ios/Sqldpass/Features/Home/HomeView.swift`) 와 정보 위계 1:1 미러:
 *  1) AppHero (eyebrow="문어CBT", title="{닉네임}님, 오늘도 한 회차 풀어볼까요?",
 *              subtitle = solvedToday 분기)
 *  2) 스트릭 카드 — 위험 톤 분기(오늘 미풀이 + lastSolvedDate ≤ 어제)
 *  3) 자격증 6종 수평 캐러셀 → 카드 탭 시 CertInfoSheet 모달
 *  4) 상태 메시지(있을 때) — iOS errorBanner 동등 자리
 *
 * 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1.
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
    val solvedToday = streak?.let { isSolvedToday(it) } == true
    val displayName = nickname ?: "학습자"
    val heroTitle = if (nickname != null) {
        "${displayName}님, 오늘도 한 회차 풀어볼까요?"
    } else {
        "오늘도 한 회차 풀어볼까요?"
    }
    val heroSubtitle = when {
        nickname == null -> "Google 로 로그인하면 학습 기록이 쌓여요."
        solvedToday -> "오늘 학습 완료. 내일도 같은 시간에 이어가면 좋아요."
        else -> "짧게라도 한 세트를 풀고 연속 학습을 이어가세요."
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHero(
            eyebrow = "문어CBT",
            title = heroTitle,
            subtitle = heroSubtitle,
            actions = { heroActions() },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SqldSpacing.lg - 4.dp,
                end = SqldSpacing.lg - 4.dp,
                top = SqldSpacing.base,
                bottom = SqldSpacing.lg - 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(SqldSpacing.md + 2.dp),
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

/** iOS HomeView 의 `viewModel.streak?.solvedToday` 동등 — KST 기준 lastSolvedDate == today. */
private fun isSolvedToday(streak: StreakResponse): Boolean {
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val lastSolved = streak.lastSolvedDate?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: return false
    return lastSolved == today
}

/**
 * 스트릭 카드 — 일반 톤(amber 권장) vs 위험 톤(warning) 분기.
 * 위험 = 현재 스트릭 > 0 이고 lastSolvedDate 가 오늘이 아님 (= 어제 풀고 오늘 안 풂).
 */
@Composable
private fun StreakCard(streak: StreakResponse) {
    val palette = LocalSqldpassPalette.current
    val atRisk = isStreakAtRisk(streak)
    val accent = if (atRisk) palette.warning else palette.certSqld

    AppCard(
        surface = AppCardSurface.Card,
        accent = if (atRisk) AppCardAccent.Warning else AppCardAccent.Sqld,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm + 2.dp),
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
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
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
 *
 * Step 4 그룹 D: Material3 IconButton → Box.clickable.sizeIn(48dp){Icon} 으로 교체.
 * AppDropdown 은 Step 3 에서 이미 적용. 본 그룹은 IconButton chrome 만.
 */
@Composable
fun HomeAccountMenu(
    nickname: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val tint = LocalContentColor.current
    if (nickname == null) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = SqldSpacing.xxl, minHeight = SqldSpacing.xxl)
                .clickable(role = Role.Button, onClick = onLogin)
                .semantics { contentDescription = "Google 로그인" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Login,
                contentDescription = null,
                tint = tint,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = SqldSpacing.xxl, minHeight = SqldSpacing.xxl)
                .clickable(role = Role.Button, onClick = { menuOpen = true })
                .semantics { contentDescription = "계정 메뉴" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = tint,
            )
        }
        AppDropdown(expanded = menuOpen, onDismiss = { menuOpen = false }) {
            AppDropdownItem(
                label = "로그아웃",
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
    val palette = LocalSqldpassPalette.current
    AppCard(
        surface = AppCardSurface.Card,
        accent = AppCardAccent.Info,
    ) {
        Text(
            message,
            modifier = Modifier.padding(vertical = SqldSpacing.xxs),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
        )
    }
}
