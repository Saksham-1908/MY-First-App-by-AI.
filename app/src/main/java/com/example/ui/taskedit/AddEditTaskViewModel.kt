package com.example.ui.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AddEditTaskUiState(
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val category: String = "Work",
    val categoryColorHex: String = "#6366F1",
    val priority: String = "Medium",
    val hasReminder: Boolean = true,
    val repeatType: String = "None",
    val notes: String = "",
    val categories: List<CategoryItem> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddEditTaskViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryItem>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun initializeWithTask(taskId: Long?, initialDate: LocalDate? = null, initialStartHour: Int? = null) {
        if (taskId != null && taskId > 0) {
            viewModelScope.launch {
                val task = repository.getTaskByIdOnce(taskId)
                if (task != null) {
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    _uiState.value = AddEditTaskUiState(
                        taskId = task.id,
                        title = task.title,
                        description = task.description,
                        date = try { LocalDate.parse(task.date, dateFormatter) } catch (e: Exception) { LocalDate.now() },
                        startTime = try { LocalTime.parse(task.startTime, timeFormatter) } catch (e: Exception) { LocalTime.of(9, 0) },
                        endTime = try { LocalTime.parse(task.endTime, timeFormatter) } catch (e: Exception) { LocalTime.of(10, 0) },
                        category = task.category,
                        categoryColorHex = task.categoryColorHex,
                        priority = task.priority,
                        hasReminder = task.hasReminder,
                        repeatType = task.repeatType,
                        notes = task.notes
                    )
                }
            }
        } else {
            val date = initialDate ?: LocalDate.now()
            val startH = initialStartHour ?: 9
            val start = LocalTime.of(startH.coerceIn(0, 23), 0)
            val end = start.plusHours(1)
            _uiState.value = AddEditTaskUiState(
                date = date,
                startTime = start,
                endTime = end
            )
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onStartTimeChange(time: LocalTime) {
        val currentEnd = _uiState.value.endTime
        val adjustedEnd = if (time.isAfter(currentEnd) || time == currentEnd) {
            time.plusHours(1)
        } else currentEnd
        _uiState.value = _uiState.value.copy(startTime = time, endTime = adjustedEnd)
    }

    fun onEndTimeChange(time: LocalTime) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun onCategorySelect(category: CategoryItem) {
        _uiState.value = _uiState.value.copy(
            category = category.name,
            categoryColorHex = category.colorHex
        )
    }

    fun onPriorityChange(priority: String) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun onReminderToggle(hasReminder: Boolean) {
        _uiState.value = _uiState.value.copy(hasReminder = hasReminder)
    }

    fun onRepeatChange(repeat: String) {
        _uiState.value = _uiState.value.copy(repeatType = repeat)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun addCustomCategory(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val category = CategoryItem(name = name.trim(), colorHex = colorHex)
            repository.insertCategory(category)
            onCategorySelect(category)
        }
    }

    fun saveTask(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a task title")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val durationMinutes = Duration.between(state.startTime, state.endTime).toMinutes().toInt().let {
                if (it <= 0) it + (24 * 60) else it
            }

            val task = TaskItem(
                id = state.taskId ?: 0L,
                title = state.title.trim(),
                description = state.description.trim(),
                date = state.date.format(dateFormatter),
                startTime = state.startTime.format(timeFormatter),
                endTime = state.endTime.format(timeFormatter),
                durationMinutes = durationMinutes,
                category = state.category,
                categoryColorHex = state.categoryColorHex,
                priority = state.priority,
                hasReminder = state.hasReminder,
                repeatType = state.repeatType,
                notes = state.notes.trim()
            )

            if (state.taskId != null && state.taskId > 0) {
                repository.updateTask(task)
            } else {
                repository.insertTask(task)
            }

            _uiState.value = state.copy(isSaving = false, isSaved = true)
            onComplete()
        }
    }
}
