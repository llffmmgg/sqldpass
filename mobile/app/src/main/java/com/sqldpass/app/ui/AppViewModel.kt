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
)

data class AppUiState(
    val loading: Boolean = false,
    val nickname: String? = null,
    val mockExams: List<MockExamSummary> = emptyList(),
    val syncResult: SyncResult? = null,
    val message: String? = null,
    val certSlugs: List<String> = listOf("SQLD", "ADsP", "ADP"),
    val selectedCertSlug: String = "SQLD",
    val pastExamsByCert: Map<String, List<PastExamSummary>> = emptyMap(),
    val pastExamsLoading: Boolean = false,
    val subjects: List<SubjectResponse> = emptyList(),
    val subjectsLoading: Boolean = false,
    val runner: RunnerSession? = null,
    val runnerSubmitting: Boolean = false,
    val runnerResult: RunnerResult? = null,
    val dashboard: DashboardData? = null,
    val dashboardLoading: Boolean = false,
)

class AppViewModel(
    private val repository: AppRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState(nickname = tokenStore.nickname))
    val state: StateFlow<AppUiState> = _state

    init {
        refresh()
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
        refresh()
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
            _state.update {
                it.copy(
                    dashboard = DashboardData(streak = streak, overallAvg = avg, bestScores = best),
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
                    RunnerMode.PRACTICE -> {
                        val answers = drafts.map {
                            SolveAnswerRequest(it.questionId, it.selectedOption, it.answerText)
                        }
                        val subjectId = session.subjectId ?: error("과목 ID 가 없습니다.")
                        val response = repository.submitPractice(subjectId, answers)
                        RunnerResult.Solve(response, RunnerMode.PRACTICE)
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
