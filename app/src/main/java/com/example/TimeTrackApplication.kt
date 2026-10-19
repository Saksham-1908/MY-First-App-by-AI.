package com.example

import android.app.Application
import com.example.data.local.TimeTrackDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.TimeTrackRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TimeTrackApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { TimeTrackDatabase.getDatabase(this, applicationScope) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val repository by lazy {
        TimeTrackRepository(
            context = this,
            taskDao = database.taskDao(),
            categoryDao = database.categoryDao(),
            preferencesRepository = preferencesRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
