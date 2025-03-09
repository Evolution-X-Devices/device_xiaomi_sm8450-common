package org.lineageos.settings.thermal;

import android.app.Service;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.graphics.drawable.Icon;
import android.util.Log;

import org.lineageos.settings.R;

public class ThermalTileService extends TileService {
    private static final String TAG = "ThermalTileService";
    private static final String THERMAL_ENABLED_KEY = "thermal_enabled";
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Listen for changes to update the tile immediately.
        mPrefListener = (sharedPreferences, key) -> {
            if (THERMAL_ENABLED_KEY.equals(key)) {
                updateTile();
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onDestroy() {
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        super.onDestroy();
    }

    @Override
    public void onClick() {
        // Toggle the master switch via ThermalUtils.
        boolean enabled = mSharedPrefs.getBoolean(THERMAL_ENABLED_KEY, false);
        boolean newState = !enabled;
        ThermalUtils.getInstance(this).setEnabled(newState);
        Log.d(TAG, "Thermal master switch toggled to: " + newState);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            boolean enabled = mSharedPrefs.getBoolean(THERMAL_ENABLED_KEY, false);
            if (enabled) {
                tile.setState(Tile.STATE_ACTIVE);
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_enabled));
                tile.setLabel(getString(R.string.thermal_tile_label));
                tile.setSubtitle(getString(R.string.thermal_tile_enabled_subtitle));
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal_disabled));
                tile.setLabel(getString(R.string.thermal_tile_label));
                tile.setSubtitle(getString(R.string.thermal_tile_disabled_subtitle));
            }
            tile.updateTile();
        }
    }
}
