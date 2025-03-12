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

package org.lineageos.settings.gamebar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;

public class GameBarBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            restoreOverlayState(context);
        }
    }

    private void restoreOverlayState(Context context) {
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mainEnabled = prefs.getBoolean("game_bar_enable", false);
        boolean autoEnabled = prefs.getBoolean("game_bar_auto_enable", false);
        if (mainEnabled) {
            GameBar.getInstance(context).applyPreferences();
            GameBar.getInstance(context).show();
        }
        if (autoEnabled) {
            Intent monitorIntent = new Intent(context, GameBarMonitorService.class);
            context.startService(monitorIntent);
        }
    }
}
