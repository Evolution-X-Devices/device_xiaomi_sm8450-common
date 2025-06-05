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

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.view.Display;

import android.provider.Settings;
import android.util.Log;
import android.view.OrientationEventListener;
import android.content.res.Configuration;
import androidx.preference.PreferenceManager;

public final class RefreshUtils {

    private static final String REFRESH_CONTROL = "refresh_control";

    private static float defaultMaxRate;
    private static float defaultMinRate;
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private Context mContext;
    protected static boolean isAppInList = false;

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_60 = 1;
    protected static final int STATE_90 = 2;
    protected static final int STATE_120 = 3;
    protected static final int STATE_60_LAND = 4;
    protected static final int STATE_90_LAND = 5;
    protected static final int STATE_120_LAND = 6;

    private static final float REFRESH_STATE_DEFAULT = 120f;
    private static final float REFRESH_STATE_60 = 60f;
    private static final float REFRESH_STATE_90 = 90f;
    private static final float REFRESH_STATE_120 = 120f;
    private static final float REFRESH_STATE_60_LAND = 60f;
    private static final float REFRESH_STATE_90_LAND = 90f;
    private static final float REFRESH_STATE_120_LAND = 120f;

    private static final String REFRESH_60 = "refresh.60=";
    private static final String REFRESH_90 = "refresh.90=";
    private static final String REFRESH_120 = "refresh.120=";
    private static final String REFRESH_60_LAND = "refresh.60land=";
    private static final String REFRESH_90_LAND = "refresh.90land=";
    private static final String REFRESH_120_LAND = "refresh.120land=";

    private SharedPreferences mSharedPrefs;

    private OrientationEventListener orientationListener;
    private boolean isLandscape = false;

    protected RefreshUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class),
                UserHandle.CURRENT);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

    protected void getOldRate(){
        defaultMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        defaultMinRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private float getUserMaxRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private float getUserMinRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }
    
    private void initializeOrientationListener(String packageName) {
        if (orientationListener != null) {
            orientationListener.disable();
        }

        orientationListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                int currentOrientation = mContext.getResources().getConfiguration().orientation;
                boolean newIsLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
                if (newIsLandscape != isLandscape) {
                    isLandscape = newIsLandscape;
                    adjustRefreshRateForOrientation(packageName);
                }
            }
        };

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        } else {
            orientationListener.disable();
        }
    }

    private void adjustRefreshRateForOrientation(String packageName) {
        int state = getStateForPackage(packageName);
        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        boolean isLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);

        if (state == STATE_60_LAND) {
            if (isLandscape) {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_60_LAND);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_60_LAND);
            } else {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, defaultMinRate);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, defaultMaxRate);
            }
        } else if (state == STATE_90_LAND) {
            if (isLandscape) {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_90_LAND);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_90_LAND);
            } else {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, defaultMinRate);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, defaultMaxRate);
            }
        } else if (state == STATE_120_LAND) {
            if (isLandscape) {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_120_LAND);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_120_LAND);
            } else {
                Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, defaultMinRate);
                Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, defaultMaxRate);
            }
        }
    }

    protected void checkOrientationAndSetRate(String packageName) {
        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        boolean isCurrentlyLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
        float currentMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);

        if (isCurrentlyLandscape && isAppInList) {
            setLandscapeModeRefreshRate(packageName);
        } else if (!isCurrentlyLandscape && isAppInList) {
            setPortraitModeRefreshRate(packageName);
        }
    }

    private void disableOrientationListener() {
        if (orientationListener != null) {
            orientationListener.disable();
            orientationListener = null;
        }
    }

    protected void setRefreshRate(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        float maxRate = defaultMaxRate;
        float minRate = defaultMinRate;
        isAppInList = false;

        if (value != null) {
            modes = value.split(":");

            if (modes[0].contains(packageName + ",")) { // 60Hz
                disableOrientationListener();
                maxRate = REFRESH_STATE_60;
                minRate = REFRESH_STATE_60;
                isAppInList = true;
            } else if (modes[1].contains(packageName + ",")) { // 90Hz
                disableOrientationListener();
                maxRate = REFRESH_STATE_90;
                minRate = REFRESH_STATE_90;
                isAppInList = true;
            } else if (modes[2].contains(packageName + ",")) { // 120Hz
                disableOrientationListener();
                maxRate = REFRESH_STATE_120;
                minRate = REFRESH_STATE_120;
                isAppInList = true;
            } else if (modes[3].contains(packageName + ",")) { // 60Hz in landscape
                initializeOrientationListener(packageName);
                isAppInList = true;
                return;
            } else if (modes[4].contains(packageName + ",")) { // 90Hz in landscape
                initializeOrientationListener(packageName);
                isAppInList = true;
                return;
            } else if (modes[5].contains(packageName + ",")) { // 120Hz in landscape
                initializeOrientationListener(packageName);
                isAppInList = true;
                return;
            } else { // default
                disableOrientationListener();
                maxRate = defaultMaxRate;
                minRate = defaultMinRate;
            }
        }
        Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
    }

    private void setLandscapeModeRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_60_LAND) {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_60_LAND);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_60_LAND);
        } else if (state == STATE_90_LAND) {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_90_LAND);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_90_LAND);
        } else if (state == STATE_120_LAND) {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_120_LAND);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_120_LAND);
        }
        // For all other states, do nothing (let setRefreshRate handle it)
    }

    private void setPortraitModeRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_60_LAND || state == STATE_90_LAND || state == STATE_120_LAND) {
            // Portrait: use default (system default)
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, defaultMaxRate);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, defaultMinRate);
        }
        // For all other states, do nothing (let setRefreshRate handle it)
    }

    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = REFRESH_60 + ":" + REFRESH_90 + ":" + REFRESH_120 + ":" + REFRESH_60_LAND + ":" + REFRESH_90_LAND + ":" + REFRESH_120_LAND;
            writeValue(value);
        }

        String[] modes = value.split(":");
        if (modes.length < 6) {
            // Pad missing modes
            String[] newModes = new String[6];
            for (int i = 0; i < 6; i++) {
                if (i < modes.length) newModes[i] = modes[i];
                else if (i == 0) newModes[i] = REFRESH_60;
                else if (i == 1) newModes[i] = REFRESH_90;
                else if (i == 2) newModes[i] = REFRESH_120;
                else if (i == 3) newModes[i] = REFRESH_60_LAND;
                else if (i == 4) newModes[i] = REFRESH_90_LAND;
                else newModes[i] = REFRESH_120_LAND;
            }
            value = String.join(":", newModes);
            writeValue(value);
            modes = newModes;
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_60:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_90:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_120:
                modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_60_LAND:
                modes[3] = modes[3] + packageName + ",";
                break;
            case STATE_90_LAND:
                modes[4] = modes[4] + packageName + ",";
                break;
            case STATE_120_LAND:
                modes[5] = modes[5] + packageName + ",";
                break;
        }

        finalString = String.join(":", modes);
        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_60;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_90;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_120;
        } else if (modes[3].contains(packageName + ",")) {
            state = STATE_60_LAND;
        } else if (modes[4].contains(packageName + ",")) {
            state = STATE_90_LAND;
        } else if (modes[5].contains(packageName + ",")) {
            state = STATE_120_LAND;
        }
        return state;
    }
}
