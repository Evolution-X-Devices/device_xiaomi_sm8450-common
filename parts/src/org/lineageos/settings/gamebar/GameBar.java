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

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GameBar {

    private static GameBar sInstance;
    public static synchronized GameBar getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GameBar(context.getApplicationContext());
        }
        return sInstance;
    }

    private static final String FPS_PATH          = "/sys/class/drm/sde-crtc-0/measured_fps";
    private static final String BATTERY_TEMP_PATH = "/sys/class/power_supply/battery/temp";

    private static final String PREF_KEY_X = "game_bar_x";
    private static final String PREF_KEY_Y = "game_bar_y";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mHandler;

    private View mOverlayView;
    private LinearLayout mRootLayout;
    private WindowManager.LayoutParams mLayoutParams;
    private boolean mIsShowing = false;

    private int mTextSizeSp       = 14;
    private int mBackgroundAlpha  = 128;
    private int mCornerRadius     = 90;
    private int mPaddingDp        = 8;
    private String mTitleColorHex = "#FFFFFF";
    private String mValueColorHex = "#FFFFFF";
    private String mOverlayFormat = "full";
    private String mPosition      = "top_center";
    private String mSplitMode     = "side_by_side";
    private int mUpdateIntervalMs = 1000;
    private boolean mDraggable    = false;

    private boolean mShowBatteryTemp = false;
    private boolean mShowCpuUsage    = true;
    private boolean mShowCpuClock    = false;
    private boolean mShowCpuTemp     = false;
    private boolean mShowRam         = false;
    private boolean mShowFps         = true;

    private boolean mShowGpuUsage    = true;
    private boolean mShowGpuClock    = false;
    private boolean mShowGpuTemp     = false;

    private boolean mLongPressEnabled      = false;
    private long mLongPressThresholdMs = 500;
    private boolean mPressActive           = false;
    private float mDownX, mDownY;
    private static final float TOUCH_SLOP = 30f;

    private GestureDetector mGestureDetector;
    private boolean mDoubleTapCaptureEnabled = true;
    private boolean mSingleTapToggleEnabled  = true;
    private GradientDrawable mBgDrawable;

    private int mItemSpacingDp = 8;

    private final Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPressActive) {
                openOverlaySettings();
                mPressActive = false;
            }
        }
    };

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsShowing) {
                updateStats();
                mHandler.postDelayed(this, mUpdateIntervalMs);
            }
        }
    };

    private GameBar(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());

        mBgDrawable = new GradientDrawable();
        applyBackgroundStyle();

        mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mDoubleTapCaptureEnabled) {
                    if (GameDataExport.getInstance().isCapturing()) {
                        GameDataExport.getInstance().stopCapture();
                        Toast.makeText(mContext, "Capture Stopped", Toast.LENGTH_SHORT).show();
                    } else {
                        GameDataExport.getInstance().startCapture();
                        Toast.makeText(mContext, "Capture Started", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mSingleTapToggleEnabled) {
                    mOverlayFormat = "full".equals(mOverlayFormat) ? "minimal" : "full";
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                        .edit()
                        .putString("game_bar_format", mOverlayFormat)
                        .apply();
                    Toast.makeText(mContext, "Overlay Format: " + mOverlayFormat, Toast.LENGTH_SHORT).show();
                    updateStats();
                    return true;
                }
                return super.onSingleTapConfirmed(e);
            }
        });
    }

    public void applyPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mShowFps         = prefs.getBoolean("game_bar_fps_enable", true);
        mShowBatteryTemp = prefs.getBoolean("game_bar_temp_enable", false);
        mShowCpuUsage    = prefs.getBoolean("game_bar_cpu_usage_enable", true);
        mShowCpuClock    = prefs.getBoolean("game_bar_cpu_clock_enable", false);
        mShowCpuTemp     = prefs.getBoolean("game_bar_cpu_temp_enable", false);
        mShowRam         = prefs.getBoolean("game_bar_ram_enable", false);

        mShowGpuUsage    = prefs.getBoolean("game_bar_gpu_usage_enable", true);
        mShowGpuClock    = prefs.getBoolean("game_bar_gpu_clock_enable", false);
        mShowGpuTemp     = prefs.getBoolean("game_bar_gpu_temp_enable", false);

        mDoubleTapCaptureEnabled = prefs.getBoolean("game_bar_doubletap_capture", true);
        mSingleTapToggleEnabled  = prefs.getBoolean("game_bar_single_tap_toggle", true);

        updateSplitMode(prefs.getString("game_bar_split_mode", "side_by_side"));
        updateTextSize(prefs.getInt("game_bar_text_size", 14));
        updateBackgroundAlpha(prefs.getInt("game_bar_background_alpha", 128));
        updateCornerRadius(prefs.getInt("game_bar_corner_radius", 90));
        updatePadding(prefs.getInt("game_bar_padding", 8));
        updateTitleColor(prefs.getString("game_bar_title_color", "#FFFFFF"));
        updateValueColor(prefs.getString("game_bar_value_color", "#4CAF50"));
        updateOverlayFormat(prefs.getString("game_bar_format", "full"));
        updateUpdateInterval(prefs.getString("game_bar_update_interval", "1000"));
        updatePosition(prefs.getString("game_bar_position", "top_center"));

        int spacing = prefs.getInt("game_bar_item_spacing", 8);
        updateItemSpacing(spacing);

        mLongPressEnabled = prefs.getBoolean("game_bar_longpress_enable", true);
        String lpTimeoutStr = prefs.getString("game_bar_longpress_timeout", "500");
        try {
            long lpt = Long.parseLong(lpTimeoutStr);
            setLongPressThresholdMs(lpt);
        } catch (NumberFormatException ignored) {}
    }

    public void show() {
        if (mIsShowing) return;

        applyPreferences();

        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        if ("draggable".equals(mPosition)) {
            mDraggable = true;
            loadSavedPosition(mLayoutParams);
            if (mLayoutParams.x == 0 && mLayoutParams.y == 0) {
                mLayoutParams.gravity = Gravity.TOP | Gravity.START;
                mLayoutParams.x = 0;
                mLayoutParams.y = 100;
            }
        } else {
            mDraggable = false;
            applyPosition(mLayoutParams, mPosition);
        }

        mOverlayView = new LinearLayout(mContext);
        mOverlayView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mRootLayout = (LinearLayout) mOverlayView;
        applySplitMode();
        applyBackgroundStyle();
        applyPadding();

        mOverlayView.setOnTouchListener((v, event) -> {
            if (mGestureDetector != null && mGestureDetector.onTouchEvent(event)) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mDraggable) {
                        initialX = mLayoutParams.x;
                        initialY = mLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                    }
                    if (mLongPressEnabled) {
                        mPressActive = true;
                        mDownX = event.getRawX();
                        mDownY = event.getRawY();
                        mHandler.postDelayed(mLongPressRunnable, mLongPressThresholdMs);
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mLongPressEnabled && mPressActive) {
                        float dx = Math.abs(event.getRawX() - mDownX);
                        float dy = Math.abs(event.getRawY() - mDownY);
                        if (dx > TOUCH_SLOP || dy > TOUCH_SLOP) {
                            mPressActive = false;
                            mHandler.removeCallbacks(mLongPressRunnable);
                        }
                    }
                    if (mDraggable) {
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        mLayoutParams.x = initialX + deltaX;
                        mLayoutParams.y = initialY + deltaY;
                        mWindowManager.updateViewLayout(mOverlayView, mLayoutParams);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mLongPressEnabled && mPressActive) {
                        mPressActive = false;
                        mHandler.removeCallbacks(mLongPressRunnable);
                    }
                    if (mDraggable) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                        prefs.edit()
                                .putInt(PREF_KEY_X, mLayoutParams.x)
                                .putInt(PREF_KEY_Y, mLayoutParams.y)
                                .apply();
                    }
                    return true;
            }
            return false;
        });

        mWindowManager.addView(mOverlayView, mLayoutParams);
        mIsShowing = true;
        startUpdates();

        // Start the FPS meter if using the new API method.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            GameBarFpsMeter.getInstance(mContext).start();
        }
    }

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    public void hide() {
        if (!mIsShowing) return;
        mHandler.removeCallbacksAndMessages(null);
        if (mOverlayView != null) {
            mWindowManager.removeView(mOverlayView);
            mOverlayView = null;
        }
        mIsShowing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            GameBarFpsMeter.getInstance(mContext).stop();
        }
    }

    private void updateStats() {
        if (!mIsShowing || mRootLayout == null) return;

        mRootLayout.removeAllViews();

        List<View> statViews = new ArrayList<>();

        // 1) FPS
        float fpsVal = GameBarFpsMeter.getInstance(mContext).getFps();
        String fpsStr = fpsVal >= 0 ? String.format(Locale.getDefault(), "%.0f", fpsVal) : "N/A";
        if (mShowFps) {
            statViews.add(createStatLine("FPS", fpsStr));
        }

        // 2) Battery temp
        String batteryTempStr = "N/A";
        if (mShowBatteryTemp) {
            String tmp = readLine(BATTERY_TEMP_PATH);
            if (tmp != null && !tmp.isEmpty()) {
                try {
                    int raw = Integer.parseInt(tmp.trim());
                    float c = raw / 10f;
                    batteryTempStr = String.format(Locale.getDefault(), "%.1f", c);
                } catch (NumberFormatException ignored) {}
            }
            statViews.add(createStatLine("Temp", batteryTempStr + "°C"));
        }

        // 3) CPU usage
        String cpuUsageStr = "N/A";
        if (mShowCpuUsage) {
            cpuUsageStr = GameBarCpuInfo.getCpuUsage();
            String display = "N/A".equals(cpuUsageStr) ? "N/A" : cpuUsageStr + "%";
            statViews.add(createStatLine("CPU", display));
        }

        // 4) CPU freq
        if (mShowCpuClock) {
            List<String> freqs = GameBarCpuInfo.getCpuFrequencies();
            if (!freqs.isEmpty()) {
                statViews.add(buildCpuFreqView(freqs));
            }
        }

        // 5) CPU temp
        String cpuTempStr = "N/A";
        if (mShowCpuTemp) {
            cpuTempStr = GameBarCpuInfo.getCpuTemp();
            statViews.add(createStatLine("CPU Temp", "N/A".equals(cpuTempStr) ? "N/A" : cpuTempStr + "°C"));
        }

        // 6) RAM usage
        String ramStr = "N/A";
        if (mShowRam) {
            ramStr = GameBarMemInfo.getRamUsage();
            statViews.add(createStatLine("RAM", "N/A".equals(ramStr) ? "N/A" : ramStr + " MB"));
        }

        // 7) GPU usage
        String gpuUsageStr = "N/A";
        if (mShowGpuUsage) {
            gpuUsageStr = GameBarGpuInfo.getGpuUsage();
            statViews.add(createStatLine("GPU", "N/A".equals(gpuUsageStr) ? "N/A" : gpuUsageStr + "%"));
        }

        // 8) GPU clock
        String gpuClockStr = "N/A";
        if (mShowGpuClock) {
            gpuClockStr = GameBarGpuInfo.getGpuClock();
            statViews.add(createStatLine("GPU Freq", "N/A".equals(gpuClockStr) ? "N/A" : gpuClockStr + "MHz"));
        }

        // 9) GPU temp
        String gpuTempStr = "N/A";
        if (mShowGpuTemp) {
            gpuTempStr = GameBarGpuInfo.getGpuTemp();
            statViews.add(createStatLine("GPU Temp", "N/A".equals(gpuTempStr) ? "N/A" : gpuTempStr + "°C"));
        }

        if ("side_by_side".equals(mSplitMode)) {
            mRootLayout.setOrientation(LinearLayout.HORIZONTAL);
            if ("minimal".equals(mOverlayFormat)) {
                for (int i = 0; i < statViews.size(); i++) {
                    mRootLayout.addView(statViews.get(i));
                    if (i < statViews.size() - 1) {
                        mRootLayout.addView(createDotView());
                    }
                }
            } else {
                for (View view : statViews) {
                    mRootLayout.addView(view);
                }
            }
        } else {
            mRootLayout.setOrientation(LinearLayout.VERTICAL);
            for (View view : statViews) {
                mRootLayout.addView(view);
            }
        }

        if (GameDataExport.getInstance().isCapturing()) {
            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String pkgName = ForegroundAppDetector.getForegroundPackageName(mContext);

            GameDataExport.getInstance().addOverlayData(
                    dateTime,
                    pkgName,
                    fpsStr,
                    batteryTempStr,
                    cpuUsageStr,
                    cpuTempStr,
                    gpuUsageStr,
                    gpuClockStr,
                    gpuTempStr
            );
        }

        if (mLayoutParams != null) {
            mWindowManager.updateViewLayout(mOverlayView, mLayoutParams);
        }
    }

    private View buildCpuFreqView(List<String> freqs) {
        LinearLayout freqContainer = new LinearLayout(mContext);
        freqContainer.setOrientation(LinearLayout.HORIZONTAL);

        int spacingPx = dpToPx(mContext, mItemSpacingDp);
        LinearLayout.LayoutParams outerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        outerLp.setMargins(spacingPx, spacingPx / 2, spacingPx, spacingPx / 2);
        freqContainer.setLayoutParams(outerLp);

        if ("full".equals(mOverlayFormat)) {
            TextView labelTv = new TextView(mContext);
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
            try {
                labelTv.setTextColor(Color.parseColor(mTitleColorHex));
            } catch (Exception e) {
                labelTv.setTextColor(Color.WHITE);
            }
            labelTv.setText("CPU Freq ");
            freqContainer.addView(labelTv);
        }

        LinearLayout verticalFreqs = new LinearLayout(mContext);
        verticalFreqs.setOrientation(LinearLayout.VERTICAL);

        for (String freqLine : freqs) {
            LinearLayout lineLayout = new LinearLayout(mContext);
            lineLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView freqTv = new TextView(mContext);
            freqTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
            try {
                freqTv.setTextColor(Color.parseColor(mValueColorHex));
            } catch (Exception e) {
                freqTv.setTextColor(Color.WHITE);
            }
            freqTv.setText(freqLine);

            lineLayout.addView(freqTv);

            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lineLp.setMargins(spacingPx, spacingPx / 4, spacingPx, spacingPx / 4);
            lineLayout.setLayoutParams(lineLp);

            verticalFreqs.addView(lineLayout);
        }

        freqContainer.addView(verticalFreqs);
        return freqContainer;
    }

    private LinearLayout createStatLine(String title, String rawValue) {
        LinearLayout lineLayout = new LinearLayout(mContext);
        lineLayout.setOrientation(LinearLayout.HORIZONTAL);

        if ("full".equals(mOverlayFormat)) {
            TextView tvTitle = new TextView(mContext);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
            try {
                tvTitle.setTextColor(Color.parseColor(mTitleColorHex));
            } catch (Exception e) {
                tvTitle.setTextColor(Color.WHITE);
            }
            tvTitle.setText(title.isEmpty() ? "" : title + " ");

            TextView tvValue = new TextView(mContext);
            tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
            try {
                tvValue.setTextColor(Color.parseColor(mValueColorHex));
            } catch (Exception e) {
                tvValue.setTextColor(Color.WHITE);
            }
            tvValue.setText(rawValue);

            lineLayout.addView(tvTitle);
            lineLayout.addView(tvValue);
        } else {
            TextView tvMinimal = new TextView(mContext);
            tvMinimal.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
            try {
                tvMinimal.setTextColor(Color.parseColor(mValueColorHex));
            } catch (Exception e) {
                tvMinimal.setTextColor(Color.WHITE);
            }
            tvMinimal.setText(rawValue);
            lineLayout.addView(tvMinimal);
        }

        int spacingPx = dpToPx(mContext, mItemSpacingDp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(spacingPx, spacingPx / 2, spacingPx, spacingPx / 2);
        lineLayout.setLayoutParams(lp);

        return lineLayout;
    }

    private View createDotView() {
        TextView dotView = new TextView(mContext);
        dotView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp);
        try {
            dotView.setTextColor(Color.parseColor(mValueColorHex));
        } catch (Exception e) {
            dotView.setTextColor(Color.WHITE);
        }
        dotView.setText(" . ");
        return dotView;
    }

    public void setShowBatteryTemp(boolean show) { mShowBatteryTemp = show; }
    public void setShowCpuUsage(boolean show)    { mShowCpuUsage = show; }
    public void setShowCpuClock(boolean show)    { mShowCpuClock = show; }
    public void setShowCpuTemp(boolean show)     { mShowCpuTemp = show; }
    public void setShowRam(boolean show)         { mShowRam = show; }
    public void setShowFps(boolean show)         { mShowFps = show; }

    public void setShowGpuUsage(boolean show)    { mShowGpuUsage = show; }
    public void setShowGpuClock(boolean show)    { mShowGpuClock = show; }
    public void setShowGpuTemp(boolean show)     { mShowGpuTemp = show; }

    public void updateTextSize(int sp) {
        mTextSizeSp = sp;
    }

    public void updateCornerRadius(int radius) {
        mCornerRadius = radius;
        applyBackgroundStyle();
    }

    public void updateBackgroundAlpha(int alpha) {
        mBackgroundAlpha = alpha;
        applyBackgroundStyle();
    }

    public void updatePadding(int dp) {
        mPaddingDp = dp;
        applyPadding();
    }

    public void updateTitleColor(String hex) {
        mTitleColorHex = hex;
    }

    public void updateValueColor(String hex) {
        mValueColorHex = hex;
    }

    public void updateOverlayFormat(String format) {
        mOverlayFormat = format;
        if (mIsShowing) {
            updateStats();
        }
    }

    public void updateItemSpacing(int dp) {
        mItemSpacingDp = dp;
        if (mIsShowing) {
            updateStats();
        }
    }

    private void applyBackgroundStyle() {
        int color = Color.argb(mBackgroundAlpha, 0, 0, 0);
        mBgDrawable.setColor(color);
        mBgDrawable.setCornerRadius(mCornerRadius);

        if (mOverlayView != null) {
            mOverlayView.setBackground(mBgDrawable);
        }
    }

    private void applyPadding() {
        if (mRootLayout != null) {
            int px = dpToPx(mContext, mPaddingDp);
            mRootLayout.setPadding(px, px, px, px);
        }
    }

    public void updatePosition(String pos) {
        mPosition = pos;
        if (mIsShowing && mOverlayView != null && mLayoutParams != null) {
            if ("draggable".equals(mPosition)) {
                mDraggable = true;
                loadSavedPosition(mLayoutParams);
                if (mLayoutParams.x == 0 && mLayoutParams.y == 0) {
                    mLayoutParams.gravity = Gravity.TOP | Gravity.START;
                    mLayoutParams.x = 0;
                    mLayoutParams.y = 100;
                }
            } else {
                mDraggable = false;
                applyPosition(mLayoutParams, mPosition);
            }
            mWindowManager.updateViewLayout(mOverlayView, mLayoutParams);
        }
    }

    public void updateSplitMode(String mode) {
        mSplitMode = mode;
        if (mIsShowing && mOverlayView != null) {
            applySplitMode();
            updateStats();
        }
    }

    public void updateUpdateInterval(String intervalStr) {
        try {
            mUpdateIntervalMs = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            mUpdateIntervalMs = 1000;
        }
        if (mIsShowing) {
            startUpdates();
        }
    }

    public void setLongPressEnabled(boolean enabled) {
        mLongPressEnabled = enabled;
    }
    public void setLongPressThresholdMs(long ms) {
        mLongPressThresholdMs = ms;
    }

    public void setDoubleTapCaptureEnabled(boolean enabled) {
        mDoubleTapCaptureEnabled = enabled;
    }

    public void setSingleTapToggleEnabled(boolean enabled) {
        mSingleTapToggleEnabled = enabled;
    }

    private void startUpdates() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(mUpdateRunnable);
    }

    private void applySplitMode() {
        if (mRootLayout == null) return;
        if ("side_by_side".equals(mSplitMode)) {
            mRootLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mRootLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    private void loadSavedPosition(WindowManager.LayoutParams lp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int savedX = prefs.getInt(PREF_KEY_X, Integer.MIN_VALUE);
        int savedY = prefs.getInt(PREF_KEY_Y, Integer.MIN_VALUE);
        if (savedX != Integer.MIN_VALUE && savedY != Integer.MIN_VALUE) {
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = savedX;
            lp.y = savedY;
        }
    }

    private void applyPosition(WindowManager.LayoutParams lp, String pos) {
        switch (pos) {
            case "top_left":
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.x = 0;
                lp.y = 100;
                break;
            case "top_center":
                lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                lp.y = 100;
                break;
            case "top_right":
                lp.gravity = Gravity.TOP | Gravity.END;
                lp.x = 0;
                lp.y = 100;
                break;
            case "bottom_left":
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                lp.x = 0;
                lp.y = 100;
                break;
            case "bottom_center":
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                lp.y = 100;
                break;
            case "bottom_right":
                lp.gravity = Gravity.BOTTOM | Gravity.END;
                lp.x = 0;
                lp.y = 100;
                break;
            default:
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.x = 0;
                lp.y = 100;
                break;
        }
    }

    private String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(mContext, GameBarSettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (Exception e) {
            // Exception ignored
        }
    }

    private static int dpToPx(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
