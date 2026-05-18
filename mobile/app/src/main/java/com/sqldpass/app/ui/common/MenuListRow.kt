package com.sqldpass.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Deprecated — Step 4 그룹 D 에서 AppListRow 로 통합.
 *
 * 본 wrapper 는 더 이상 실제 호출처가 없다 (DashboardTab 의 import 잔재만 있었음).
 * 호환을 위해 시그니처는 유지하고, 내부는 [AppListRow] 위임으로 단순화.
 * 새 코드에서는 `AppListRow(...)` 를 직접 호출할 것.
 */
@Deprecated(
    message = "AppListRow 로 통합되었습니다. 직접 AppListRow 를 호출하세요.",
    replaceWith = ReplaceWith(
        "AppListRow(title = label, leadingIcon = icon, trailing = trailing, onClick = onClick)",
    ),
)
@Composable
fun MenuListRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    AppListRow(
        title = label,
        leadingIcon = icon,
        trailing = trailing,
        onClick = onClick,
        modifier = modifier,
    )
}
