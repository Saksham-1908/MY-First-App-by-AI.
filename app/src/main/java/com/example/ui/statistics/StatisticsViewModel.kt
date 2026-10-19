package com.example.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class StatsTimeRange {
    TODAY, WEEK, MONTH
}

data class CategoryStat(
    val category: String,
    val colorHex: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val percentage: Float
)

data class DayComparisonStat(
    val dayLabel: String, // "Mon", "Tue"
    val dateStr: String,
    val plannedMinutes: Int,
    val actualMinutes: Int
)

data class StatisticsUiState(
    val timeRange: StatsTimeRange = StatsTimeRange.WEEK,
    val totalPlannedHours: Float = 0f,
    val totalActualHours: Float = 0f,
    val completionRate: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val productivityScore: Int = 0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val dayComparisons: List<DayComparisonStat> = emptyList(),
    val timeAccuracyPercentage: Int = 0
)

class StatisticsViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow(StatsTimeRange.WEEK)
    val timeRange: StateFlow<StatsTimeRange> = _timeRange.asStateFlow()

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.allTasks,
        _timeRange
    ) { allTasks, range ->
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val relevantTasks = when (range) {
            StatsTimeRange.TODAY -> {
                val todayStr = today.format(dateFormatter)
                allTasks.filter { it.date == todayStr }
            }
            StatsTimeRange.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val mondayStr = monday.format(dateFormatter)
                val sundayStr = sunday.format(dateFormatter)
                allTasks.filter { it.date in mondayStr..sundayStr }
            }
            StatsTimeRange.MONTH -> {
                val firstDay = today.withDayOfMonth(1).format(dateFormatter)
                val lastDay = today.withDayOfMonth(today.lengthOfMonth()).format(dateFormatter)
                allTasks.filter { it.date in firstDay..lastDay }
            }
        }

        val totalPlannedMins = relevantTasks.sumOf { it.calculateDurationMinutes() }
        val totalActualMins = relevantTasks.sumOf { it.actualMinutesSpent }
        val completedCount = relevantTasks.count { it.isCompleted }
        val totalCount = relevantTasks.size
        val completionRate = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        // Accuracy: actual / planned ratio
        val timeAccuracy = if (totalPlannedMins > 0) {
            ((totalActualMins.toFloat() / totalPlannedMins.toFloat()) * 100).toInt().coerceIn(0, 200)
        } else 0

        // Productivity score: combination of completion and tracked time
        val score = if (totalCount > 0) {
            val taskWeight = completionRate * 0.6f
            val trackingWeight = (if (totalActualMins > 0) 40f else 20f)
            (taskWeight + trackingWeight).toInt().coerceIn(0, 100)
        } else 0

        // Category breakdown
        val categoryGroups = relevantTasks.groupBy { it.category }
        val totalCatMins = totalActualMins.takeIf { it > 0 } ?: totalPlannedMins.takeIf { it > 0 } ?: 1
        val categoryStats = categoryGroups.map { (catName, taskList) ->
            val planned = taskList.sumOf { it.calculateDurationMinutes() }
            val actual = taskList.sumOf { it.actualMinutesSpent }
            val baseMins = if (totalActualMins > 0) actual else planned
            val pct = (baseMins.toFloat() / totalCatMins.toFloat()) * 100f
            val colorHex = taskList.firstOrNull()?.categoryColorHex ?: "#6366F1"
            CategoryStat(
                category = catName,
                colorHex = colorHex,
                plannedMinutes = planned,
                actualMinutes = actual,
                percentage = pct
            )
        }.sortedByDescending { it.percentage }

        // Day comparison for the week
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dayComparisons = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val dateStr = date.format(dateFormatter)
            val dayTasks = allTasks.filter { it.date == dateStr }
            DayComparisonStat(
                dayLabel = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                dateStr = dateStr,
                plannedMinutes = dayTasks.sumOf { it.calculateDurationMinutes() },
                actualMinutes = dayTasks.sumOf { it.actualMinutesSpent }
            )
        }

        StatisticsUiState(
            timeRange = range,
            totalPlannedHours = totalPlannedMins / 60f,
            totalActualHours = totalActualMins / 60f,
            completionRate = completionRate,
            completedTasksCount = completedCount,
            totalTasksCount = totalCount,
            productivityScore = score,
            categoryStats = categoryStats,
            dayComparisons = dayComparisons,
            timeAccuracyPercentage = timeAccuracy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    fun setTimeRange(range: StatsTimeRange) {
        _timeRange.value = range
    }
}
