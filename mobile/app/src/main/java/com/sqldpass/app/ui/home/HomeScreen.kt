package com.sqldpass.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sqldpass.app.ui.common.HeroHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun HomeScreen(
    nickname: String?,
    message: String?,
    currentStreak: Int? = null,
    onQuickPractice: () -> Unit,
    onSync: () -> Unit,
    onPurchase: (String) -> Unit,
    heroActions: @Composable () -> Unit = {},
) {
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
        if (nickname != null && currentStreak != null) {
            item { StreakMiniCard(currentStreak) }
        }
        item {
            // 강조 카드 — 좌측 emerald 액센트 바
            com.sqldpass.app.ui.common.AccentCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("오늘 바로 풀기", style = MaterialTheme.typography.titleLarge)
                        com.sqldpass.app.ui.common.SqldpassBadge(
                            label = "NEW",
                            base = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "과목을 골라 10문제 세트로 바로 시작합니다. 필요한 콘텐츠는 앱에 내려받아 둘 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            shape = RoundedCornerShape(ButtonCorner),
                            onClick = onQuickPractice,
                            modifier = Modifier.sizeIn(minHeight = 48.dp),
                        ) { Text("10문제 풀기") }
                        OutlinedButton(
                            shape = RoundedCornerShape(ButtonCorner),
                            onClick = onSync,
                            modifier = Modifier.sizeIn(minHeight = 48.dp),
                        ) { Text("오프라인 준비") }
                    }
                }
            }
        }
        item {
            ActionCard(
                title = "PASS+ 모의고사",
                body = "프리미엄 회차는 구매 후 앱에서도 오프라인 풀이가 가능합니다.",
                action = "PASS+ 보기",
                onClick = { onPurchase("iap_one_month") },
                badgeLabel = "PASS+",
                badgeColor = com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors.current.cert.sqld,
            )
        }
        message?.let { item { StatusCard(it) } }
        }
    }
}

/**
 * TopAppBar 의 actions 슬롯에 들어갈 로그인/로그아웃 아이콘.
 * Dropdown 메뉴 포함 — 로그인 상태일 때만 expand.
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
fun ActionCard(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
    badgeLabel: String? = null,
    badgeColor: Color? = null,
) {
    val haptic = LocalHapticFeedback.current
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (badgeLabel != null && badgeColor != null) {
                    com.sqldpass.app.ui.common.SqldpassBadge(
                        label = badgeLabel,
                        base = badgeColor,
                    )
                }
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Button(
                shape = RoundedCornerShape(ButtonCorner),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) { Text(action) }
        }
    }
}

@Composable
private fun StreakMiniCard(currentStreak: Int) {
    val cert = com.sqldpass.app.ui.theme.LocalSqldpassSemanticColors.current.cert
    val amber = cert.sqld
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val softBg = amber.copy(alpha = if (isDark) 0.18f else 0.10f)
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
                Icons.Outlined.Whatshot,
                contentDescription = null,
                tint = amber,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (currentStreak >= 1) "연속 학습 ${currentStreak}일째"
                    else "오늘 풀이로 streak 을 시작해요",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (currentStreak >= 1) "꾸준함이 합격을 만듭니다."
                    else "한 문제만 풀어도 streak 1일.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
