package com.healthcraft.adaptivehz.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.healthcraft.adaptivehz.R
import com.healthcraft.adaptivehz.core.RefreshRateController
import com.healthcraft.adaptivehz.data.OperationMode
import com.healthcraft.adaptivehz.data.SettingsRepository

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
                val intent = Intent(this, com.healthcraft.adaptivehz.ui.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            }
            return
        }

        // Cycle: Adaptive -> Force 60 -> Force 120 -> Adaptive
        val currentMode = repository.operationMode.value

        when (currentMode) {
            OperationMode.ADAPTIVE,
            OperationMode.ECO -> {
                // Switch to fixed 60Hz
                repository.setOperationMode(OperationMode.FIXED_60)
                controller.setFixed60()
            }
            OperationMode.FIXED_60 -> {
                // Switch to fixed 120Hz
                repository.setOperationMode(OperationMode.FIXED_120)
                controller.setFixed120()
            }
            OperationMode.FIXED_120 -> {
                // Switch back to Adaptive
                repository.setOperationMode(OperationMode.ADAPTIVE)
                AdaptiveCoordinator.notifyUserInteraction()
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        val currentMode = repository.operationMode.value
        val currentHz = controller.currentRefreshRateHz.value

        when (currentMode) {
            OperationMode.ADAPTIVE,
            OperationMode.ECO -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.tile_subtitle_adaptive, currentHz)
                }
            }
            OperationMode.FIXED_120 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.tile_subtitle_fixed, currentHz)
                }
            }
            OperationMode.FIXED_60 -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.tile_subtitle_fixed, currentHz)
                }
            }
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_hz)
        tile.updateTile()
    }
}
