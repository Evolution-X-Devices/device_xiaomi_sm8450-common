/*
 * Copyright (C) 2024 The LineageOS Project
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
package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.autohbm.AutoHbmActivity;
import org.lineageos.settings.autohbm.AutoHbmFragment;
import org.lineageos.settings.autohbm.AutoHbmTileService;
import org.lineageos.settings.saturation.SaturationFragment;
import org.lineageos.settings.utils.ComponentUtils;
import org.lineageos.settings.utils.FileUtils;

public class Startup extends BroadcastReceiver {

    private static final String TAG = "Startup";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive called with action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_REBOOT.equals(action)) {

            // Adding a delay before applying the settings
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "Applying saved settings...");
                applySavedSaturation(context);
                applyAutoHbmSettings(context);
            }, 5000); // Delay of 5 seconds
        }
    }

    private void applySavedSaturation(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int seekBarValue = sharedPrefs.getInt(Constants.KEY_SATURATION, 100);
        Log.d(TAG, "Retrieved seekBarValue: " + seekBarValue);

        // Apply the saved saturation value
        applySaturation(seekBarValue);
    }

    private void applySaturation(int seekBarValue) {
        Log.d(TAG, "Applying saturation: " + seekBarValue);

        float saturation;
        if (seekBarValue == 100) {
            saturation = 1.001f;
        } else {
            saturation = seekBarValue / 100.0f;
        }

        IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        if (surfaceFlinger != null) {
            try {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeFloat(saturation);
                surfaceFlinger.transact(1022, data, null, 0);
                data.recycle();
                Log.d(TAG, "Saturation applied successfully");
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to apply saturation", e);
            }
        } else {
            Log.e(TAG, "SurfaceFlinger service not found");
        }
    }

    private void applyAutoHbmSettings(Context context) {
        Log.d(TAG, "Applying Auto HBM settings...");
        AutoHbmFragment.toggleAutoHbmService(context);

        ComponentUtils.toggleComponent(
                context,
                AutoHbmActivity.class,
                true
        );

        ComponentUtils.toggleComponent(
                context,
                AutoHbmTileService.class,
                true
        );
        Log.d(TAG, "Auto HBM settings applied");
    }
}
