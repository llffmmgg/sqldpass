package com.sqldpass.app.ui.solve.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqldpass.app.ui.theme.SqldRadius
import com.sqldpass.app.ui.theme.SqldSpacing

/**
 * 단일 채점 풀이 하단 액션 바.
 *
 * 미답 상태: [이전] [정답 확인]   (정답 확인은 hasAnswer 일 때만 활성)
 * 정답 공개 상태: [이전] [다음 문제 / 결과 보기]
 *
 * navigationBarsPadding + imePadding 로 시스템 인셋·키보드 회피.
 */
@Composable
fun SoloBottomActionBar(
    revealed: Boolean,
    hasAnswer: Boolean,
    submitting: Boolean,
    isLastBeforeComplete: Boolean,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = SqldSpacing.lg, vertical = SqldSpacing.base),
        horizontalArrangement = Arrangement.spacedBy(SqldSpacing.md),
    ) {
        if (!revealed) {
            Button(
                onClick = onSubmit,
                enabled = hasAnswer && !submitting,
                shape = RoundedCornerShape(SqldRadius.sm),
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text(if (submitting) "확인중…" else "정답 확인")
            }
        } else {
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(SqldRadius.sm),
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text("채점 완료")
            }
            Button(
                onClick = onNext,
                enabled = !submitting,
                shape = RoundedCornerShape(SqldRadius.sm),
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text(if (isLastBeforeComplete) "결과 보기" else "다음 문제")
            }
        }
    }
}
