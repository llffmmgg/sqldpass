package com.sqldpass.app.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.SubscriptionResponse
import com.sqldpass.app.data.ThemeMode
import com.sqldpass.app.text.formatKstDate
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.CtaCard
import com.sqldpass.app.ui.common.HeroHeader
import com.sqldpass.app.ui.common.MenuListRow
import com.sqldpass.app.ui.dashboard.NicknameEditDialog

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun ProfileTab(
    state: AppUiState,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSync: () -> Unit = {},
    onUpdateNickname: (String, (Boolean) -> Unit) -> Unit = { _, cb -> cb(false) },
    onSubmitFeedback: (String, Long?, String, (Boolean) -> Unit) -> Unit = { _, _, _, cb -> cb(false) },
    onOpenPassPlus: () -> Unit = {},
    onOpenWrongAnswers: () -> Unit = {},
    onLoadMe: () -> Unit = {},
    onLoadSubscription: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onThemeChange: (ThemeMode) -> Unit = {},
) {
    LaunchedEffect(state.nickname) {
        if (state.nickname != null) {
            onLoadMe()
            onLoadSubscription()
        }
    }

    var nicknameDialogOpen by remember { mutableStateOf(false) }
    var nicknameSubmitting by remember { mutableStateOf(false) }
    var feedbackDialogOpen by remember { mutableStateOf(false) }
    var pendingNotice by remember { mutableStateOf<String?>(null) }

    if (nicknameDialogOpen) {
        NicknameEditDialog(
            currentNickname = state.nickname,
            submitting = nicknameSubmitting,
            onDismiss = { if (!nicknameSubmitting) nicknameDialogOpen = false },
            onSubmit = { newName ->
                nicknameSubmitting = true
                onUpdateNickname(newName) { ok ->
                    nicknameSubmitting = false
                    if (ok) nicknameDialogOpen = false
                }
            },
        )
    }

    if (feedbackDialogOpen) {
        FeedbackDialog(
            onDismiss = { feedbackDialogOpen = false },
            onSubmit = { content, done ->
                onSubmitFeedback("GENERAL", null, content) { ok ->
                    done(ok)
                    if (ok) feedbackDialogOpen = false
                }
            },
        )
    }

    pendingNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { pendingNotice = null },
            confirmButton = { TextButton(onClick = { pendingNotice = null }) { Text("확인") } },
            title = { Text("알림") },
            text = { Text(msg) },
        )
    }

    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            title = "마이",
            subtitle = state.nickname?.let { "$it 님의 학습 공간" } ?: "Google 로 로그인하면 시작돼요.",
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProfileHeaderCard(
                    nickname = state.nickname,
                    createdAt = formatKstDate(state.memberMe?.createdAt),
                    onLogin = onLogin,
                    onEditNickname = { nicknameDialogOpen = true },
                )
            }
            item {
                SubscriptionCard(
                    subscription = state.subscription,
                    onOpenPassPlus = onOpenPassPlus,
                )
            }

            if (state.nickname != null) {
                // KPI 2x2 — longestStreak 만 실데이터, 나머지 3개 placeholder.
                // 누적·평균·합격 확률 백엔드 신설은 별 phase `kpi-backend-support`.
                item {
                    KpiGrid(
                        totalSolved = null,
                        avgCorrectRate = null,
                        longestStreak = state.dashboard?.streak?.longestStreak,
                        passProbability = null,
                    )
                }

                item { SectionTitle("학습") }
                item {
                    MenuListRow(
                        icon = Icons.Outlined.History,
                        label = "오답노트",
                        onClick = onOpenWrongAnswers,
                    )
                }
                item {
                    MenuListRow(
                        icon = Icons.Outlined.Bookmark,
                        label = "북마크한 문제",
                        onClick = { pendingNotice = "북마크 화면은 곧 출시됩니다." },
                    )
                }
                item {
                    MenuListRow(
                        icon = Icons.Outlined.History,
                        label = "풀이 기록",
                        onClick = { pendingNotice = "풀이 기록 화면은 곧 출시됩니다." },
                    )
                }
                item {
                    MenuListRow(
                        icon = Icons.Outlined.CloudDownload,
                        label = "오프라인 콘텐츠 다운로드",
                        onClick = onSync,
                    )
                }

                item { SectionTitle("설정") }
                item {
                    ThemeToggleCard(themeMode = themeMode, onThemeChange = onThemeChange)
                }
                item {
                    MenuListRow(
                        icon = Icons.Outlined.Edit,
                        label = "닉네임 편집",
                        onClick = { nicknameDialogOpen = true },
                    )
                }
            }

            item { SectionTitle("지원") }
            item {
                MenuListRow(
                    icon = Icons.Outlined.Feedback,
                    label = "피드백 보내기",
                    onClick = {
                        if (state.nickname == null) pendingNotice = "로그인 후 이용 가능합니다."
                        else feedbackDialogOpen = true
                    },
                )
            }
            item {
                MenuListRow(
                    icon = Icons.Outlined.Description,
                    label = "이용약관",
                    onClick = { openUrl("https://www.sqldpass.com/terms") },
                )
            }
            item {
                MenuListRow(
                    icon = Icons.Outlined.PrivacyTip,
                    label = "개인정보처리방침",
                    onClick = { openUrl("https://www.sqldpass.com/privacy") },
                )
            }

            if (state.nickname != null) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    MenuListRow(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        label = "로그아웃",
                        onClick = onLogout,
                    )
                }
                item {
                    MenuListRow(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        label = "계정 삭제",
                        onClick = {
                            pendingNotice = "고객센터(heehun3658@gmail.com) 로 문의해주세요."
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    nickname: String?,
    createdAt: String?,
    onLogin: () -> Unit,
    onEditNickname: () -> Unit,
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
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (nickname == null) {
                Text("비로그인", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Google 로 로그인하면 풀이 기록·오답·구독이 동기화됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onLogin,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) { Text("Google 로 로그인") }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onEditNickname) { Text("편집") }
                }
                createdAt?.let {
                    Text(
                        "가입일 $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionResponse?,
    onOpenPassPlus: () -> Unit,
) {
    val active = subscription?.active == true
    val planLabel = subscription?.plan ?: "FREE"
    val expires = formatKstDate(subscription?.expiresAt)
    Card(
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (active) "구독: $planLabel" else "무료 플랜",
                style = MaterialTheme.typography.titleMedium,
            )
            if (active) {
                expires?.let {
                    Text("만료 $it", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    "PASS+ 로 PDF·오답노트·즐겨찾기를 풀어보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    shape = RoundedCornerShape(ButtonCorner),
                    onClick = onOpenPassPlus,
                    modifier = Modifier.sizeIn(minHeight = 44.dp),
                ) { Text("PASS+ 알아보기") }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeToggleCard(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
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
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("화면 테마", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to "시스템",
                    ThemeMode.LIGHT to "라이트",
                    ThemeMode.DARK to "다크",
                ).forEach { (mode, label) ->
                    val selected = themeMode == mode
                    AssistChip(
                        onClick = { onThemeChange(mode) },
                        label = { Text(label) },
                        colors = if (selected) AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) else AssistChipDefaults.assistChipColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, (Boolean) -> Unit) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("피드백 보내기") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                label = { Text("의견 (최대 1000자)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
        },
        confirmButton = {
            Button(
                enabled = !submitting && content.trim().length >= 5,
                onClick = {
                    submitting = true
                    onSubmit(content.trim()) { ok ->
                        submitting = false
                        if (!ok) {
                            // 다이얼로그 유지 — 외부에서 message 표기.
                        }
                    }
                },
            ) { Text(if (submitting) "전송중…" else "보내기") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text("취소") } },
    )
}
