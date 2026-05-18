package com.sqldpass.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sqldpass.app.data.AppRepository
import com.sqldpass.app.data.BestScoreSummary
import com.sqldpass.app.data.MockExamSummary
import com.sqldpass.app.data.OverallAvgResponse
import com.sqldpass.app.data.PastExamAnswer
import com.sqldpass.app.data.PastExamSummary
import com.sqldpass.app.data.SolveAnswerRequest
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardData(
    val streak: StreakResponse? = null,
    val overallAvg: OverallAvgResponse? = null,
    val bestScores: List<BestScoreSummary> = emptyList(),
    val dailyCounts: List<com.sqldpass.app.data.DailyCountResponse> = emptyList(),
)

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
)

class AppViewModel(
    private val repository: AppRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState(nickname = tokenStore.nickname))
    val state: StateFlow<AppUiState> = _state

    init {
        // 비로그인 콜드 스타트에서 /api/mock-exams 가 401 응답까지 100~300ms 블록되는 비용
        // 회피. 로그인된 상태일 때만 즉시 refresh, 아니면 onAuthChanged() 경로에 맡김.
        if (!tokenStore.token.isNullOrBlank()) refresh()
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

    fun toggleBookmark(questionId: Long) {
        val isBookmarked = _state.value.runnerBookmarks.contains(questionId)
        // optimistic
        _state.update {
            it.copy(
                runnerBookmarks = if (isBookmarked) it.runnerBookmarks - questionId
                else it.runnerBookmarks + questionId
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
                        message = "즐겨찾기 변경 실패: ${e.message}",
                    )
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
        viewModelScope.launch {
            runCatching { repository.subscription() }
                .onSuccess { sub -> _state.update { it.copy(subscription = sub) } }
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
     * subjectId 는 모든 문제가 동일 과목일 때만 채우고, 섞여 있으면 null → submitPractice 가
     * 0L 로 폴백한다(기존 startWrongAnswerRunner 동작과 동일).
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
                        val subjectId = session.subjectId ?: -1L
                        val response = if (subjectId > 0) {
                            repository.submitPractice(subjectId, answers)
                        } else {
                            // 오답노트 전체 풀이 등 subjectId 없는 케이스 — 백엔드 호환 시 그대로
                            repository.submitPractice(0L, answers)
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
