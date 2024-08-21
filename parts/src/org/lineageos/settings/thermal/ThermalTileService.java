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
import org.lineageos.settings.utils.TileUtils;
import org.lineageos.settings.R;

public class ThermalTileService extends TileService {

    private static final String TAG = "ThermalTileService";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private String[] modes;
    private int currentMode = 0; // Default to the first mode

    @Override
    public void onStartListening() {
        super.onStartListening();
        modes = new String[]{
            getString(R.string.thermal_mode_default),
            getString(R.string.thermal_mode_performance),
            getString(R.string.thermal_mode_battery_saver),
            getString(R.string.thermal_mode_gaming)
        };
        currentMode = getCurrentThermalMode();
        
        // If sconfig value is -1 or invalid, set it to default mode
        if (currentMode == -1) {
            currentMode = 0; // Default mode
            setThermalMode(0); // Write default mode value to sconfig
        }

        updateTile(); // Ensure the tile displays the correct mode when added to quick settings
    }

    @Override
    public void onClick() {
        toggleThermalMode();
    }

    private void toggleThermalMode() {
        currentMode = (currentMode + 1) % modes.length; // Cycle through modes
        setThermalMode(currentMode);
        updateTile();
    }

    private int getCurrentThermalMode() {
        String line = FileUtils.readOneLine(THERMAL_SCONFIG);
        if (line != null) {
            try {
                int value = Integer.parseInt(line.trim());
                switch (value) {
                    case 20: return 0; // Default
                    case 10: return 1; // Performance
                    case 3: return 2;  // Battery Saver
                    case 9: return 3;  // Gaming
                    default: return 0; // Default if unknown value
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing thermal mode value: ", e);
            }
        }
        return -1; // Indicate invalid or unknown mode
    }

    private void setThermalMode(int mode) {
        int thermalValue;
        switch (mode) {
            case 0: thermalValue = 20; break; // Default
            case 1: thermalValue = 10; break; // Performance
            case 2: thermalValue = 3; break;  // Battery Saver
            case 3: thermalValue = 9; break;  // Gaming
            default: thermalValue = 20; break; // Default
        }

        // Write the new thermal value to the sconfig file
        boolean success = FileUtils.writeLine(THERMAL_SCONFIG, String.valueOf(thermalValue));
        Log.d(TAG, "Thermal mode changed to " + modes[mode] + ": " + success);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setLabel("Thermal Profile"); // Set the main label
            tile.setSubtitle(modes[currentMode]); // Set the current mode as the subtitle
            tile.updateTile();
        }
    }
}
