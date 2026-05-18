package com.sqldpass.app.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PrivacyTip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqldpass.app.data.SubscriptionResponse
import com.sqldpass.app.data.ThemeMode
import com.sqldpass.app.text.formatKstDate
import com.sqldpass.app.ui.AppUiState
import com.sqldpass.app.ui.common.AppBadge
import com.sqldpass.app.ui.common.AppBadgeTone
import com.sqldpass.app.ui.common.AppBadgeVariant
import com.sqldpass.app.ui.common.AppButton
import com.sqldpass.app.ui.common.AppButtonSize
import com.sqldpass.app.ui.common.AppButtonVariant
import com.sqldpass.app.ui.common.AppCard
import com.sqldpass.app.ui.common.AppCardAccent
import com.sqldpass.app.ui.common.AppCardSurface
import com.sqldpass.app.ui.common.AppChip
import com.sqldpass.app.ui.common.AppDialog
import com.sqldpass.app.ui.common.AppHero
import com.sqldpass.app.ui.common.AppListRow
import com.sqldpass.app.ui.common.AppSectionHeader
import com.sqldpass.app.ui.common.AppTextField
import com.sqldpass.app.ui.dashboard.NicknameEditDialog
import com.sqldpass.app.ui.theme.LocalSqldpassPalette
import com.sqldpass.app.ui.theme.SqldSpacing

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
        AppDialog(
            onDismiss = { pendingNotice = null },
            title = "알림",
            message = msg,
            confirmLabel = "확인",
            onConfirm = { pendingNotice = null },
            dismissLabel = null,
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
        AppHero(
            title = "마이",
            subtitle = state.nickname?.let { "$it 님의 학습 공간" }
                ?: "Google 로 로그인하면 시작돼요.",
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

                item { AppSectionHeader(title = "학습") }
                item {
                    AppListRow(
                        title = "오답노트",
                        leadingIcon = Icons.Outlined.History,
                        onClick = onOpenWrongAnswers,
                    )
                }
                item {
                    AppListRow(
                        title = "북마크한 문제",
                        leadingIcon = Icons.Outlined.Bookmark,
                        onClick = { pendingNotice = "북마크 화면은 곧 출시됩니다." },
                    )
                }
                item {
                    AppListRow(
                        title = "풀이 기록",
                        leadingIcon = Icons.Outlined.History,
                        onClick = { pendingNotice = "풀이 기록 화면은 곧 출시됩니다." },
                    )
                }
                item {
                    AppListRow(
                        title = "오프라인 콘텐츠 다운로드",
                        leadingIcon = Icons.Outlined.CloudDownload,
                        onClick = onSync,
                    )
                }

                item { AppSectionHeader(title = "설정") }
                item {
                    ThemeToggleCard(themeMode = themeMode, onThemeChange = onThemeChange)
                }
                item {
                    AppListRow(
                        title = "닉네임 편집",
                        leadingIcon = Icons.Outlined.Edit,
                        onClick = { nicknameDialogOpen = true },
                    )
                }
            }

            item { AppSectionHeader(title = "지원") }
            item {
                AppListRow(
                    title = "피드백 보내기",
                    leadingIcon = Icons.Outlined.Feedback,
                    onClick = {
                        if (state.nickname == null) pendingNotice = "로그인 후 이용 가능합니다."
                        else feedbackDialogOpen = true
                    },
                )
            }
            item {
                AppListRow(
                    title = "이용약관",
                    leadingIcon = Icons.Outlined.Description,
                    onClick = { openUrl("https://www.sqldpass.com/terms") },
                )
            }
            item {
                AppListRow(
                    title = "개인정보처리방침",
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    onClick = { openUrl("https://www.sqldpass.com/privacy") },
                )
            }

            if (state.nickname != null) {
                item { Spacer(Modifier.height(SqldSpacing.xs)) }
                item {
                    AppListRow(
                        title = "로그아웃",
                        leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                        destructive = true,
                        onClick = onLogout,
                    )
                }
                item {
                    AppListRow(
                        title = "계정 삭제",
                        leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                        destructive = true,
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
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        if (nickname == null) {
            Text(
                "비로그인",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Text(
                "Google 로 로그인하면 풀이 기록·오답·구독이 동기화됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
            Spacer(Modifier.height(SqldSpacing.xs))
            AppButton(
                text = "Google 로 로그인",
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Regular,
                leadingIcon = Icons.Outlined.AccountCircle,
                onClick = onLogin,
            )
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
                    color = palette.textPrimary,
                )
                AppButton(
                    text = "편집",
                    variant = AppButtonVariant.Tertiary,
                    size = AppButtonSize.Compact,
                    onClick = onEditNickname,
                )
            }
            createdAt?.let {
                Text(
                    "가입일 $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionResponse?,
    onOpenPassPlus: () -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    val active = subscription?.active == true
    val planLabel = subscription?.plan ?: "FREE"
    val expires = formatKstDate(subscription?.expiresAt)

    AppCard(
        surface = AppCardSurface.Card,
        accent = if (active) AppCardAccent.Success else AppCardAccent.None,
    ) {
        if (active) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "구독",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                AppBadge(
                    label = planLabel,
                    tone = AppBadgeTone.Success,
                    variant = AppBadgeVariant.Solid,
                )
            }
            expires?.let {
                Text(
                    "만료 $it",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textMuted,
                )
            }
        } else {
            Text(
                "무료 플랜",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Text(
                "PASS+ 로 PDF·오답노트·즐겨찾기를 풀어보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
            Spacer(Modifier.height(SqldSpacing.xs))
            AppButton(
                text = "PASS+ 알아보기",
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Regular,
                onClick = onOpenPassPlus,
            )
        }
    }
}

@Composable
private fun ThemeToggleCard(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val palette = LocalSqldpassPalette.current
    AppCard(surface = AppCardSurface.Card) {
        Text(
            "화면 테마",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(SqldSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                ThemeMode.SYSTEM to "시스템",
                ThemeMode.LIGHT to "라이트",
                ThemeMode.DARK to "다크",
            ).forEach { (mode, label) ->
                AppChip(
                    label = label,
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                )
            }
        }
    }
}

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, (Boolean) -> Unit) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val canSubmit = !submitting && content.trim().length >= 5

    AppDialog(
        onDismiss = { if (!submitting) onDismiss() },
        title = "피드백 보내기",
        content = {
            AppTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                label = "의견 (최대 1000자)",
                helper = "${content.length}/1000",
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmLabel = if (submitting) "전송중…" else "보내기",
        onConfirm = {
            if (canSubmit) {
                submitting = true
                onSubmit(content.trim()) { ok ->
                    submitting = false
                    if (!ok) {
                        // 다이얼로그 유지 — 외부에서 message 표기.
                    }
                }
            }
        },
        dismissLabel = "취소",
        onDismissAction = { if (!submitting) onDismiss() },
    )
}
