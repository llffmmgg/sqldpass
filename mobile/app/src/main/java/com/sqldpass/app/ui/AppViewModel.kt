package com.sqldpass.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sqldpass.app.data.AppRepository
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.BookmarkSummary
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.data.OverallAvgResponse
import com.sqldpass.app.data.PastExamAnswer
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.data.QuestionDetailResponse
import com.sqldpass.app.data.QuestionResponse
import com.sqldpass.app.data.QuotaInfo
import com.sqldpass.app.data.QuotaResponse
import com.sqldpass.app.data.SolveAnswerRequest
import com.sqldpass.app.data.SolveSummary
import com.sqldpass.app.data.StreakResponse
import com.sqldpass.app.data.SubjectResponse
import com.sqldpass.app.data.SyncResult
import com.sqldpass.app.data.TokenStore
import com.sqldpass.app.ui.runner.RunnerAnswerDraft
import com.sqldpass.app.ui.runner.RunnerMode
import com.sqldpass.app.ui.runner.RunnerQuestion
import com.sqldpass.app.ui.runner.RunnerResult
import com.sqldpass.app.ui.runner.RunnerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardData(
    val streak: StreakResponse? = null,
    val overallAvg: OverallAvgResponse? = null,
    val bestScores: List<BestScoreSummary> = emptyList(),
    val dailyCounts: List<com.sqldpass.app.data.DailyCountResponse> = emptyList(),
)

/**
 * 단일 채점 풀이(SoloSolve) 세션 상태.
 *
 * 웹 frontend/src/app/solve/SolveClient.tsx 의 phase="solve" 상태와 동치.
 * 1문제씩 즉시 채점 → 정답 공개 → 다음 문제 흐름.
 *
 * - queue: 현재 풀이 큐(첫 항목이 current 가 된 직후 currentIndex 로 추적)
 * - sessionQuestions: 이번 세션의 원본 10문 (replaySame 용)
 * - solvedCount/correctCount: 누적 카운트 (마지막 SET_SIZE 도달 시 sessionComplete)
 * - revealed/detail: revealed=true 시 detail 의 정답·해설을 보여주는 단계
 */
data class SoloSession(
    val subjectId: Long,
    val subjectName: String,
    val sessionQuestions: List<QuestionResponse>,
    val queue: List<QuestionResponse>,
    val currentIndex: Int = 0,
    val solvedCount: Int = 0,
    val correctCount: Int = 0,
    val selectedOption: Int? = null,
    val answerText: String = "",
    val revealed: Boolean = false,
    val detail: QuestionDetailResponse? = null,
    val sessionComplete: Boolean = false,
    val submitting: Boolean = false,
    val submitError: String? = null,
) {
    val current: QuestionResponse? get() = queue.firstOrNull()
    val isLast: Boolean get() = solvedCount >= SOLO_SET_SIZE - 1
    val hasAnswer: Boolean
        get() = when (current?.questionType?.uppercase()) {
            "MCQ", null -> selectedOption != null
            else -> answerText.trim().isNotEmpty()
        }
}

/** 한 세트의 문제 개수 (웹 SolveClient.SET_SIZE 동치). */
const val SOLO_SET_SIZE = 10

