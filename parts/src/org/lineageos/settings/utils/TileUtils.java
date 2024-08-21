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

package org.lineageos.settings.utils;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.widget.Toast;

import org.lineageos.settings.R;

public class TileUtils {

    public static void requestAddTileService(Context context, Class<?> tileServiceClass, int labelResId, int iconResId) {
        ComponentName componentName = new ComponentName(context, tileServiceClass);
        String label = context.getString(labelResId);
        Icon icon = Icon.createWithResource(context, iconResId);

        StatusBarManager sbm = (StatusBarManager) context.getSystemService(Context.STATUS_BAR_SERVICE);

        if (sbm != null) {
            sbm.requestAddTileService(
                    componentName,
                    label,
                    icon,
                    context.getMainExecutor(),
                    result -> handleResult(context, result)
            );
        }
    }

    private static void handleResult(Context context, Integer result) {
        if (result == null)
            return;
        switch (result) {
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED:
                Toast.makeText(context, R.string.tile_added, Toast.LENGTH_SHORT).show();
                break;
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED:
                Toast.makeText(context, R.string.tile_not_added, Toast.LENGTH_SHORT).show();
                break;
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED:
                Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
