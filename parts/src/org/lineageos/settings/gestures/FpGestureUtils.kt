/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import org.lineageos.settings.R
import org.lineageos.settings.utils.Logging
import vendor.xiaomi.hardware.fingerprintextension.V1_0.IXiaomiFingerprint

object FpGestureUtils {
    private const val TAG = "FpGestureUtils"
    
    // Settings Keys
    const val KEY_ENABLE = "fp_double_tap_enable"
    const val KEY_ACTION = "fp_double_tap_action"
    const val KEY_SCREEN_OFF_ENABLE = "fp_double_tap_screen_off_enable"
    const val KEY_SCREEN_OFF_ACTION = "fp_double_tap_screen_off_action"

    // Actions that make sense with the screen off, as ids shared with the key handler
    val SCREEN_OFF_ACTIONS = listOf(6, 3, 5, 7, 8)
    const val DEFAULT_SCREEN_OFF_ACTION = 6
    
    // Driver Commands
    private const val CMD_LOCKOUT_MODE = 12
    private const val VALUE_DISABLE_NAV = 0
    private const val VALUE_ENABLE_NAV = 2

    fun isAvailable(context: Context): Boolean {
        return try {
            val resId = context.resources.getIdentifier(
                "config_is_powerbutton_fps", "bool", "android"
            )
            if (resId > 0) context.resources.getBoolean(resId) else false
        } catch (e: Exception) {
            false
        }
    }

    fun isEnabled(context: Context): Boolean {
        return Settings.System.getIntForUser(
            context.contentResolver, 
            KEY_ENABLE, 
            0, 
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun getAction(context: Context): Int {
        return Settings.System.getIntForUser(
            context.contentResolver, 
            KEY_ACTION, 
            1, 
            UserHandle.USER_CURRENT
        )
    }

    fun isScreenOffEnabled(context: Context): Boolean {
        return Settings.System.getIntForUser(
            context.contentResolver,
            KEY_SCREEN_OFF_ENABLE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun getScreenOffAction(context: Context): Int {
        return Settings.System.getIntForUser(
            context.contentResolver,
            KEY_SCREEN_OFF_ACTION,
            DEFAULT_SCREEN_OFF_ACTION,
            UserHandle.USER_CURRENT
        )
    }

    fun setScreenOffEnabled(context: Context, enable: Boolean) {
        Settings.System.putIntForUser(
            context.contentResolver,
            KEY_SCREEN_OFF_ENABLE,
            if (enable) 1 else 0,
            UserHandle.USER_CURRENT
        )
    }

    fun setScreenOffAction(context: Context, action: Int) {
        Settings.System.putIntForUser(
            context.contentResolver,
            KEY_SCREEN_OFF_ACTION,
            action,
            UserHandle.USER_CURRENT
        )
    }

    fun setEnabled(context: Context, enable: Boolean) {
        Settings.System.putIntForUser(
            context.contentResolver,
            KEY_ENABLE,
            if (enable) 1 else 0,
            UserHandle.USER_CURRENT
        )
        updateDriver(enable)
    }

    fun setAction(context: Context, action: Int) {
        Settings.System.putIntForUser(
            context.contentResolver,
            KEY_ACTION,
            action,
            UserHandle.USER_CURRENT
        )
    }

    fun getActionLabel(action: Int): Int = when (action) {
        1 -> R.string.action_screenshot
        2 -> R.string.action_assistant
        3 -> R.string.action_play_pause
        4 -> R.string.action_notifications
        5 -> R.string.action_camera
        6 -> R.string.action_flashlight
        7 -> R.string.action_mute
        8 -> R.string.action_vibrate
        9 -> R.string.action_volume
        10 -> R.string.action_sleep
        else -> R.string.action_screenshot
    }

    fun updateDriver(enable: Boolean) {
        try {
            val service = IXiaomiFingerprint.getService()
            if (service != null) {
                service.extCmd(
                    CMD_LOCKOUT_MODE,
                    if (enable) VALUE_ENABLE_NAV else VALUE_DISABLE_NAV
                )
                Logging.d(TAG, "Updated driver navigation state: $enable")
            }
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to communicate with Fingerprint Extension", e)
        }
    }
}
