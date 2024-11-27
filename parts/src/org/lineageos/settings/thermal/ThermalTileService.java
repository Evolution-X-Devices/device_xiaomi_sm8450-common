/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.service.quicksettings.TileService;
import android.service.quicksettings.Tile;
import android.util.Log;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.R;

public class ThermalTileService extends TileService {

    private static final String TAG = "ThermalTileService";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private String[] modes;
    private int currentMode = 0; // Default mode index

    @Override
    public void onStartListening() {
        super.onStartListening();
        modes = new String[]{
            getString(R.string.thermal_mode_default),
            getString(R.string.thermal_mode_performance),
            getString(R.string.thermal_mode_gaming),
            getString(R.string.thermal_mode_battery_saver),
            getString(R.string.thermal_mode_unknown)
        };
        currentMode = getCurrentThermalMode();

        // Reset to Default if mode is Unknown
        if (currentMode == 4) {
            currentMode = 0;
            setThermalMode(currentMode);
        }
        updateTile();
    }

    @Override
    public void onClick() {
        toggleThermalMode();
    }

    private void toggleThermalMode() {
        if (currentMode == 4) {
            // If in Unknown mode, reset to Default
            currentMode = 0;
        } else {
            // Cycle through the order: Default → Performance → Gaming → Battery Saver → Default
            currentMode = (currentMode + 1) % 4;
        }
        setThermalMode(currentMode);
        updateTile();
    }

    private int getCurrentThermalMode() {
        String line = FileUtils.readOneLine(THERMAL_SCONFIG);
        if (line != null) {
            try {
                int value = Integer.parseInt(line.trim());
                switch (value) {
                    case 0: return 0; // Default
                    case 10: return 1; // Performance
                    case 9: return 2; // Gaming
                    case 3: return 3; // Battery Saver
                    default: return 4; // Unknown mode
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing thermal mode value: ", e);
            }
        }
        return 4; // Treat invalid or missing values as Unknown
    }

    private void setThermalMode(int mode) {
        int thermalValue;
        switch (mode) {
            case 0: thermalValue = 0; break;  // Default
            case 1: thermalValue = 10; break;  // Performance
            case 2: thermalValue = 9; break; // Gaming
            case 3: thermalValue = 3; break;  // Battery Saver
            default: thermalValue = 0; break; // Reset to Default for Unknown
        }
        boolean success = FileUtils.writeLine(THERMAL_SCONFIG, String.valueOf(thermalValue));
        Log.d(TAG, "Thermal mode changed to " + modes[mode] + ": " + success);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            // Set tile state based on current mode
            if (currentMode == 1 || currentMode == 2) { // Performance or Gaming
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                tile.setState(Tile.STATE_INACTIVE);
            }

            // Update label and subtitle based on current mode
            tile.setLabel(getString(R.string.thermal_tile_label));
            tile.setSubtitle(modes[currentMode]);
            tile.updateTile();
        } else {
            Log.e(TAG, "QS Tile is unavailable, attempting recovery...");
            // Fallback: Reset tile state in case of inconsistency
            onStartListening();
        }
    }
}
