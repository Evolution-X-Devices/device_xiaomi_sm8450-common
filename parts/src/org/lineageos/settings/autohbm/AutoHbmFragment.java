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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.UsageProgressBarPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.TileUtils;

public class AutoHbmFragment extends PreferenceFragmentCompat
        implements OnCheckedChangeListener, SensorEventListener, Preference.OnPreferenceChangeListener {

    private static final String[] AUTO_HBM_PREFERENCES = {
            Constants.KEY_AUTO_HBM_THRESHOLD,
            Constants.KEY_AUTO_HBM_ENABLE_TIME,
            Constants.KEY_AUTO_HBM_DISABLE_TIME,
            Constants.KEY_CURRENT_LUX_LEVEL
    };

    private CustomSeekBarPreference mAutoHbmThresholdPreference;
    private MainSwitchPreference mAutoHbmSwitch;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private UsageProgressBarPreference mCurrentLuxLevelPreference;
    private int mCurrentLux;

    private static boolean mAutoHbmServiceEnabled = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_hbm, rootKey);
        setHasOptionsMenu(true);

        Context context = getContext();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mAutoHbmSwitch = findPreference(Constants.KEY_AUTO_HBM);
        mAutoHbmSwitch.setChecked(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        mAutoHbmSwitch.addOnSwitchChangeListener(this);

        mAutoHbmThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_THRESHOLD);
        mAutoHbmThresholdPreference.setOnPreferenceChangeListener(this);

        mCurrentLuxLevelPreference = findPreference(Constants.KEY_CURRENT_LUX_LEVEL);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        toggleAutoHbmPreferencesVisibility(mAutoHbmSwitch.isChecked());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.auto_hbm_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_tile) {
            TileUtils.requestAddTileService(
                    getContext(),
                    AutoHbmTileService.class,
                    R.string.auto_hbm_title,
                    R.drawable.ic_auto_hbm_tile
            );
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAutoHbmSwitch.isChecked()) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAutoHbmSwitch.isChecked()) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, isChecked).apply();
        toggleAutoHbmService(getContext());
        toggleAutoHbmPreferencesVisibility(isChecked);

        if (isChecked) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoHbmThresholdPreference && mCurrentLuxLevelPreference != null) {
            int threshold = (int) newValue;
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (event.sensor.getType() == Sensor.TYPE_LIGHT && mCurrentLuxLevelPreference != null) {
            float luxValue = event.values[0];
            mCurrentLux = (int) luxValue;
            int threshold = sharedPrefs.getInt(Constants.KEY_AUTO_HBM_THRESHOLD, 20000);
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateCurrentLuxLevelPreference(int currentLux, int threshold) {
        if (mCurrentLuxLevelPreference != null) {
            mCurrentLuxLevelPreference.setUsageSummary(String.valueOf(currentLux));
            mCurrentLuxLevelPreference.setTotalSummary(String.valueOf(threshold));

            if (currentLux >= threshold) {
                mCurrentLuxLevelPreference.setPercent(100, 100);
            } else {
                mCurrentLuxLevelPreference.setPercent(currentLux, threshold);
            }
        }
    }

    private void toggleAutoHbmPreferencesVisibility(boolean show) {
        for (String prefKey : AUTO_HBM_PREFERENCES) {
            Preference pref = findPreference(prefKey);
            if (pref != null) {
                pref.setVisible(show);
            }
        }
    }

    public static void toggleAutoHbmService(Context context) {
        boolean isAutoHbmEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_AUTO_HBM, false);

        if (isAutoHbmEnabled && !mAutoHbmServiceEnabled) {
            startAutoHbmService(context);
        } else if (!isAutoHbmEnabled && mAutoHbmServiceEnabled) {
            stopAutoHbmService(context);
        }
    }

    private static void startAutoHbmService(Context context) {
        context.startServiceAsUser(new Intent(context, AutoHbmService.class),
                UserHandle.CURRENT);
        mAutoHbmServiceEnabled = true;
    }

    private static void stopAutoHbmService(Context context) {
        mAutoHbmServiceEnabled = false;
        context.stopServiceAsUser(new Intent(context, AutoHbmService.class),
                UserHandle.CURRENT);
    }
}
