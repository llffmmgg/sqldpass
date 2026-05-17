package com.sqldpass.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqldpass.app.R
import com.sqldpass.app.ui.theme.SqldpassTheme

private val CardCorner = 14.dp
private val ButtonCorner = 12.dp

@Composable
fun HomeScreen(
    nickname: String?,
    message: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onSync: () -> Unit,
    onPurchase: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BrandHeaderRow(
                nickname = nickname,
                onLogin = onLogin,
                onLogout = onLogout,
            )
        }
        item {
            ActionCard(
                title = "오늘 바로 풀기",
                body = "최근 공개된 모의고사와 다운로드된 문제를 앱에서 바로 이어서 풉니다.",
                action = "오프라인 준비",
                onClick = onSync,
            )
        }
        item {
            ActionCard(
                title = "PASS+ 모의고사",
                body = "프리미엄 회차는 구매 후 앱에서도 오프라인 풀이가 가능합니다.",
                action = "PASS+ 보기",
                onClick = { onPurchase("iap_one_month") },
            )
        }
        message?.let { item { StatusCard(it) } }
    }
}

@Composable
private fun BrandHeaderRow(
    nickname: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "문어CBT 로고",
            modifier = Modifier.size(54.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "문어CBT",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                nickname?.let { "$it 님, 오늘도 한 회차 풀어볼까요?" } ?: "웹과 같은 톤, 더 편한 앱 경험",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (nickname == null) {
            IconButton(
                onClick = onLogin,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.Outlined.Login, contentDescription = "Google 로그인")
            }
        } else {
            IconButton(
                onClick = { menuOpen = true },
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
}

@Composable
fun ActionCard(title: String, body: String, action: String, onClick: () -> Unit) {
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
            Text(title, style = MaterialTheme.typography.titleLarge)
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

@Preview(name = "Home — Light", showBackground = true)
@Composable
private fun HomePreviewLight() {
    SqldpassTheme(darkTheme = false) {
        HomeScreen(
            nickname = "문어",
            message = "오프라인 콘텐츠가 준비됐습니다.",
            onLogin = {}, onLogout = {}, onSync = {}, onPurchase = {},
        )
    }
}

@Preview(name = "Home — Dark", showBackground = true)
@Composable
private fun HomePreviewDark() {
    SqldpassTheme(darkTheme = true) {
        HomeScreen(
            nickname = null,
            message = null,
            onLogin = {}, onLogout = {}, onSync = {}, onPurchase = {},
        )
    }
}

@Preview(name = "Home — Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun HomePreviewLargeFont() {
    SqldpassTheme(darkTheme = false) {
        HomeScreen(
            nickname = "문어",
            message = null,
            onLogin = {}, onLogout = {}, onSync = {}, onPurchase = {},
        )
    }
}
