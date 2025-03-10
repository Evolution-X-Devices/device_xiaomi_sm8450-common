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

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

import java.util.Arrays;
import java.util.List;

public class PowerProfileUtil {

    private static final String TAG = "PowerProfileUtil";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";
    public static final String THERMAL_ENABLED_KEY = "thermal_enabled";
    private static final String SYS_PROP = "sys.perf_mode_active";
    private static final int NOTIFICATION_ID_PERFORMANCE = 1001;
    private static final int NOTIFICATION_ID_GAMING = 1002;

    // Modes:
    public static final int MODE_BALANCE = 0;
    public static final int MODE_GAMING = 1;
    public static final int MODE_PERFORMANCE = 2;
    public static final int MODE_BATTERY_SAVER = 3;
    public static final int MODE_UNKNOWN = 4;

    private static final int POWERPROFILE_BALANCE = 0;
    private static final int POWERPROFILE_GAMING = 10;
    private static final int POWERPROFILE_PERFORMANCE = 23;
    private static final int POWERPROFILE_BATTERY_SAVER = 3;

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private NotificationManager mNotificationManager;
    private List<String> mGamePackages;
    private ContentObserver mBatterySaverObserver;
    private int mCurrentMode = MODE_BALANCE;
    private String[] mModes;

    public PowerProfileUtil(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Resources res = mContext.getResources();
        mGamePackages = Arrays.asList(res.getStringArray(R.array.game_packages));
        mModes = new String[]{
                mContext.getString(R.string.powerprofile_mode_balance),
                mContext.getString(R.string.powerprofile_mode_gaming),
                mContext.getString(R.string.powerprofile_mode_performance),
                mContext.getString(R.string.powerprofile_mode_battery_saver),
                mContext.getString(R.string.powerprofile_mode_unknown)
        };

        if (!mSharedPrefs.contains(THERMAL_ENABLED_KEY)) {
            mSharedPrefs.edit().putBoolean(THERMAL_ENABLED_KEY, false).apply();
        }

        setupNotificationChannel();
        registerBatterySaverObserver();
    }