data class AppUiState(
    val loading: Boolean = false,
    val nickname: String? = null,
    val mockExams: List<MockExamSummary> = emptyList(),
    val syncResult: SyncResult? = null,
    val message: String? = null,
    val certSlugs: List<String> = listOf(
        "sqld",
        "engineer",
        "engineer-written",
        "computer-literacy-1",
        "computer-literacy-2",
        "adsp",
    ),
    val selectedCertSlug: String = "sqld",
    val pastExamsByCert: Map<String, List<PastExamSummary>> = emptyMap(),
    val pastExamsLoading: Boolean = false,
    val subjects: List<SubjectResponse> = emptyList(),
    val subjectsLoading: Boolean = false,
    val runner: RunnerSession? = null,
    val runnerSubmitting: Boolean = false,
    val runnerResult: RunnerResult? = null,
    val runnerBookmarks: Set<Long> = emptySet(),
    val dashboard: DashboardData? = null,
    val dashboardLoading: Boolean = false,
    val subscription: com.sqldpass.app.data.SubscriptionResponse? = null,
    val wrongAnswerStats: List<com.sqldpass.app.data.WrongAnswerStatsSummary> = emptyList(),
    val wrongAnswers: List<com.sqldpass.app.data.WrongAnswerSummary> = emptyList(),
    val wrongAnswersLoading: Boolean = false,
    val memberMe: com.sqldpass.app.data.MemberMeResponse? = null,
    val passplusOpen: Boolean = false,
    val soloSession: SoloSession? = null,
    val bookmarks: List<BookmarkSummary> = emptyList(),
    val bookmarksLoading: Boolean = false,
    val bookmarksError: String? = null,
    val history: List<SolveSummary> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    /** 무료 일일 한도 초과 페이월 — non-null 이면 [com.sqldpass.app.ui.common.QuotaPaywallSheet] 표시. */
    val quotaPaywall: QuotaInfo? = null,
    /**
     * GET /api/quota 사전 표시 캐시. MockExamTab/SolveTab 진입 시 [com.sqldpass.app.ui.AppViewModel.refreshQuota] 호출.
     * questionLimit/mockLimit 가 null 이면 활성 구독자 — [QuotaBadge] 가 표시 숨김.
     */
    val quota: QuotaResponse? = null,
)

