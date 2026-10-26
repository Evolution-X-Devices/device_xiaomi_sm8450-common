/*
 * Copyright (C) 2023 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.aospa.keyhandler

import android.app.SearchManager
import android.app.StatusBarManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager.ScreenshotSource.SCREENSHOT_VENDOR_GESTURE
import android.util.Log

import com.android.internal.app.AssistUtils
import com.android.internal.util.ScreenshotHelper

class FpDoubleTapHandler(
    private val context: Context
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)!!
    private val cameraManager = context.getSystemService(CameraManager::class.java)!!
    private val powerManager = context.getSystemService(PowerManager::class.java)!!
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val statusBarManager = context.getSystemService(StatusBarManager::class.java)!!
    private val vibrator = context.getSystemService(Vibrator::class.java)!!

    private val handler = Handler(Looper.getMainLooper())
    private val screenshotHelper = ScreenshotHelper(context)

    private val vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    private val vibrationAttrs =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)

    private val isFpDoubleTapEnabled: Boolean
        get() =
            Settings.System.getIntForUser(context.contentResolver, SETTING_KEY_ENABLE,
                0, UserHandle.USER_CURRENT) == 1

    private val fpDoubleTapAction: Int
        get() =
            Settings.System.getIntForUser(context.contentResolver, SETTING_KEY_ACTION,
                1, UserHandle.USER_CURRENT)

    private val isScreenOffEnabled: Boolean
        get() =
            Settings.System.getIntForUser(context.contentResolver, SETTING_KEY_SCREEN_OFF_ENABLE,
                0, UserHandle.USER_CURRENT) == 1

    private val screenOffAction: Int
        get() =
            Settings.System.getIntForUser(context.contentResolver, SETTING_KEY_SCREEN_OFF_ACTION,
                ACTION_FLASHLIGHT, UserHandle.USER_CURRENT)

    private var torchOn = false
    private var screenOn = true
    private var screenOnRunnable = Runnable { screenOn = true }

    init {
        cameraManager.registerTorchCallback(
            object: CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == REAR_CAMERA_ID) {
                        torchOn = enabled
                    }
                }
            },
            handler
        )
        context.registerReceiver(
            object: BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    dlog("onReceive: ${intent.action}")
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> {
                            handler.removeCallbacks(screenOnRunnable)
                            screenOn = false
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            handler.postDelayed(screenOnRunnable, UNLOCK_WAIT_MS)
                        }
                    }
                }
            },
            IntentFilter(Intent.ACTION_USER_PRESENT).apply {
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
    }

    fun handleEvent(event: KeyEvent) {
        val enabled = isFpDoubleTapEnabled
        val action = fpDoubleTapAction
        val interactive = powerManager.isInteractive()
        dlog("handleEvent: enabled=$enabled action=$action"
                + " screenOn=$screenOn interactive=$interactive")
        if (enabled && !interactive && event.action == KeyEvent.ACTION_UP) {
            handleScreenOffEvent()
            return
        }
        if (!screenOn || !enabled || !interactive || event.action != KeyEvent.ACTION_UP) {
            dlog("wont handle")
            return
        }
        when (action) {
            1 -> takeScreenshot()
            2 -> launchAssist(event.eventTime)
            3 -> playPauseMedia()
            4 -> showNotifications()
            5 -> launchCamera()
            6 -> toggleFlashlight()
            7 -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            8 -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            9 -> showVolumePanel()
            10 -> goToSleep()
            else -> Log.e(TAG, "unsupported action: $action")
        }
    }

    private fun handleScreenOffEvent() {
        if (!isScreenOffEnabled) {
            dlog("screen off gesture disabled")
            return
        }
        val action = screenOffAction
        dlog("handleScreenOffEvent: action=$action")
        runIfNotCovered {
            when (action) {
                ACTION_PLAY_PAUSE -> playPauseMedia()
                ACTION_CAMERA -> launchCameraFromScreenOff()
                ACTION_FLASHLIGHT -> toggleFlashlight()
                ACTION_MUTE -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
                ACTION_VIBRATE -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
                else -> Log.e(TAG, "unsupported screen off action: $action")
            }
        }
    }

    /**
     * Runs [block] only if the proximity sensor reports nothing close to the device, so a double
     * tap registered by the sensor in a pocket or bag does nothing. The first reading after
     * registering is used; if none arrives in time the gesture is dropped.
     */
    private fun runIfNotCovered(block: () -> Unit) {
        val manager = sensorManager
        val sensor = proximitySensor
        if (manager == null || sensor == null) {
            block()
            return
        }
        val listener = object : SensorEventListener {
            private var handled = false

            override fun onSensorChanged(sensorEvent: SensorEvent) {
                if (handled) return
                handled = true
                manager.unregisterListener(this)
                handler.removeCallbacksAndMessages(this)
                val covered = sensorEvent.values[0] < sensor.maximumRange
                dlog("proximity=${sensorEvent.values[0]} covered=$covered")
                if (!covered) block()
            }

            override fun onAccuracyChanged(s: Sensor, accuracy: Int) {}
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler)
        handler.postAtTime({
            manager.unregisterListener(listener)
            dlog("proximity: no reading in time, dropping gesture")
        }, listener, SystemClock.uptimeMillis() + PROXIMITY_TIMEOUT_MS)
    }

    private fun launchCameraFromScreenOff() {
        dlog("launchCameraFromScreenOff")
        vibrate()
        powerManager.wakeUp(
            SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, "fp-double-tap")
        context.startActivityAsUser(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            UserHandle.CURRENT
        )
    }

    private fun takeScreenshot() {
        dlog("takeScreenshot")
        screenshotHelper.takeScreenshot(SCREENSHOT_VENDOR_GESTURE, handler, null)
    }

    private fun launchAssist(eventTime: Long) {
        dlog("launchAssist: eventTime=$eventTime")
        val searchManager = context.getSystemService(SearchManager::class.java)
        if (searchManager == null) {
            dlog("launchAssist: searchManager is null!")
            return
        }
        vibrate()
        val args = Bundle()
        args.putLong(Intent.EXTRA_TIME, eventTime)
        args.putInt(AssistUtils.INVOCATION_TYPE_KEY, AssistUtils.INVOCATION_TYPE_PHYSICAL_GESTURE)
        searchManager.launchAssist(args)
    }

    private fun playPauseMedia() {
        dlog("playPauseMedia")
        vibrate()
        val time = SystemClock.uptimeMillis()
        var event = KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
        audioManager.dispatchMediaKeyEvent(event)
        event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP)
        audioManager.dispatchMediaKeyEvent(event)
    }

    private fun showNotifications() {
        dlog("showNotifications")
        statusBarManager.expandNotificationsPanel()
    }

    private fun launchCamera() {
        dlog("launchCamera")
        vibrate()
        context.startActivity(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun toggleFlashlight() {
        dlog("toggleFlashlight: torchOn=$torchOn")
        vibrate()
        cameraManager.setTorchMode(REAR_CAMERA_ID, !torchOn)
    }

    private fun toggleRingerMode(ringerMode: Int) {
        val currentMode = audioManager.getRingerModeInternal()
        dlog("toggleRingerMode: $ringerMode currentMode=$currentMode")
        vibrate()
        audioManager.setRingerModeInternal(
            if (currentMode != ringerMode) ringerMode else AudioManager.RINGER_MODE_NORMAL
        )
    }

    private fun showVolumePanel() {
        dlog("showVolumePanel")
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    private fun goToSleep() {
        dlog("goToSleep")
        vibrate()
        powerManager.goToSleep(SystemClock.uptimeMillis())
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(vibrationEffect, vibrationAttrs)
        }
    }

    companion object {
        private const val TAG = "FpDoubleTapHandler"
        private const val REAR_CAMERA_ID = "0"
        private const val SETTING_KEY_ENABLE = "fp_double_tap_enable"
        private const val SETTING_KEY_ACTION = "fp_double_tap_action"
        private const val SETTING_KEY_SCREEN_OFF_ENABLE = "fp_double_tap_screen_off_enable"
        private const val SETTING_KEY_SCREEN_OFF_ACTION = "fp_double_tap_screen_off_action"
        private const val ACTION_PLAY_PAUSE = 3
        private const val ACTION_CAMERA = 5
        private const val ACTION_FLASHLIGHT = 6
        private const val ACTION_MUTE = 7
        private const val ACTION_VIBRATE = 8
        private const val PROXIMITY_TIMEOUT_MS = 300L
        private const val UNLOCK_WAIT_MS = 1500L

        fun dlog(msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, msg)
            }
        }
    }
}
