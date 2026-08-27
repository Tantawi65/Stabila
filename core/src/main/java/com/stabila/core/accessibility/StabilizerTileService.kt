package com.stabila.core.accessibility

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.stabila.core.data.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class StabilizerTileService : TileService() {

    @Inject
    lateinit var userPrefs: UserPreferencesDataStore

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isEnabled = false

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        scope.launch {
            isEnabled = userPrefs.touchStabilizerEnabled.first()
            val tile = qsTile ?: return@launch
            
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Touch Stabilizer"
            tile.subtitle = if (isEnabled) "On" else "Off"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            isEnabled = !isEnabled
            userPrefs.setTouchStabilizerEnabled(isEnabled)
            updateTileState()
        }
    }
}
