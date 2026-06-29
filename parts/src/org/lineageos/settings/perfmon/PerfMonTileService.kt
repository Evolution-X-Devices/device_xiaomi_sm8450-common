/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lineageos.settings.R

class PerfMonTileService : TileService() {

    override fun onClick() {
        val utils = PerfMonUtils.getInstance(this)
        val isEnabled = utils.isEnabled
        
        val newState = !isEnabled
        utils.isEnabled = newState
        
        if (newState) {
            utils.showFps = true
        }
        
        updateTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val utils = PerfMonUtils.getInstance(this)
        val tile = qsTile ?: return
        val isEnabled = utils.isEnabled

        tile.label = getString(R.string.perfmon_title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tile.subtitle = if (isEnabled) "On" else "Off"
        }

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        tile.updateTile()
    }
}
