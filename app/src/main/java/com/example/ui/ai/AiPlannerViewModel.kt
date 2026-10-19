package com.example.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeneratedScheduleResult
import com.example.data.ai.ScheduleOptimizationResult
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AiPlannerMode {
    GENERATE, OPTIMIZE
}

data class AiPlannerUiState(
    val mode: AiPlannerMode = AiPlannerMode.GENERATE,
    val promptText: String = "",
    val targetDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val generatedResult: GeneratedScheduleResult? = null,
    val editableTasks: List<TaskItem> = emptyList(),
    val optimizationResult: ScheduleOptimizationResult? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isAppliedSuccess: Boolean = false
)

class AiPlannerViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiPlannerUiState())
    val uiState: StateFlow<AiPlannerUiState> = _uiState.asStateFlow()

    val todayTasks: StateFlow<List<TaskItem>> = repository.getTasksForDate(LocalDate.now()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMode(mode: AiPlannerMode) {
        _uiState.value = _uiState.value.copy(mode = mode, statusMessage = null, errorMessage = null)
        if (mode == AiPlannerMode.OPTIMIZE) {
            runOptimization()
        }
    }

    fun onPromptChange(text: String) {
        _uiState.value = _uiState.value.copy(promptText = text, errorMessage = null)
    }

    fun setTargetDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(targetDate = date)
    }

    fun selectPresetPrompt(preset: String) {
        _uiState.value = _uiState.value.copy(promptText = preset)
    }

    fun generateSchedule() {
        val prompt = _uiState.value.promptText.trim()
        if (prompt.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your goals or routine for the day")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isAppliedSuccess = false)
            try {
                val result = repository.generateAiSchedule(
                    prompt = prompt,
                    targetDate = _uiState.value.targetDate
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedResult = result,
                    editableTasks = result.tasks,
                    statusMessage = if (result.isFromRemoteAi) "Schedule created with Gemini AI!" else "Schedule generated with TimeTrack Smart Planner"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate schedule: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun removeGeneratedTask(index: Int) {
        val current = _uiState.value.editableTasks.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(editableTasks = current)
        }
    }

    fun applyScheduleToTimetable(onSuccess: () -> Unit) {
        val tasksToInsert = _uiState.value.editableTasks
        if (tasksToInsert.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.insertTasks(tasksToInsert)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAppliedSuccess = true,
                    statusMessage = "Added ${tasksToInsert.size} tasks to your timetable!"
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save tasks: ${e.localizedMessage}"
                )
            }
        }
    }

    fun runOptimization() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val tasks = todayTasks.value
                val optResult = repository.analyzeSchedule(tasks, LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    optimizationResult = optResult
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to analyze schedule: ${e.localizedMessage}"
                )
            }
        }
    }
}
