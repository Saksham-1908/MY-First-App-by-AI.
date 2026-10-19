package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.TaskItem
import com.example.receiver.TaskReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {
    const val CHANNEL_REMINDERS_ID = "channel_task_reminders"
    const val CHANNEL_TIMER_ID = "channel_active_timer"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when scheduled timetable tasks are starting"
                enableVibration(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Active Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active task timer"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskItem) {
        if (!task.hasReminder) return
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val date = LocalDate.parse(task.date, dateFormatter)
            val time = LocalTime.parse(task.startTime, timeFormatter)
            val taskDateTime = LocalDateTime.of(date, time)

            val triggerMillis = taskDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()

            if (triggerMillis <= now) {
                // If it's already in the past, don't schedule
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("task_id", task.id)
                putExtra("task_title", task.title)
                putExtra("task_time", "${task.startTime} - ${task.endTime}")
                putExtra("task_category", task.category)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("NotificationHelper", "Scheduled reminder for task: ${task.title} at $triggerMillis")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to schedule reminder for task ${task.id}", e)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to cancel reminder for task $taskId", e)
        }
    }
}
