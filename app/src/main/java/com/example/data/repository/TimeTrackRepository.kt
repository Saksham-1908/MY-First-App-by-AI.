package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiScheduleManager
import com.example.data.ai.GeneratedScheduleResult
import com.example.data.ai.ScheduleOptimizationResult
import com.example.data.local.CategoryDao
import com.example.data.local.TaskDao
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TimeTrackRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    val preferencesRepository: UserPreferencesRepository,
    private val aiScheduleManager: AiScheduleManager = AiScheduleManager()
) {
    // Tasks Flow
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()

    fun getTasksForDate(date: LocalDate): Flow<List<TaskItem>> {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return taskDao.getTasksForDate(dateString)
    }

    fun getTasksBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskItem>> {
        val startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return taskDao.getTasksBetweenDates(startStr, endStr)
    }

    fun getTaskById(id: Long): Flow<TaskItem?> = taskDao.getTaskById(id)

    suspend fun getTaskByIdOnce(id: Long): TaskItem? = taskDao.getTaskByIdOnce(id)

    suspend fun insertTask(task: TaskItem): Long {
        val calculatedDuration = if (task.durationMinutes > 0) task.durationMinutes else task.calculateDurationMinutes()
        val newTask = task.copy(durationMinutes = calculatedDuration)
        val id = taskDao.insertTask(newTask)
        val insertedTask = newTask.copy(id = id)
        if (insertedTask.hasReminder) {
            NotificationHelper.scheduleTaskReminder(context, insertedTask)
        }
        return id
    }

    suspend fun insertTasks(tasks: List<TaskItem>): List<Long> {
        val prepared = tasks.map {
            val dur = if (it.durationMinutes > 0) it.durationMinutes else it.calculateDurationMinutes()
            it.copy(durationMinutes = dur)
        }
        val ids = taskDao.insertAll(prepared)
        // Schedule reminders if applicable
        for (i in prepared.indices) {
            if (prepared[i].hasReminder) {
                NotificationHelper.scheduleTaskReminder(context, prepared[i].copy(id = ids[i]))
            }
        }
        return ids
    }

    suspend fun updateTask(task: TaskItem) {
        val calculatedDuration = if (task.durationMinutes > 0) task.durationMinutes else task.calculateDurationMinutes()
        val updated = task.copy(durationMinutes = calculatedDuration)
        taskDao.updateTask(updated)
        if (updated.hasReminder && !updated.isCompleted) {
            NotificationHelper.scheduleTaskReminder(context, updated)
        } else {
            NotificationHelper.cancelTaskReminder(context, updated.id)
        }
    }

    suspend fun toggleTaskCompletion(task: TaskItem): TaskItem {
        val isNowCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = isNowCompleted,
            completedAt = if (isNowCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
        if (isNowCompleted) {
            NotificationHelper.cancelTaskReminder(context, task.id)
        }
        return updated
    }

    suspend fun recordActualTime(taskId: Long, additionalMinutes: Int) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return
        val updated = task.copy(actualMinutesSpent = task.actualMinutesSpent + additionalMinutes)
        taskDao.updateTask(updated)
    }

    suspend fun deleteTask(task: TaskItem) {
        NotificationHelper.cancelTaskReminder(context, task.id)
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        NotificationHelper.cancelTaskReminder(context, id)
        taskDao.deleteTaskById(id)
    }

    suspend fun clearAllTasks() {
        taskDao.clearAllTasks()
    }

    // Categories
    val allCategories: Flow<List<CategoryItem>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryItem): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryItem) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryItem) {
        categoryDao.deleteCategory(category)
    }

    // AI Features
    suspend fun generateAiSchedule(
        prompt: String,
        targetDate: LocalDate = LocalDate.now(),
        startHour: Int = 8
    ): GeneratedScheduleResult {
        return aiScheduleManager.generateSchedule(prompt, targetDate, startHour)
    }

    suspend fun analyzeSchedule(
        tasks: List<TaskItem>,
        targetDate: LocalDate = LocalDate.now()
    ): ScheduleOptimizationResult {
        return aiScheduleManager.analyzeSchedule(tasks, targetDate)
    }
}
