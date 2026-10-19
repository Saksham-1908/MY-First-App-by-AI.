package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeUiState(
    val greeting: String = "",
    val formattedDate: String = "",
    val tasks: List<TaskItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercentage: Int = 0,
    val plannedMinutesTotal: Int = 0,
    val actualMinutesTotal: Int = 0,
    val upcomingTasks: List<TaskItem> = emptyList(),
    val activeTask: TaskItem? = null,
    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isDayEmpty: Boolean = true
)

class HomeViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _activeTaskId = MutableStateFlow<Long?>(null)
    private val _timerSeconds = MutableStateFlow(0)
    private val _isTimerRunning = MutableStateFlow(false)

    private var timerJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        _activeTaskId,
        _timerSeconds,
        _isTimerRunning
    ) { date, activeId, seconds, running ->
        val hour = LocalTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Good night"
        }
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
        val formattedDate = date.format(dateFormatter)

        Triple(greeting, formattedDate, Pair(activeId, Pair(seconds, running)))
    }.combine(repository.getTasksForDate(LocalDate.now())) { (greeting, formattedDate, timerInfo), tasks ->
        val (activeId, secRun) = timerInfo
        val (seconds, running) = secRun

        val totalCount = tasks.size
        val completedCount = tasks.count { it.isCompleted }
        val completionPercentage = if (totalCount > 0) ((completedCount * 100) / totalCount) else 0

        val plannedMinutes = tasks.sumOf { it.calculateDurationMinutes() }
        val actualMinutes = tasks.sumOf { it.actualMinutesSpent }

        // Find active task or first unfinished task
        val activeTask = if (activeId != null) {
            tasks.find { it.id == activeId }
        } else null

        // Upcoming 3 tasks: not yet completed, ordered by start time
        val upcomingTasks = tasks
            .filter { !it.isCompleted && (activeTask == null || it.id != activeTask.id) }
            .sortedBy { it.startTime }
            .take(3)

        HomeUiState(
            greeting = greeting,
            formattedDate = formattedDate,
            tasks = tasks,
            completedCount = completedCount,
            totalCount = totalCount,
            completionPercentage = completionPercentage,
            plannedMinutesTotal = plannedMinutes,
            actualMinutesTotal = actualMinutes,
            upcomingTasks = upcomingTasks,
            activeTask = activeTask,
            timerSeconds = seconds,
            isTimerRunning = running,
            isDayEmpty = tasks.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            if (task.id == _activeTaskId.value && !task.isCompleted) {
                // If we completed the active task, stop timer and log remaining time
                stopAndSaveTimer()
            }
        }
    }

    fun startTimerForTask(task: TaskItem) {
        if (_activeTaskId.value == task.id) {
            // Already active, toggle running state
            if (_isTimerRunning.value) {
                pauseTimer()
            } else {
                resumeTimer()
            }
            return
        }

        // Switching to a different task
        if (_isTimerRunning.value && _activeTaskId.value != null) {
            stopAndSaveTimer()
        }

        _activeTaskId.value = task.id
        _timerSeconds.value = 0
        _isTimerRunning.value = true
        startTicker()
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_activeTaskId.value != null) {
            _isTimerRunning.value = true
            startTicker()
        }
    }

    fun stopAndSaveTimer() {
        val taskId = _activeTaskId.value ?: return
        val elapsedSecs = _timerSeconds.value
        val minutesToAdd = (elapsedSecs + 30) / 60 // Round to nearest minute
        timerJob?.cancel()
        _isTimerRunning.value = false
        _activeTaskId.value = null
        _timerSeconds.value = 0

        if (minutesToAdd > 0) {
            viewModelScope.launch {
                repository.recordActualTime(taskId, minutesToAdd)
            }
        }
    }

    fun completeActiveTask() {
        val task = uiState.value.activeTask ?: return
        stopAndSaveTimer()
        viewModelScope.launch {
            if (!task.isCompleted) {
                repository.toggleTaskCompletion(task)
            }
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _isTimerRunning.value) {
                delay(1000L)
                _timerSeconds.value += 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
