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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class HbmTileService extends TileService {
    private static final int MAX_BRIGHTNESS = 4000;
    private static final int FALLBACK_BRIGHTNESS = 200;
    private static final String NOTIFICATION_CHANNEL_ID = "hbm_tile_service_channel";
    private static final int NOTIFICATION_ID = 2;
    private int mLastManualBrightness = FALLBACK_BRIGHTNESS;
    private boolean mIsAutoBrightnessEnabled = false;
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationChannel();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoHbmEnabled = sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false);
        mIsAutoBrightnessEnabled = isSystemAutoBrightnessEnabled();
        boolean isHbmEnabled = isCurrentlyEnabled();
        updateTile(isAutoHbmEnabled, mIsAutoBrightnessEnabled, isHbmEnabled);
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoHbmEnabled = sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false);
        mIsAutoBrightnessEnabled = isSystemAutoBrightnessEnabled();

        if (isAutoHbmEnabled || mIsAutoBrightnessEnabled) {
            // Do nothing if Auto HBM or Auto Brightness is enabled
            return;
        }

        boolean isHbmEnabled = isCurrentlyEnabled();
        if (isHbmEnabled) {
            restoreBrightness();
            cancelHbmNotification();
        } else {
            saveCurrentBrightness();
            setBrightnessDirectly(MAX_BRIGHTNESS);
            showHbmNotification();
        }
        updateTile(isAutoHbmEnabled, mIsAutoBrightnessEnabled, !isHbmEnabled);
    }

    private void updateTile(boolean isAutoHbmEnabled, boolean isAutoBrightnessEnabled, boolean isHbmEnabled) {
        final Tile tile = getQsTile();
        if (isAutoHbmEnabled || isAutoBrightnessEnabled) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel(getString(R.string.tile_auto_hbm_or_brightness_enabled));
            tile.setSubtitle(null);
        } else {
            tile.setState(isHbmEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_hbm));
            tile.setSubtitle(isHbmEnabled ? getString(R.string.tile_hbm_on) : getString(R.string.tile_hbm_off));
        }
        tile.updateTile();
    }

    private void setBrightnessDirectly(int brightness) {
        FileUtils.writeValue(Constants.NODE_BRIGHTNESS, String.valueOf(brightness));
    }

    private void restoreBrightness() {
        if (mIsAutoBrightnessEnabled) {
            return;
        }
        setBrightnessDirectly(mLastManualBrightness > 0 ? mLastManualBrightness : FALLBACK_BRIGHTNESS);
    }

    private boolean isCurrentlyEnabled() {
        String fileValue = FileUtils.getFileValue(Constants.NODE_BRIGHTNESS, "0");
        return Integer.parseInt(fileValue) == MAX_BRIGHTNESS;
    }

    private void saveCurrentBrightness() {
        try {
            String fileValue = FileUtils.getFileValue(Constants.NODE_BRIGHTNESS, String.valueOf(FALLBACK_BRIGHTNESS));
            mLastManualBrightness = Integer.parseInt(fileValue);
        } catch (NumberFormatException e) {
            mLastManualBrightness = FALLBACK_BRIGHTNESS;
        }
    }

    private boolean isSystemAutoBrightnessEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private void setupNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.hbm_mode_title),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setBlockable(true);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void showHbmNotification() {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.hbm_mode_title))
                .setContentText(getString(R.string.hbm_mode_notification))
                .setSmallIcon(R.drawable.ic_auto_hbm_tile)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelHbmNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}
