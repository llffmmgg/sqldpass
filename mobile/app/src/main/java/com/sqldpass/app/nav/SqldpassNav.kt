package com.sqldpass.app.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

/** 하단 탭으로 노출되는 5개 destination + 풀스크린 destination 들. */
sealed class SqldpassRoute(val route: String) {
    data object Home : SqldpassRoute("home")
    data object MockExam : SqldpassRoute("mock-exams")
    data object PastExam : SqldpassRoute("past-exams")
    data object Solve : SqldpassRoute("solve")
    data object Dashboard : SqldpassRoute("dashboard")

    /** 풀스크린 — 탭에 안 보임. */
    data object PassPlus : SqldpassRoute("passplus")

    /** 풀이 — mode 파라미터로 어느 컨텍스트인지 식별. */
    data object Runner : SqldpassRoute("runner")

    /** 결과 — Runner 와 짝. */
    data object Result : SqldpassRoute("result")
}

data class TabSpec(val route: SqldpassRoute, val label: String, val icon: ImageVector)

val BOTTOM_TABS: List<TabSpec> = listOf(
    TabSpec(SqldpassRoute.Home, "홈", Icons.Outlined.Home),
    TabSpec(SqldpassRoute.MockExam, "모의고사", Icons.Outlined.Quiz),
    TabSpec(SqldpassRoute.PastExam, "기출복원", Icons.Outlined.History),
    TabSpec(SqldpassRoute.Solve, "문제풀기", Icons.Outlined.EditNote),
    TabSpec(SqldpassRoute.Dashboard, "대시보드", Icons.Outlined.BarChart),
)

/** 하단 탭 표시 여부 — 풀스크린(runner/result/passplus) 은 숨김. */
fun isBottomTabRoute(route: String?): Boolean =
    route != null && BOTTOM_TABS.any { it.route.route == route }
