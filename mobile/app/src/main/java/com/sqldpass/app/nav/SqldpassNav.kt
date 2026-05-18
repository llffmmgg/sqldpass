package com.sqldpass.app.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.ui.graphics.vector.ImageVector

/** 하단 탭 + 풀스크린 destination. */
sealed class SqldpassRoute(val route: String) {
    /** 탭 */
    data object Home : SqldpassRoute("home")
    data object MockExam : SqldpassRoute("mock-exams")
    data object WrongAnswers : SqldpassRoute("wrong-answers")
    data object Insights : SqldpassRoute("insights")
    data object Profile : SqldpassRoute("profile")

    /** 풀스크린 / 라우트 (탭에서 빠짐). */
    data object PastExam : SqldpassRoute("past-exams")
    data object Solve : SqldpassRoute("solve")
    data object Dashboard : SqldpassRoute("dashboard")
    data object PassPlus : SqldpassRoute("passplus")

    /** 풀이 — mode 파라미터로 컨텍스트 식별. */
    data object Runner : SqldpassRoute("runner")

    /** 결과 — Runner 와 짝. */
    data object Result : SqldpassRoute("result")
}

data class TabSpec(val route: SqldpassRoute, val label: String, val icon: ImageVector)

val BOTTOM_TABS: List<TabSpec> = listOf(
    TabSpec(SqldpassRoute.Home, "홈", Icons.Outlined.Home),
    TabSpec(SqldpassRoute.MockExam, "모의고사", Icons.Outlined.Quiz),
    TabSpec(SqldpassRoute.WrongAnswers, "오답노트", Icons.Outlined.Replay),
    TabSpec(SqldpassRoute.Insights, "인사이트", Icons.Outlined.BarChart),
    TabSpec(SqldpassRoute.Profile, "마이", Icons.Outlined.PersonOutline),
)

/** 하단 탭 표시 여부 — 풀스크린(runner/result/passplus/legacy 라우트) 은 숨김. */
fun isBottomTabRoute(route: String?): Boolean =
    route != null && BOTTOM_TABS.any { it.route.route == route }
