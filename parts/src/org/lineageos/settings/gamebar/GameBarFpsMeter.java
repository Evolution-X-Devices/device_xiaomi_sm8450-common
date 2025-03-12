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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.WindowManager;
import android.window.TaskFpsCallback;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GameBarFpsMeter {

    private static final float TOLERANCE = 0.1f;
    private static final long STALENESS_THRESHOLD_MS = 2000;
    private static final long TASK_CHECK_INTERVAL_MS = 1000;

    private static GameBarFpsMeter sInstance;
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final SharedPreferences mPrefs;
    private float mCurrentFps = 0f;
    private TaskFpsCallback mTaskFpsCallback;
    private boolean mCallbackRegistered = false;
    private int mCurrentTaskId = -1;
    private long mLastFpsUpdateTime = System.currentTimeMillis();
    private final android.os.Handler mHandler = new android.os.Handler();

    public static synchronized GameBarFpsMeter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GameBarFpsMeter(context.getApplicationContext());
        }
        return sInstance;
    }

    private GameBarFpsMeter(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mTaskFpsCallback = new TaskFpsCallback() {
                @Override
                public void onFpsReported(float fps) {
                    if (fps > 0) {
                        mCurrentFps = fps;
                        mLastFpsUpdateTime = System.currentTimeMillis();
                    }
                }
            };
        }
    }

    public void start() {
        String method = mPrefs.getString("game_bar_fps_method", "new");
        if (!"new".equals(method)) return;

        stop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int taskId = getFocusedTaskId();
            if (taskId <= 0) {
                return;
            }
            mCurrentTaskId = taskId;
            try {
                mWindowManager.registerTaskFpsCallback(mCurrentTaskId, Runnable::run, mTaskFpsCallback);
                mCallbackRegistered = true;
            } catch (Exception e) {
            }
            mLastFpsUpdateTime = System.currentTimeMillis();
            mHandler.postDelayed(mTaskCheckRunnable, TASK_CHECK_INTERVAL_MS);
        }
    }

    public void stop() {
        String method = mPrefs.getString("game_bar_fps_method", "new");
        if ("new".equals(method) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mCallbackRegistered) {
                try {
                    mWindowManager.unregisterTaskFpsCallback(mTaskFpsCallback);
                } catch (Exception e) {
                }
                mCallbackRegistered = false;
            }
            mHandler.removeCallbacks(mTaskCheckRunnable);
        }
    }

    public float getFps() {
        String method = mPrefs.getString("game_bar_fps_method", "new");
        if ("legacy".equals(method)) {
            return readLegacyFps();
        } else {
            return mCurrentFps;
        }
    }

    private float readLegacyFps() {
        try (BufferedReader br = new BufferedReader(new FileReader("/sys/class/drm/sde-crtc-0/measured_fps"))) {
            String line = br.readLine();
            if (line != null && line.startsWith("fps:")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    return Float.parseFloat(parts[1].trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
        }
        return -1f;
    }

    private final Runnable mTaskCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int newTaskId = getFocusedTaskId();
                if (newTaskId > 0 && newTaskId != mCurrentTaskId) {
                    reinitCallback();
                } else {
                    long now = System.currentTimeMillis();
                    if (now - mLastFpsUpdateTime > STALENESS_THRESHOLD_MS) {
                        reinitCallback();
                    }
                }
                mHandler.postDelayed(this, TASK_CHECK_INTERVAL_MS);
            }
        }
    };

    private int getFocusedTaskId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return -1;
        }
        try {
            Class<?> atmClass = Class.forName("android.app.ActivityTaskManager");
            Method getServiceMethod = atmClass.getDeclaredMethod("getService");
            Object atmService = getServiceMethod.invoke(null);
            Method getFocusedRootTaskInfoMethod = atmService.getClass().getMethod("getFocusedRootTaskInfo");
            Object taskInfo = getFocusedRootTaskInfoMethod.invoke(atmService);
            if (taskInfo != null) {
                try {
                    Field taskIdField = taskInfo.getClass().getField("taskId");
                    return taskIdField.getInt(taskInfo);
                } catch (NoSuchFieldException nsfe) {
                    try {
                        Field taskIdField = taskInfo.getClass().getField("mTaskId");
                        return taskIdField.getInt(taskInfo);
                    } catch (NoSuchFieldException nsfe2) {
                    }
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    private void reinitCallback() {
        stop();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }, 500);
    }
}
