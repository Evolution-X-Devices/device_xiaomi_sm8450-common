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

package org.lineageos.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.util.Log;

import org.lineageos.settings.autohbm.AutoHbmActivity;
import org.lineageos.settings.autohbm.AutoHbmTileService;

public final class TileHandlerActivity extends Activity {
    private static final String TAG = "TileHandlerActivity";

    @Override
    protected void onCreate(final android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        try {
            if (android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES.equals(intent.getAction())) {
                final ComponentName qsTile =
                        intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
                final String qsName = qsTile.getClassName();
                final Intent aIntent = new Intent();

                if (qsName.equals(AutoHbmTileService.class.getName())) {
                    aIntent.setClass(this, AutoHbmActivity.class);
                } else {
                    aIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    aIntent.setData(Uri.fromParts("package", qsTile.getPackageName(), null));
                }

                aIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(aIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling intent: " + intent, e);
        } finally {
            finish();
        }
    }
}

