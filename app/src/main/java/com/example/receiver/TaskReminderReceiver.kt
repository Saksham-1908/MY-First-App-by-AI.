package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.NotificationHelper

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", 0L)
        val title = intent.getStringExtra("task_title") ?: "Scheduled Task"
        val time = intent.getStringExtra("task_time") ?: ""
        val category = intent.getStringExtra("task_category") ?: "TimeTrack"

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Upcoming: $title")
            .setContentText("$time • $category")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to start '$title' scheduled for $time ($category). Tap to view and start timer.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId.toInt(), notification)
    }
}
