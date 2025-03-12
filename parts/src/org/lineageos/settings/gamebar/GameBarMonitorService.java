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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import java.util.HashSet;
import java.util.Set;

public class GameBarMonitorService extends Service {

    private Handler mHandler;
    private Runnable mMonitorRunnable;
    private static final long MONITOR_INTERVAL = 2000; // 2 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                monitorForegroundApp();
                mHandler.postDelayed(this, MONITOR_INTERVAL);
            }
        };
        mHandler.post(mMonitorRunnable);
    }

    private void monitorForegroundApp() {
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean masterEnabled = prefs.getBoolean("game_bar_enable", false);
        if (masterEnabled) {
            GameBar.getInstance(this).applyPreferences();
            GameBar.getInstance(this).show();
            return;
        }
        
        boolean autoEnabled = prefs.getBoolean("game_bar_auto_enable", false);
        if (!autoEnabled) {
            GameBar.getInstance(this).hide();
            return;
        }
        
        String foreground = ForegroundAppDetector.getForegroundPackageName(this);
        Set<String> autoApps = prefs.getStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, new HashSet<>());
        if (autoApps.contains(foreground)) {
            GameBar.getInstance(this).applyPreferences();
            GameBar.getInstance(this).show();
        } else {
            GameBar.getInstance(this).hide();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mMonitorRunnable);
    }
}
