package com.stabila.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stabila.core.data.db.TremorReadingDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val PREFS_NAME = "stabila_ati"
private const val KEY_LAST_SCORE = "last_tremor_score"

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TremorReadingDao
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Latest tremor score as a Float (0–100), persisted across restarts.
     * Defaults to the last persisted score so the ATI is immediately correct
     * when the user opens the app in the morning.
     */
    val latestScore = dao.getAllReadings()
        .map { readings -> readings.firstOrNull()?.score ?: -1f }
        .onEach { score ->
            // Persist so ATI is correctly calibrated on next app launch
            prefs.edit().putFloat(KEY_LAST_SCORE, score).apply()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = prefs.getFloat(KEY_LAST_SCORE, -1f)
        )
}