    public int getCurrentMode() {
        String line = FileUtils.readOneLine(THERMAL_SCONFIG);
        if (line == null) {
            Log.e(TAG, "Failed to read thermal mode from " + THERMAL_SCONFIG);
            return MODE_UNKNOWN;
        }
        try {
            int value = Integer.parseInt(line.trim());
            switch (value) {
                case POWERPROFILE_BALANCE:
                    return MODE_BALANCE;
                case POWERPROFILE_GAMING:
                    return MODE_GAMING;
                case POWERPROFILE_PERFORMANCE:
                    return MODE_PERFORMANCE;
                case POWERPROFILE_BATTERY_SAVER:
                    return MODE_BATTERY_SAVER;
                default:
                    return MODE_UNKNOWN;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing thermal mode value: ", e);
            return MODE_UNKNOWN;
        }
    }

    public void setMode(int mode) {
        mCurrentMode = mode;
        int thermalValue;
        switch (mode) {
            case MODE_BALANCE:
                thermalValue = POWERPROFILE_BALANCE;
                setPerformanceModeActive(1);
                break;
            case MODE_GAMING:
                thermalValue = POWERPROFILE_GAMING;
                setPerformanceModeActive(3);
                optimizeGameLaunch();
                break;
            case MODE_PERFORMANCE:
                thermalValue = POWERPROFILE_PERFORMANCE;
                setPerformanceModeActive(2);
                break;
            case MODE_BATTERY_SAVER:
                thermalValue = POWERPROFILE_BATTERY_SAVER;
                setPerformanceModeActive(0);
                break;
            default:
                thermalValue = POWERPROFILE_BALANCE;
                setPerformanceModeActive(1);
                break;
        }

        boolean success = FileUtils.writeLine(THERMAL_SCONFIG, String.valueOf(thermalValue));
        Log.d(TAG, "Thermal mode changed to " + mModes[mode] + ": " + success);

        if (mode == MODE_BATTERY_SAVER) {
            enableBatterySaver(true);
            cancelPerformanceNotification();
            cancelGamingNotification();
        } else {
            enableBatterySaver(false);
            if (mode == MODE_PERFORMANCE) {
                showPerformanceNotification();
                cancelGamingNotification();
            } else if (mode == MODE_GAMING) {
                showGamingNotification();
                cancelPerformanceNotification();
            } else {
                cancelPerformanceNotification();
                cancelGamingNotification();
            }
        }
    }

    public int getManagedMode() {
        mCurrentMode = getCurrentMode();
        if (mCurrentMode == MODE_UNKNOWN) {
            mCurrentMode = MODE_BALANCE;
            setMode(mCurrentMode);
        }
        return mCurrentMode;
    }

    public boolean isMasterEnabled() {
        return mSharedPrefs.getBoolean(THERMAL_ENABLED_KEY, false);
    }

    public String getModeLabel() {
        if (mCurrentMode >= 0 && mCurrentMode < mModes.length) {
            return mModes[mCurrentMode];
        }
        return mModes[MODE_UNKNOWN];
    }

    public void toggleMode() {
        int currentMode = getManagedMode();
        int newMode;
        switch (currentMode) {
            case MODE_BALANCE:
                newMode = MODE_GAMING;
                break;
            case MODE_GAMING:
                newMode = MODE_PERFORMANCE;
                break;
            case MODE_PERFORMANCE:
                newMode = MODE_BATTERY_SAVER;
                break;
            case MODE_BATTERY_SAVER:
            default:
                newMode = MODE_BALANCE;
                break;
        }
        Log.d(TAG, "Toggling mode: " + currentMode + " -> " + newMode);
        setMode(newMode);
    }

    private void optimizeGameLaunch() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return;

        List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
        if (runningApps == null) return;

        for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
            if (mGamePackages.contains(processInfo.processName)) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

                trimMemory(activityManager, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW);

                Log.d(TAG, "Optimizations applied to game package: " + processInfo.processName);
            }
        }

        clearBackgroundProcesses(activityManager);
    }

    private void clearBackgroundProcesses(ActivityManager activityManager) {
        List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
        if (runningApps == null) return;

        for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
            if (mGamePackages.contains(processInfo.processName)) continue;
            if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activityManager.killBackgroundProcesses(processInfo.processName);
            }
        }
    }

    private void trimMemory(ActivityManager activityManager, int level) {
        try {
            activityManager.getClass().getMethod("trimMemory", int.class).invoke(activityManager, level);
        } catch (Exception e) {
            Log.e(TAG, "Failed to trim memory", e);
        }
    }

    private void enableBatterySaver(boolean enable) {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            boolean isBatterySaverEnabled = powerManager.isPowerSaveMode();
            if (enable && !isBatterySaverEnabled) {
                powerManager.setPowerSaveModeEnabled(true);
                Log.d(TAG, "Battery Saver mode enabled.");
            } else if (!enable && isBatterySaverEnabled) {
                powerManager.setPowerSaveModeEnabled(false);
                Log.d(TAG, "Battery Saver mode disabled.");
            }
        }
    }

    private void setupNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                TAG,
                mContext.getString(R.string.perf_mode_title),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setBlockable(true);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void showPerformanceNotification() {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(mContext, TAG)
                .setContentTitle(mContext.getString(R.string.perf_mode_title))
                .setContentText(mContext.getString(R.string.perf_mode_notification))
                .setSmallIcon(R.drawable.ic_thermal_performance)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID_PERFORMANCE, notification);
    }

    private void showGamingNotification() {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(mContext, TAG)
                .setContentTitle(mContext.getString(R.string.gaming_mode_title))
                .setContentText(mContext.getString(R.string.gaming_mode_notification))
                .setSmallIcon(R.drawable.ic_thermal_gaming)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID_GAMING, notification);
    }

    private void cancelPerformanceNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID_PERFORMANCE);
    }

    private void cancelGamingNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID_GAMING);
    }

    private void setPerformanceModeActive(int mode) {
        SystemProperties.set(SYS_PROP, String.valueOf(mode));
        Log.d(TAG, "Performance mode active set to: " + mode);
    }

    private void registerBatterySaverObserver() {
        mBatterySaverObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                boolean isBatterySaverOn = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.LOW_POWER_MODE, 0) == 1;
                if (isBatterySaverOn && (mCurrentMode == MODE_BALANCE || mCurrentMode == MODE_PERFORMANCE || mCurrentMode == MODE_GAMING)) {
                    Log.d(TAG, "Battery saver enabled, switching to battery saver thermal mode.");
                    mCurrentMode = MODE_BATTERY_SAVER;
                    setMode(mCurrentMode);
                }
            }
        };

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.LOW_POWER_MODE),
                false,
                mBatterySaverObserver
        );
    }

    public void cleanup() {
        if (mBatterySaverObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mBatterySaverObserver);
        }
    }
}