class AppViewModel(
    private val repository: AppRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState(nickname = tokenStore.nickname))
    val state: StateFlow<AppUiState> = _state

    /**
     * 오프라인 큐 미동기화 카운트. SoloSolveScreen 의 "오프라인 — N개 보관 중" 인디케이터 노출에 사용.
     * Room Flow → stateIn 으로 ViewModel scope.
     */
    val pendingSolveCount: StateFlow<Int> =
        repository.pendingSolveCountFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // 비로그인 콜드 스타트에서 /api/mock-exams 가 401 응답까지 100~300ms 블록되는 비용
        // 회피. 로그인된 상태일 때만 즉시 refresh, 아니면 onAuthChanged() 경로에 맡김.
        if (!tokenStore.token.isNullOrBlank()) refresh()
        if (!tokenStore.token.isNullOrBlank()) loadSubscription()
    }

    /** 네트워크 복귀 콜백 등이 호출 — 비차단으로 큐 drain. */
    fun tryDrainPendingSolves() {
        viewModelScope.launch {
            runCatching { repository.drainPendingSolves() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null, nickname = tokenStore.nickname) }
            runCatching { repository.mockExams() }
                .onSuccess { exams -> _state.update { it.copy(mockExams = exams, loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.message) } }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "콘텐츠 동기화 중") }
            runCatching {
                val result = repository.syncContent()
                repository.drainPendingSolves()
                result
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        loading = false,
                        syncResult = result,
                        message = "오프라인 콘텐츠가 준비됐습니다.",
                    )
                }
                refresh()
            }.onFailure { e ->
                _state.update { it.copy(loading = false, message = e.message) }
            }
        }
    }

    fun onAuthChanged() {
        _state.update { it.copy(nickname = tokenStore.nickname) }
        // 로그인 직후: 인증 필수 fetch 들 한꺼번에 트리거. 로그아웃 시엔 그대로.
        if (!tokenStore.token.isNullOrBlank()) {
            refresh()
            loadSubjects()
            loadDashboard()
            loadWrongAnswerStats()
            loadSubscription()
        } else {
            // 로그아웃 시 보호된 상태 초기화
            _state.update {
                it.copy(
                    subjects = emptyList(),
                    dashboard = null,
                    wrongAnswerStats = emptyList(),
                    subscription = null,
                )
            }
        }
    }

    fun setMessage(message: String?) {
        _state.update { it.copy(message = message) }
    }

    /**
     * QuotaInterceptor 가 HTTP 402 를 잡았을 때 호출되는 전역 enter point.
     * Bottom Sheet 표시는 [com.sqldpass.app.MainActivity.SqldpassApp] 최상위에서 collect.
     */
    fun showQuotaPaywall(info: QuotaInfo) {
        _state.update { it.copy(quotaPaywall = info) }
    }

    fun dismissQuotaPaywall() {
        _state.update { it.copy(quotaPaywall = null) }
    }

    /**
     * MockExamTab / SolveTab 진입 시 호출. 비로그인이면 무시(401 노이즈 회피).
     * 결과는 [AppUiState.quota] 에 저장 — limit null 이면 [QuotaBadge] 가 표시 숨김.
     */
    fun refreshQuota() {
        if (tokenStore.token.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { repository.fetchQuota() }
                .onSuccess { q -> _state.update { it.copy(quota = q) } }
            // 실패 시: 조용히 무시 — 배지 미표시는 회귀 아님(이전 캐시 유지).
        }
    }

    fun toggleBookmark(questionId: Long) {
        val isBookmarked = _state.value.runnerBookmarks.contains(questionId)
        // optimistic
        val previousList = _state.value.bookmarks
        _state.update {
            it.copy(
                runnerBookmarks = if (isBookmarked) it.runnerBookmarks - questionId
                else it.runnerBookmarks + questionId,
                bookmarks = if (isBookmarked) it.bookmarks.filterNot { b -> b.questionId == questionId }
                else it.bookmarks,
            )
        }
        viewModelScope.launch {
            runCatching {
                if (isBookmarked) repository.removeBookmark(questionId)
                else repository.addBookmark(questionId)
            }.onFailure { e ->
                // revert
                _state.update {
                    it.copy(
                        runnerBookmarks = if (isBookmarked) it.runnerBookmarks + questionId
                        else it.runnerBookmarks - questionId,
                        bookmarks = if (isBookmarked) previousList else it.bookmarks,
                        message = "즐겨찾기 변경 실패: ${e.message}",
                    )
                }
            }
        }
    }

    /** BookmarksScreen 진입 시 호출 — 인증 가드 후 GET /api/bookmarks. */
    fun loadBookmarks() {
        if (_state.value.bookmarksLoading) return
        if (tokenStore.token.isNullOrBlank()) {
            _state.update { it.copy(bookmarks = emptyList(), bookmarksError = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(bookmarksLoading = true, bookmarksError = null) }
            runCatching { repository.bookmarks() }
                .onSuccess { resp ->
                    _state.update {
                        it.copy(
                            bookmarks = resp.items,
                            bookmarksLoading = false,
                            bookmarksError = null,
                            // 화면 진입 시 runnerBookmarks 도 서버 상태와 동기화
                            runnerBookmarks = resp.items.map { b -> b.questionId }.toSet(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(bookmarksLoading = false, bookmarksError = e.message)
                    }
                }
        }
    }

    /** HistoryScreen 진입 시 호출 — 인증 가드 후 GET /api/solves. */
    fun loadHistory() {
        if (_state.value.historyLoading) return
        if (tokenStore.token.isNullOrBlank()) {
            _state.update { it.copy(history = emptyList(), historyError = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(historyLoading = true, historyError = null) }
            runCatching { repository.mySolves() }
                .onSuccess { list ->
                    _state.update {
                        it.copy(history = list, historyLoading = false, historyError = null)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(historyLoading = false, historyError = e.message)
                    }
                }
        }
    }

    fun submitFeedback(type: String, questionId: Long?, content: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.reportFeedback(type, questionId, content, pageUrl = "mobile://runner")
            }.onSuccess {
                _state.update { it.copy(message = "신고가 접수됐습니다. 감사합니다.") }
                onDone(true)
            }.onFailure { e ->
                _state.update { it.copy(message = "신고 전송 실패: ${e.message}") }
                onDone(false)
            }
        }
    }

    fun loadSubscription() {
        if (tokenStore.token.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { repository.subscription() }
                .onSuccess { sub -> _state.update { it.copy(subscription = sub) } }
        }
    }

    fun checkPaymentEligibility(onDone: (Boolean) -> Unit) {
        if (tokenStore.token.isNullOrBlank()) {
            _state.update { it.copy(message = "로그인 후 구매할 수 있습니다.") }
            onDone(false)
            return
        }
        viewModelScope.launch {
            runCatching { repository.paymentEligible() }
                .onSuccess { eligible ->
                    if (!eligible) {
                        _state.update { it.copy(message = "현재 결제 테스트 대상 계정이 아닙니다.") }
                    }
                    onDone(eligible)
                }
                .onFailure { e ->
                    _state.update { it.copy(message = "결제 가능 여부 확인 실패: ${e.message}") }
                    onDone(false)
                }
        }
    }

    fun loadMe() {
        if (tokenStore.token.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { repository.me() }
                .onSuccess { me -> _state.update { it.copy(memberMe = me) } }
        }
    }

    fun openPassPlus() { _state.update { it.copy(passplusOpen = true) } }

    fun closePassPlus() { _state.update { it.copy(passplusOpen = false) } }

    fun loadWrongAnswerStats() {
        viewModelScope.launch {
            runCatching { repository.wrongAnswerStats() }
                .onSuccess { stats -> _state.update { it.copy(wrongAnswerStats = stats) } }
        }
    }

    fun loadWrongAnswers(subjectId: Long? = null) {
        if (_state.value.wrongAnswersLoading) return
        if (tokenStore.token.isNullOrBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(wrongAnswersLoading = true) }
            runCatching { repository.wrongAnswers(subjectId) }
                .onSuccess { list ->
                    _state.update { it.copy(wrongAnswers = list, wrongAnswersLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(wrongAnswersLoading = false, message = e.message) }
                }
        }
    }

    /**
     * 선택된 오답 문제만 모아 RunnerSession 시작.
     * subjectId 는 모든 문제가 동일 과목일 때만 채운다. 섞여 있으면 BOOKMARK source 로
     * subjectId 없이 제출해 백엔드의 수집형 풀이 계약을 사용한다.
     */
    fun startWrongAnswersFromQuestions(
        items: List<com.sqldpass.app.data.WrongAnswerSummary>,
        title: String,
    ) {
        if (items.isEmpty()) {
            _state.update { it.copy(message = "선택된 오답이 없습니다.") }
            return
        }
        val uniformSubjectId = items.map { it.subjectId }.distinct().singleOrNull()
        val session = RunnerSession(
            mode = RunnerMode.WRONG_ANSWERS,
            title = title,
            originId = uniformSubjectId ?: -1L,
            questions = items.mapIndexed { idx, q ->
                RunnerQuestion(q.questionId, idx + 1, q.questionContent, "MCQ")
            },
            subjectId = uniformSubjectId,
        )
        _state.update { it.copy(runner = session, runnerResult = null, runnerBookmarks = emptySet()) }
    }

    fun startWrongAnswerRunner(subjectId: Long?, subjectName: String?) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.wrongAnswers(subjectId) }
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _state.update { it.copy(loading = false, message = "오답이 없습니다.") }
                        return@onSuccess
                    }
                    val session = RunnerSession(
                        mode = RunnerMode.WRONG_ANSWERS,
                        title = "오답 다시 풀기${subjectName?.let { " · $it" } ?: ""}",
                        originId = subjectId ?: -1L,
                        questions = items.mapIndexed { idx, q ->
                            RunnerQuestion(q.questionId, idx + 1, q.questionContent, "MCQ")
                        },
                        subjectId = subjectId,
                    )
                    _state.update {
                        it.copy(loading = false, runner = session, runnerResult = null)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message) }
                }
        }
    }

    fun updateNickname(newName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.updateNickname(newName) }
                .onSuccess { me ->
                    tokenStore.nickname = me.nickname
                    _state.update { it.copy(nickname = me.nickname, message = "닉네임이 변경됐습니다.") }
                    onDone(true)
                }
                .onFailure { e ->
                    _state.update { it.copy(message = "닉네임 변경 실패: ${e.message}") }
                    onDone(false)
                }
        }
    }

    fun openHistoryDetail(solveId: Long, onOpened: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.solveDetail(solveId) }
                .onSuccess { solve ->
                    _state.update {
                        it.copy(
                            loading = false,
                            runnerResult = RunnerResult.Solve(solve, RunnerMode.HISTORY),
                        )
                    }
                    onOpened(true)
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = "풀이 기록 상세 조회 실패: ${e.message}") }
                    onOpened(false)
                }
        }
    }

    fun deleteAccount(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deleteAccount() }
                .onSuccess {
                    tokenStore.clear()
                    _state.value = AppUiState(message = "계정이 삭제되었습니다.")
                    onDone(true)
                }
                .onFailure { e ->
                    _state.update { it.copy(message = "계정 삭제 실패: ${e.message}") }
                    onDone(false)
                }
        }
    }

    fun selectCertSlug(slug: String) {
        _state.update { it.copy(selectedCertSlug = slug) }
        loadPastExams(slug)
    }

    fun loadPastExams(slug: String = _state.value.selectedCertSlug) {
        if (_state.value.pastExamsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(pastExamsLoading = true) }
            runCatching { repository.pastExams(slug) }
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            pastExamsByCert = it.pastExamsByCert + (slug to list),
                            pastExamsLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(pastExamsLoading = false, message = e.message) }
                }
        }
    }

    fun loadSubjects() {
        if (_state.value.subjects.isNotEmpty() || _state.value.subjectsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(subjectsLoading = true) }
            runCatching { repository.subjects() }
                .onSuccess { list ->
                    _state.update { it.copy(subjects = list, subjectsLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(subjectsLoading = false, message = e.message) }
                }
        }
    }

    fun loadDashboard() {
        if (_state.value.dashboardLoading) return
        viewModelScope.launch {
            _state.update { it.copy(dashboardLoading = true) }
            val streak = runCatching { repository.streak() }.getOrNull()
            val avg = runCatching { repository.overallAvg() }.getOrNull()
            val best = runCatching { repository.bestScores() }.getOrDefault(emptyList())
            val daily = runCatching { repository.myDailyCounts(14) }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    dashboard = DashboardData(
                        streak = streak,
                        overallAvg = avg,
                        bestScores = best,
                        dailyCounts = daily,
                    ),
                    dashboardLoading = false,
                )
            }
        }
    }

    fun startMockExamRunner(mockExamId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.mockExam(mockExamId) }
                .onSuccess { exam ->
                    val session = RunnerSession(
                        mode = RunnerMode.MOCK_EXAM,
                        title = exam.name,
                        originId = exam.id,
                        questions = exam.questions.map {
                            RunnerQuestion(it.id, it.displayOrder, it.content, it.questionType)
                        },
                        durationSeconds = defaultDurationSeconds(exam.examType),
                    )
                    _state.update {
                        it.copy(loading = false, runner = session, runnerResult = null, runnerBookmarks = emptySet())
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message) }
                }
        }
    }

    fun startPastExamRunner(pastExamId: Long, certSlug: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.pastExam(pastExamId) }
                .onSuccess { exam ->
                    val session = RunnerSession(
                        mode = RunnerMode.PAST_EXAM,
                        title = exam.name,
                        originId = exam.id,
                        questions = exam.questions.map {
                            RunnerQuestion(it.id, it.displayOrder, it.content, it.questionType)
                        },
                        certSlug = certSlug,
                        durationSeconds = defaultDurationSeconds(exam.examType ?: certSlug),
                    )
                    _state.update {
                        it.copy(loading = false, runner = session, runnerResult = null, runnerBookmarks = emptySet())
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message) }
                }
        }
    }

    private fun defaultDurationSeconds(examType: String?): Int = when {
        examType.isNullOrBlank() -> 0
        examType.contains("ENGINEER", ignoreCase = true) -> 150 * 60
        examType.contains("ADSP", ignoreCase = true) -> 90 * 60
        examType.contains("ADP", ignoreCase = true) -> 180 * 60
        examType.contains("SQLD", ignoreCase = true) -> 90 * 60
        examType.contains("COMP", ignoreCase = true) -> 50 * 60
        else -> 60 * 60
    }

    fun startPracticeRunner(subjectId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.randomQuestions(subjectId, 10) }
                .onSuccess { questions ->
                    if (questions.isEmpty()) {
                        _state.update {
                            it.copy(loading = false, message = "이 과목에서 가져올 문제가 없습니다.")
                        }
                        return@onSuccess
                    }
                    val session = RunnerSession(
                        mode = RunnerMode.PRACTICE,
                        title = "랜덤 10문제",
                        originId = subjectId,
                        questions = questions.mapIndexed { idx, q ->
                            RunnerQuestion(q.id, idx + 1, q.content, q.questionType)
                        },
                        subjectId = subjectId,
                    )
                    _state.update {
                        it.copy(loading = false, runner = session, runnerResult = null)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message) }
                }
        }
    }

    fun submitRunner(drafts: List<RunnerAnswerDraft>) {
        val session = _state.value.runner ?: return
        viewModelScope.launch {
            _state.update { it.copy(runnerSubmitting = true, message = null) }
            try {
                val result = when (session.mode) {
                    RunnerMode.MOCK_EXAM -> {
                        val answers = drafts.map {
                            SolveAnswerRequest(it.questionId, it.selectedOption, it.answerText)
                        }
                        val response = repository.submitMockExam(session.originId, answers)
                        RunnerResult.Solve(response, RunnerMode.MOCK_EXAM)
                    }
                    RunnerMode.PRACTICE, RunnerMode.WRONG_ANSWERS -> {
                        val answers = drafts.map {
                            SolveAnswerRequest(it.questionId, it.selectedOption, it.answerText)
                        }
                        val response = if (session.mode == RunnerMode.WRONG_ANSWERS && session.subjectId == null) {
                            repository.submitCollectedAnswers(answers)
                        } else if (session.subjectId != null) {
                            repository.submitPractice(session.subjectId, answers)
                        } else {
                            error("풀이 과목 정보가 없습니다.")
                        }
                        RunnerResult.Solve(response, session.mode)
                    }
                    RunnerMode.PAST_EXAM -> {
                        val answers = drafts.map {
                            PastExamAnswer(it.questionId, it.selectedOption, it.answerText)
                        }
                        val response = repository.gradePastExam(session.originId, answers)
                        RunnerResult.PastExam(response)
                    }
                    RunnerMode.HISTORY -> error("기록 상세는 제출할 수 없습니다.")
                }
                _state.update {
                    it.copy(
                        runnerSubmitting = false,
                        runner = null,
                        runnerResult = result,
                    )
                }
                refresh()
            } catch (e: Exception) {
                _state.update {
                    it.copy(runnerSubmitting = false, message = e.message ?: "제출 실패")
                }
            }
        }
    }

    fun cancelRunner() {
        _state.update { it.copy(runner = null, runnerSubmitting = false) }
    }

    fun dismissResult() {
        _state.update { it.copy(runnerResult = null) }
    }

    fun restartLastPractice() {
        val last = _state.value.runnerResult
        if (last is RunnerResult.Solve && last.mode == RunnerMode.PRACTICE) {
            // Re-derive subjectId from last session — keep state minimal by recomputing.
            // The result carries SolveResponse which has subjectId. Use it.
            val subjectId = last.response.subjectId ?: return
            _state.update { it.copy(runnerResult = null) }
            startPracticeRunner(subjectId)
        }
    }

    // ── Solo Solve (단일 채점 풀이) ────────────────────────────────────

    /**
     * 단일 채점 풀이 세션 시작.
     * 과목 선택 직후 호출되어 SET_SIZE(10) 문제를 fetch 한 뒤 첫 문제로 진입한다.
     */
    fun startSoloSolve(subjectId: Long, subjectName: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.randomQuestions(subjectId, SOLO_SET_SIZE) }
                .onSuccess { questions ->
                    if (questions.isEmpty()) {
                        _state.update {
                            it.copy(loading = false, message = "이 과목에서 가져올 문제가 없습니다.")
                        }
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            soloSession = SoloSession(
                                subjectId = subjectId,
                                subjectName = subjectName,
                                sessionQuestions = questions,
                                queue = questions,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message) }
                }
        }
    }

    fun soloSelectOption(option: Int) {
        _state.update { st ->
            val s = st.soloSession ?: return@update st
            if (s.revealed) return@update st
            st.copy(soloSession = s.copy(selectedOption = option))
        }
    }

    fun soloSetAnswerText(text: String) {
        _state.update { st ->
            val s = st.soloSession ?: return@update st
            if (s.revealed) return@update st
            st.copy(soloSession = s.copy(answerText = text))
        }
    }

    /**
     * 현재 문제 채점. 정답/해설은 GET /api/questions/{id} 응답으로 즉시 표시(클라이언트 측 채점).
     * 풀이 기록은 백그라운드로 POST /api/solves — 실패해도 진행 흐름 막지 않음.
     */
    fun soloSubmit() {
        val session = _state.value.soloSession ?: return
        if (session.revealed || session.submitting) return
        val current = session.current ?: return
        if (!session.hasAnswer) return

        _state.update { it.copy(soloSession = session.copy(submitting = true, submitError = null)) }
        viewModelScope.launch {
            runCatching { repository.questionDetail(current.id) }
                .onSuccess { detail ->
                    val correct = isSoloCorrect(detail, session.selectedOption, session.answerText)
                    _state.update { st ->
                        val s = st.soloSession ?: return@update st
                        st.copy(
                            soloSession = s.copy(
                                submitting = false,
                                revealed = true,
                                detail = detail,
                                solvedCount = s.solvedCount + 1,
                                correctCount = s.correctCount + if (correct) 1 else 0,
                            ),
                        )
                    }
                    // 백그라운드 풀이 기록 — 실패해도 화면 진행 흐름 막지 않음.
                    if (!tokenStore.token.isNullOrBlank()) {
                        runCatching {
                            repository.submitSoloAnswer(
                                subjectId = session.subjectId,
                                questionId = current.id,
                                selectedOption = if (isShortAnswerType(current.questionType)) null
                                else session.selectedOption,
                                answerText = if (isShortAnswerType(current.questionType)) session.answerText
                                else null,
                            )
                        }.onFailure { e ->
                            _state.update { st ->
                                val s = st.soloSession ?: return@update st
                                st.copy(soloSession = s.copy(submitError = e.message))
                            }
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { st ->
                        val s = st.soloSession ?: return@update st
                        st.copy(soloSession = s.copy(submitting = false, submitError = e.message))
                    }
                }
        }
    }

    /**
     * 다음 문제로. SET_SIZE 도달 시 sessionComplete 로 전이.
     * 큐가 부족하면 추가 fetch (서버에서 새 랜덤).
     */
    fun soloNext() {
        val session = _state.value.soloSession ?: return
        if (!session.revealed) return

        if (session.solvedCount >= SOLO_SET_SIZE) {
            _state.update {
                it.copy(soloSession = session.copy(sessionComplete = true))
            }
            return
        }

        val remaining = session.queue.drop(1)
        if (remaining.isNotEmpty()) {
            _state.update {
                it.copy(
                    soloSession = session.copy(
                        queue = remaining,
                        currentIndex = session.currentIndex + 1,
                        selectedOption = null,
                        answerText = "",
                        revealed = false,
                        detail = null,
                        submitError = null,
                    ),
                )
            }
            return
        }

        // 큐 소진 — 같은 과목에서 추가 fetch
        viewModelScope.launch {
            _state.update {
                it.copy(soloSession = session.copy(submitting = true))
            }
            runCatching { repository.randomQuestions(session.subjectId, SOLO_SET_SIZE) }
                .onSuccess { fresh ->
                    _state.update { st ->
                        val s = st.soloSession ?: return@update st
                        st.copy(
                            soloSession = s.copy(
                                queue = fresh,
                                currentIndex = s.currentIndex + 1,
                                selectedOption = null,
                                answerText = "",
                                revealed = false,
                                detail = null,
                                submitting = false,
                                submitError = null,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { st ->
                        val s = st.soloSession ?: return@update st
                        st.copy(soloSession = s.copy(submitting = false, submitError = e.message))
                    }
                }
        }
    }

    fun soloExit() {
        _state.update { it.copy(soloSession = null) }
    }

    /** 같은 10문제 다시 풀기. */
    fun soloReplaySame() {
        val s = _state.value.soloSession ?: return
        _state.update {
            it.copy(
                soloSession = SoloSession(
                    subjectId = s.subjectId,
                    subjectName = s.subjectName,
                    sessionQuestions = s.sessionQuestions,
                    queue = s.sessionQuestions,
                ),
            )
        }
    }

    /** 같은 과목 새 10문제. */
    fun soloNewRandom() {
        val s = _state.value.soloSession ?: return
        startSoloSolve(s.subjectId, s.subjectName)
    }

    private fun isSoloCorrect(
        detail: QuestionDetailResponse,
        selectedOption: Int?,
        answerText: String,
    ): Boolean {
        val type = detail.questionType?.uppercase()
        if (type == null || type == "MCQ") {
            return selectedOption != null && selectedOption == detail.correctOption
        }
        val normalize: (String) -> String = { it.trim().lowercase().replace(Regex("\\s+"), " ") }
        val submitted = normalize(answerText)
        if (submitted.isEmpty()) return false
        detail.answer?.let { if (normalize(it) == submitted) return true }
        return detail.keywords.any { normalize(it) == submitted }
    }

    private fun isShortAnswerType(type: String?): Boolean =
        type.equals("SHORT_ANSWER", ignoreCase = true) ||
            type.equals("DESCRIPTIVE", ignoreCase = true)
}

class AppViewModelFactory(
    private val repository: AppRepository,
    private val tokenStore: TokenStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(repository, tokenStore) as T
    }
}
