package com.sqldpass.app.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Quiz
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

    /** 단일 채점 풀이 — 웹 /solve 와 동치(1문제씩 즉시 채점). */
    data object SoloSolve : SqldpassRoute("solo-solve")

    /** 북마크 모아보기 — ProfileTab 에서 진입. */
    data object Bookmarks : SqldpassRoute("bookmarks")

    /** 풀이 기록 — ProfileTab 에서 진입. */
    data object History : SqldpassRoute("history")
}

data class TabSpec(val route: SqldpassRoute, val label: String, val icon: ImageVector)

val BOTTOM_TABS: List<TabSpec> = listOf(
    TabSpec(SqldpassRoute.Home, "홈", Icons.Outlined.Home),
    TabSpec(SqldpassRoute.MockExam, "모의고사", Icons.Outlined.Quiz),
    TabSpec(SqldpassRoute.PastExam, "기출복원", Icons.Outlined.History),
    TabSpec(SqldpassRoute.Solve, "실전 문제", Icons.Outlined.PlayCircleOutline),
    TabSpec(SqldpassRoute.Profile, "마이", Icons.Outlined.PersonOutline),
)

/** 하단 탭 표시 여부 — 풀스크린(runner/result/passplus/legacy 라우트) 은 숨김. */
fun isBottomTabRoute(route: String?): Boolean =
    route != null && BOTTOM_TABS.any { it.route.route == route }
