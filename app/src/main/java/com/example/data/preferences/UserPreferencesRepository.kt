package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_DURATION_MINUTES = intPreferencesKey("default_duration_minutes")
        val WORK_START_HOUR = intPreferencesKey("work_start_hour")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "dark", "light"
        val ACTIVE_TIMER_TASK_ID = longPreferencesKey("active_timer_task_id")
        val ACTIVE_TIMER_SECONDS = intPreferencesKey("active_timer_seconds")
        val ACTIVE_TIMER_IS_RUNNING = booleanPreferencesKey("active_timer_is_running")
        val ACTIVE_TIMER_LAST_TIMESTAMP = longPreferencesKey("active_timer_last_timestamp")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val defaultDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_DURATION_MINUTES] ?: 45
    }

    val workStartHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WORK_START_HOUR] ?: 8
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val activeTimerTaskId: Flow<Long?> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.ACTIVE_TIMER_TASK_ID] ?: -1L
        if (id > 0) id else null
    }

    val activeTimerSeconds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_TIMER_SECONDS] ?: 0
    }

    val activeTimerIsRunning: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_TIMER_IS_RUNNING] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDefaultDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_DURATION_MINUTES] = minutes
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun saveActiveTimerState(taskId: Long?, seconds: Int, isRunning: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_TIMER_TASK_ID] = taskId ?: -1L
            preferences[PreferencesKeys.ACTIVE_TIMER_SECONDS] = seconds
            preferences[PreferencesKeys.ACTIVE_TIMER_IS_RUNNING] = isRunning
            preferences[PreferencesKeys.ACTIVE_TIMER_LAST_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearActiveTimer() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_TIMER_TASK_ID] = -1L
            preferences[PreferencesKeys.ACTIVE_TIMER_SECONDS] = 0
            preferences[PreferencesKeys.ACTIVE_TIMER_IS_RUNNING] = false
            preferences[PreferencesKeys.ACTIVE_TIMER_LAST_TIMESTAMP] = 0L
        }
    }
}
