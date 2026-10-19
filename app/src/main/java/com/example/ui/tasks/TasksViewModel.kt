package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class TaskFilterOption {
    ALL, TODAY, UPCOMING, COMPLETED
}

enum class TaskSortOption {
    TIME, PRIORITY, TITLE
}

data class TasksUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val selectedFilter: TaskFilterOption = TaskFilterOption.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val sortOption: TaskSortOption = TaskSortOption.TIME,
    val totalCount: Int = 0,
    val completedCount: Int = 0
)

class TasksViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilterOption.ALL)
    val selectedFilter: StateFlow<TaskFilterOption> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(TaskSortOption.TIME)
    val sortOption: StateFlow<TaskSortOption> = _sortOption.asStateFlow()

    val uiState: StateFlow<TasksUiState> = combine(
        repository.allTasks,
        repository.allCategories,
        _selectedFilter,
        _selectedCategory,
        _searchQuery
    ) { tasks, categories, filter, category, query ->
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        var filtered = tasks.filter { task ->
            // Filter option
            val matchesFilter = when (filter) {
                TaskFilterOption.ALL -> true
                TaskFilterOption.TODAY -> task.date == todayStr
                TaskFilterOption.UPCOMING -> task.date >= todayStr && !task.isCompleted
                TaskFilterOption.COMPLETED -> task.isCompleted
            }

            // Category filter
            val matchesCategory = category == null || task.category.equals(category, ignoreCase = true)

            // Search query
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true)

            matchesFilter && matchesCategory && matchesQuery
        }

        // Apply sorting
        filtered = when (_sortOption.value) {
            TaskSortOption.TIME -> filtered.sortedWith(compareBy({ it.date }, { it.startTime }))
            TaskSortOption.PRIORITY -> filtered.sortedByDescending {
                when (it.priority) {
                    "High" -> 3
                    "Medium" -> 2
                    else -> 1
                }
            }
            TaskSortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
        }

        TasksUiState(
            tasks = tasks,
            filteredTasks = filtered,
            categories = categories,
            selectedFilter = filter,
            selectedCategory = category,
            searchQuery = query,
            sortOption = _sortOption.value,
            totalCount = tasks.size,
            completedCount = tasks.count { it.isCompleted }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun setFilter(filter: TaskFilterOption) {
        _selectedFilter.value = filter
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: TaskSortOption) {
        _sortOption.value = sort
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

    fun duplicateTask(task: TaskItem) {
        viewModelScope.launch {
            val duplicated = task.copy(
                id = 0,
                title = "${task.title} (Copy)",
                isCompleted = false,
                completedAt = null,
                actualMinutesSpent = 0
            )
            repository.insertTask(duplicated)
        }
    }
}
