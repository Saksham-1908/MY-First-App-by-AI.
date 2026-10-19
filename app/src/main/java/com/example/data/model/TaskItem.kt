package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: String, // Format: "yyyy-MM-dd"
    val startTime: String, // Format: "HH:mm" (24-hour)
    val endTime: String, // Format: "HH:mm" (24-hour)
    val durationMinutes: Int = 0, // Computed or stored
    val actualMinutesSpent: Int = 0, // Time tracked live by timer in minutes
    val category: String = "Work",
    val categoryColorHex: String = "#6366F1",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val hasReminder: Boolean = false,
    val repeatType: String = "None", // "None", "Daily", "Weekdays", "Weekly"
    val notes: String = "",
    val orderIndex: Int = 0
) {
    /**
     * Calculates the duration in minutes based on start and end time strings.
     */
    fun calculateDurationMinutes(): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(startTime, formatter)
            val end = LocalTime.parse(endTime, formatter)
            val minutes = Duration.between(start, end).toMinutes().toInt()
            if (minutes < 0) minutes + (24 * 60) else minutes
        } catch (e: Exception) {
            durationMinutes.coerceAtLeast(15)
        }
    }

    /**
     * Human-readable formatted duration, e.g. "1h 30m" or "45m"
     */
    fun formattedDuration(): String {
        val total = if (durationMinutes > 0) durationMinutes else calculateDurationMinutes()
        val hours = total / 60
        val mins = total % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Human-readable formatted actual duration spent
     */
    fun formattedActualDuration(): String {
        val hours = actualMinutesSpent / 60
        val mins = actualMinutesSpent % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "0m"
        }
    }
}
