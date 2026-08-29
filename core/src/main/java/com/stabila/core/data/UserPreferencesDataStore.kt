package com.stabila.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Central DataStore for user-tuneable parameters.
 * All constants have sensible defaults and are overridden by the daily Tremor Score
 * automatically — these manual values are exposed in Settings as user overrides only.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Keyboard tuning
        val KEY_DEBOUNCE_WINDOW_MS = longPreferencesKey("debounce_window_ms")
        val KEY_SIGMA_MULTIPLIER = floatPreferencesKey("sigma_multiplier")
        val KEY_AUTOCORRECT_THRESHOLD = floatPreferencesKey("autocorrect_threshold")

        // Camera tuning
        val KEY_CAMERA_TIMEOUT_MS = longPreferencesKey("camera_timeout_ms")
        val KEY_TROUGH_THRESHOLD_MULTIPLIER = floatPreferencesKey("trough_threshold_multiplier")

        // Medication settings
        val KEY_MEDICATION_NAMES = stringPreferencesKey("medication_names")

        // Auto-Scroll Settings
        val KEY_AUTO_SCROLL_MASTER_ENABLED = booleanPreferencesKey("auto_scroll_master_enabled")
        val KEY_AUTO_SCROLL_SPEED = floatPreferencesKey("auto_scroll_speed")
        val KEY_ENABLED_SCROLL_APPS = stringPreferencesKey("enabled_scroll_apps")
        val KEY_AUTO_SCROLL_BUTTON_X = floatPreferencesKey("auto_scroll_button_x")
        val KEY_AUTO_SCROLL_BUTTON_Y = floatPreferencesKey("auto_scroll_button_y")
        // Theme Setting
        val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")

        // Defaults
        const val DEFAULT_DEBOUNCE_WINDOW_MS = 120L
        const val DEFAULT_SIGMA_MULTIPLIER = 1.0f
        const val DEFAULT_AUTOCORRECT_THRESHOLD = 0.6f
        const val DEFAULT_CAMERA_TIMEOUT_MS = 2500L
        const val DEFAULT_TROUGH_MULTIPLIER = 0.5f
        const val DEFAULT_AUTO_SCROLL_SPEED = 3f // Medium speed
    }

    val debounceWindowMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEBOUNCE_WINDOW_MS] ?: DEFAULT_DEBOUNCE_WINDOW_MS
    }

    val sigmaMultiplier: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIGMA_MULTIPLIER] ?: DEFAULT_SIGMA_MULTIPLIER
    }

    val autocorrectThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOCORRECT_THRESHOLD] ?: DEFAULT_AUTOCORRECT_THRESHOLD
    }

    val cameraTimeoutMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_CAMERA_TIMEOUT_MS] ?: DEFAULT_CAMERA_TIMEOUT_MS
    }

    val troughThresholdMultiplier: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_TROUGH_THRESHOLD_MULTIPLIER] ?: DEFAULT_TROUGH_MULTIPLIER
    }

    val medicationNames: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MEDICATION_NAMES] ?: ""
    }

    suspend fun setDebounceWindowMs(value: Long) {
        context.dataStore.edit { it[KEY_DEBOUNCE_WINDOW_MS] = value }
    }

    suspend fun setSigmaMultiplier(value: Float) {
        context.dataStore.edit { it[KEY_SIGMA_MULTIPLIER] = value }
    }

    suspend fun setAutocorrectThreshold(value: Float) {
        context.dataStore.edit { it[KEY_AUTOCORRECT_THRESHOLD] = value }
    }

    suspend fun setCameraTimeoutMs(value: Long) {
        context.dataStore.edit { it[KEY_CAMERA_TIMEOUT_MS] = value }
    }

    suspend fun setTroughThresholdMultiplier(value: Float) {
        context.dataStore.edit { it[KEY_TROUGH_THRESHOLD_MULTIPLIER] = value }
    }

    suspend fun setMedicationNames(value: String) {
        context.dataStore.edit { it[KEY_MEDICATION_NAMES] = value }
    }

    val autoScrollSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SCROLL_SPEED] ?: DEFAULT_AUTO_SCROLL_SPEED
    }

    val isAutoScrollMasterEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SCROLL_MASTER_ENABLED] ?: true // Default to true so it works out of the box
    }

    val enabledScrollApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val csv = prefs[KEY_ENABLED_SCROLL_APPS]
        if (csv == null) setOf("all")
        else if (csv.isBlank()) emptySet()
        else csv.split(",").toSet()
    }

    val autoScrollButtonX: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SCROLL_BUTTON_X] ?: -1f
    }

    val autoScrollButtonY: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SCROLL_BUTTON_Y] ?: -1f
    }

    suspend fun setAutoScrollSpeed(value: Float) {
        context.dataStore.edit { it[KEY_AUTO_SCROLL_SPEED] = value }
    }

    suspend fun setAutoScrollMasterEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SCROLL_MASTER_ENABLED] = value }
    }

    suspend fun setEnabledScrollApps(apps: Set<String>) {
        context.dataStore.edit { it[KEY_ENABLED_SCROLL_APPS] = apps.joinToString(",") }
    }

    suspend fun setAutoScrollButtonPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SCROLL_BUTTON_X] = x
            prefs[KEY_AUTO_SCROLL_BUTTON_Y] = y
        }
    }

    val themePreference: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_PREFERENCE] ?: "SYSTEM"
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { it[KEY_THEME_PREFERENCE] = theme }
    }

    // Touch Stabilizer Settings
    val touchStabilizerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[androidx.datastore.preferences.core.booleanPreferencesKey("touch_stabilizer_enabled")] ?: false
    }

    suspend fun setTouchStabilizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[androidx.datastore.preferences.core.booleanPreferencesKey("touch_stabilizer_enabled")] = enabled }
    }
    
    val touchTremorRadius: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[androidx.datastore.preferences.core.floatPreferencesKey("touch_tremor_radius")] ?: 50f
    }

    suspend fun setTouchTremorRadius(radius: Float) {
        context.dataStore.edit { it[androidx.datastore.preferences.core.floatPreferencesKey("touch_tremor_radius")] = radius }
    }
}
