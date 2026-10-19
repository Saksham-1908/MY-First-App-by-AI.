package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, startTime ASC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY startTime ASC")
    fun getTasksForDate(date: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskItem?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdOnce(id: Long): TaskItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskItem>): List<Long>

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
