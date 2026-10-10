package com.antigravity.adaptivehz.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.antigravity.adaptivehz.R
import com.antigravity.adaptivehz.core.RefreshRateController
import com.antigravity.adaptivehz.data.SettingsRepository

@RequiresApi(Build.VERSION_CODES.N)
class AdaptiveTileService : TileService() {

    private lateinit var controller: RefreshRateController
    private lateinit var repository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        controller = RefreshRateController.getInstance(this)
        repository = SettingsRepository(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!controller.hasWriteSecureSettings()) {
            unlockAndRun {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.tile_permission_required),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                val intent = Intent(this, com.antigravity.adaptivehz.ui.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivityAndCollapse(intent)
            }
            return
        }

        // Cycle: Adaptive -> Force 60 -> Force 120 -> Adaptive
        val isAdaptive = repository.isAdaptiveEnabled.value
        val currentHz = controller.currentRefreshRateHz.value

        when {
            isAdaptive -> {
                // Switch to fixed 60Hz
                repository.setAdaptiveEnabled(false)
                controller.dropToLow(60)
            }
            currentHz <= 60 -> {
                // Switch to fixed 120Hz
                repository.setAdaptiveEnabled(false)
                controller.boostToHigh(120)
            }
            else -> {
                // Switch back to Adaptive
                repository.setAdaptiveEnabled(true)
                AdaptiveCoordinator.notifyUserInteraction()
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        val isAdaptive = repository.isAdaptiveEnabled.value
        val currentHz = controller.currentRefreshRateHz.value

        if (isAdaptive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_adaptive, currentHz)
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_fixed, currentHz)
            }
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_hz)
        tile.updateTile()
    }
}
