package com.stabila.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import android.content.SharedPreferences

import kotlinx.coroutines.launch
import com.stabila.core.data.UserPreferencesDataStore
import com.stabila.app.notifications.NotificationScheduler

import androidx.lifecycle.viewModelScope

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPreferencesDataStore,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("stabila_settings", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow(prefs.getBoolean("notifications", false))
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _reminderTime = MutableStateFlow(prefs.getString("reminder_time", "09:00 AM") ?: "09:00 AM")
    val reminderTime: StateFlow<String> = _reminderTime.asStateFlow()

    init {
        // Initialize scheduler based on saved preferences
        if (_notifications.value) {
            notificationScheduler.scheduleDailyReminder(_reminderTime.value)
        }
    }

    fun setNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("notifications", enabled).apply()
        _notifications.value = enabled
        if (enabled) {
            notificationScheduler.scheduleDailyReminder(_reminderTime.value)
        } else {
            notificationScheduler.cancelReminder()
        }
    }



    fun setReminderTime(time: String) {
        prefs.edit().putString("reminder_time", time).apply()
        _reminderTime.value = time
        if (_notifications.value) {
            notificationScheduler.scheduleDailyReminder(time)
        }
    }

    // Auto-Scroll Integration
    val autoScrollSpeed = userPrefs.autoScrollSpeed
    val enabledScrollApps = userPrefs.enabledScrollApps

    fun setAutoScrollSpeed(speed: Float) {
        viewModelScope.launch {
            userPrefs.setAutoScrollSpeed(speed)
        }
    }
    
    fun setEnabledScrollApps(apps: Set<String>) {
        viewModelScope.launch {
            userPrefs.setEnabledScrollApps(apps)
        }
    }

    val themePreference = userPrefs.themePreference

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            userPrefs.setThemePreference(theme)
        }
    }
}
