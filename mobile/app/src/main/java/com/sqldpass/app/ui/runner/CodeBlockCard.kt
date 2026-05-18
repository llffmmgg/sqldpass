package com.sqldpass.app.ui.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mac 윈도우 크롬 스타일 코드블록 카드.
 *
 * frontend `frontend/src/components/QuestionCodeBlock.tsx:51-85` 패턴:
 *  - 둥근 모서리 10dp, 어두운 배경 #1E1E22 (라이트/다크 동일 — 코드 가독성)
 *  - 상단 헤더: 빨강/노랑/초록 8dp 점 3개 + 언어 라벨 (회색 mono)
 *  - 본문: FontFamily.Monospace, 14sp, 가로 스크롤 (softWrap=false)
 */
@Composable
fun CodeBlockCard(language: String?, code: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CodeBlockBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CodeBlockHeader(language)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = code,
                    color = CodeBlockText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    ),
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun CodeBlockHeader(language: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBlockHeaderBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrafficLight(MacRed)
        Spacer(Modifier.size(6.dp))
        TrafficLight(MacYellow)
        Spacer(Modifier.size(6.dp))
        TrafficLight(MacGreen)
        Spacer(Modifier.weight(1f))
        language?.takeIf { it.isNotBlank() }?.let { lang ->
            Text(
                text = lang.uppercase(),
                color = CodeBlockLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

@Composable
private fun TrafficLight(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

// 라이트/다크 무관 항상 어두운 톤 — 코드 가독성 위함.
private val CodeBlockBg = Color(0xFF1E1E22)
private val CodeBlockHeaderBg = Color(0xFF2A2A30)
private val CodeBlockText = Color(0xFFE4E4E7)
private val CodeBlockLabel = Color(0xFF9CA3AF)
private val MacRed = Color(0xFFFF5F57)
private val MacYellow = Color(0xFFFEBC2E)
private val MacGreen = Color(0xFF28C840)
