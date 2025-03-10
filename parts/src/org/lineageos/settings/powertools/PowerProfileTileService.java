/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.powertools;

import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;

public class PowerProfileTileService extends TileService {
    private static final String TAG = "PowerProfileTileService";
    private PowerProfileUtil mManager;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new PowerProfileUtil(this);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mPrefListener = (sharedPreferences, key) -> {
            if (PowerProfileUtil.THERMAL_ENABLED_KEY.equals(key)) {
                if (sharedPreferences.getBoolean(PowerProfileUtil.THERMAL_ENABLED_KEY, false)) {
                    updateTileDisabled();
                } else {
                    updateTile();
                }
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        boolean isMasterEnabled = mManager.isMasterEnabled();
        if (isMasterEnabled) {
            updateTileDisabled();
        } else {
            mManager.getManagedMode();
            updateTile();
        }
    }

    @Override
    public void onClick() {
        if (mManager.isMasterEnabled()) {
            return;
        }
        mManager.toggleMode();
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            int currentMode = mManager.getManagedMode();
            switch (currentMode) {
                case PowerProfileUtil.MODE_GAMING:
                    tile.setState(Tile.STATE_ACTIVE);
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_gaming));
                    break;
                case PowerProfileUtil.MODE_PERFORMANCE:
                    tile.setState(Tile.STATE_ACTIVE);
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_performance));
                    break;
                case PowerProfileUtil.MODE_BALANCE:
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_balance));
                    break;
                case PowerProfileUtil.MODE_BATTERY_SAVER:
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_battery_saver));
                    break;
                default:
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_balance));
                    break;
            }
            tile.setLabel(getString(R.string.powerprofile_tile_label));
            tile.setSubtitle(mManager.getModeLabel());
            tile.updateTile();
        }
    }

    private void updateTileDisabled() {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_balance));
            tile.setLabel(getString(R.string.powerprofile_tile_label));
            tile.setSubtitle(getString(R.string.powerprofile_tile_disabled_subtitle));
            tile.updateTile();
        }
    }

    @Override
    public void onDestroy() {
        if (mSharedPrefs != null && mPrefListener != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        }
        if (mManager != null) {
            mManager.cleanup();
        }
        super.onDestroy();
    }
}
