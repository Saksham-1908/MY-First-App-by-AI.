package com.example.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class ScheduleViewMode {
    DAY, WEEK
}

data class DayTimelineSlot(
    val hour: Int, // 6 to 23
    val label: String, // "08:00"
    val tasks: List<TaskItem> = emptyList()
)

data class WeekDayData(
    val date: LocalDate,
    val dayName: String, // "Mon", "Tue"
    val dayNumber: String, // "19"
    val isToday: Boolean,
    val isSelected: Boolean,
    val tasks: List<TaskItem>
)

data class ScheduleUiState(
    val viewMode: ScheduleViewMode = ScheduleViewMode.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val dayTasks: List<TaskItem> = emptyList(),
    val weekDays: List<WeekDayData> = emptyList(),
    val timelineSlots: List<DayTimelineSlot> = emptyList(),
    val formattedDateHeader: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ScheduleViewMode.DAY)
    val viewMode: StateFlow<ScheduleViewMode> = _viewMode.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Day tasks flow
    private val dayTasksFlow = _selectedDate.flatMapLatest { date ->
        repository.getTasksForDate(date)
    }

    // Week tasks flow (Monday to Sunday)
    private val weekTasksFlow = _selectedDate.flatMapLatest { date ->
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        repository.getTasksBetweenDates(monday, sunday)
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        _viewMode,
        _selectedDate,
        dayTasksFlow,
        weekTasksFlow
    ) { mode, date, dayTasks, weekTasks ->
        // Format header
        val header = when (mode) {
            ScheduleViewMode.DAY -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            ScheduleViewMode.WEEK -> {
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                "${monday.format(DateTimeFormatter.ofPattern("d MMM"))} - ${sunday.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
            }
        }

        // Timeline slots for 24 hours (from 06:00 to 23:00)
        val slots = (6..23).map { hour ->
            val hourStr = String.format("%02d", hour)
            val slotTasks = dayTasks.filter {
                it.startTime.startsWith(hourStr)
            }
            DayTimelineSlot(
                hour = hour,
                label = String.format("%02d:00", hour),
                tasks = slotTasks
            )
        }

        // Weekdays mapping
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val today = LocalDate.now()
        val weekList = (0..6).map { dayOffset ->
            val dayDate = monday.plusDays(dayOffset.toLong())
            val dateStr = dayDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val matchingTasks = weekTasks.filter { it.date == dateStr }
            WeekDayData(
                date = dayDate,
                dayName = dayDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                dayNumber = dayDate.dayOfMonth.toString(),
                isToday = dayDate.isEqual(today),
                isSelected = dayDate.isEqual(date),
                tasks = matchingTasks
            )
        }

        ScheduleUiState(
            viewMode = mode,
            selectedDate = date,
            dayTasks = dayTasks.sortedBy { it.startTime },
            weekDays = weekList,
            timelineSlots = slots,
            formattedDateHeader = header
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState()
    )

    fun setViewMode(mode: ScheduleViewMode) {
        _viewMode.value = mode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToPrevious() {
        if (_viewMode.value == ScheduleViewMode.DAY) {
            _selectedDate.value = _selectedDate.value.minusDays(1)
        } else {
            _selectedDate.value = _selectedDate.value.minusWeeks(1)
        }
    }

    fun goToNext() {
        if (_viewMode.value == ScheduleViewMode.DAY) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        } else {
            _selectedDate.value = _selectedDate.value.plusWeeks(1)
        }
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun duplicateTaskToTomorrow(task: TaskItem) {
        viewModelScope.launch {
            val currentDate = LocalDate.parse(task.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val tomorrow = currentDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val duplicated = task.copy(
                id = 0,
                date = tomorrow,
                isCompleted = false,
                completedAt = null,
                actualMinutesSpent = 0
            )
            repository.insertTask(duplicated)
        }
    }

    fun shiftTaskTime(task: TaskItem, deltaHours: Int) {
        viewModelScope.launch {
            try {
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val start = LocalTime.parse(task.startTime, timeFormatter).plusHours(deltaHours.toLong())
                val end = LocalTime.parse(task.endTime, timeFormatter).plusHours(deltaHours.toLong())
                val updated = task.copy(
                    startTime = start.format(timeFormatter),
                    endTime = end.format(timeFormatter)
                )
                repository.updateTask(updated)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }
}
