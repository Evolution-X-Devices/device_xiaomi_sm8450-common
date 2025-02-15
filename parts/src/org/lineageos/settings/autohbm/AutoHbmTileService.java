/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.autohbm;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class AutoHbmTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTile(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, enabled).commit();
        AutoHbmFragment.toggleAutoHbmService(this);
        updateTile(enabled);
    }

    private void updateTile(boolean enabled) {
        final Tile tile = getQsTile();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

        // Set the title for the tile using the new string resource
        String title = getString(R.string.tile_auto_hbm);
        tile.setLabel(title);

        // Set the subtitle based on the state
        String subtitle = enabled ? getString(R.string.tile_hbm_on) : getString(R.string.tile_hbm_off);
        tile.setSubtitle(subtitle);

        tile.updateTile();
    }
}
