/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import org.lineageos.settings.utils.Logging

object AuxCameraUtils {

    private const val TAG = "AuxCameraUtils"

    private const val PROP =
        "persist.sys.aux_camera_control"

    private const val SETTINGS_KEY =
        "aux_camera_excluded_apps"

    private const val ENABLED_KEY =
        "aux_camera_enabled"

    fun isEnabled(context: Context): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            ENABLED_KEY,
            0
        ) == 1
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        Settings.System.putInt(
            context.contentResolver,
            ENABLED_KEY,
            if (enabled) 1 else 0
        )

        if (enabled) {
            restore(context)
        } else {
            applyProp("none")
        }
    }

    fun getApps(context: Context): Set<String> {
        val pm = context.packageManager

        val value = Settings.System.getString(
            context.contentResolver,
            SETTINGS_KEY
        ) ?: return emptySet()

        val validApps = value
            .split(",")
            .filter { it.isNotBlank() }
            .filter {
                try {
                    pm.getApplicationInfo(it, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            }
            .toSet()

        val cleaned = validApps.joinToString(",")

        if (cleaned != value) {
            Settings.System.putString(
                context.contentResolver,
                SETTINGS_KEY,
                cleaned
            )
            Logging.i(TAG, "Removed uninstalled apps from aux camera list")
        }

        return validApps
    }

    fun isAppEnabled(
        context: Context,
        packageName: String
    ): Boolean {
        return getApps(context).contains(packageName)
    }

    fun setAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean
    ) {
        val apps = getApps(context).toMutableSet()

        if (enabled) {
            apps.add(packageName)
        } else {
            apps.remove(packageName)
        }

        val updated = apps.joinToString(",")

        Settings.System.putString(
            context.contentResolver,
            SETTINGS_KEY,
            updated
        )

        if (isEnabled(context)) {
            applyProp(
                if (updated.isBlank()) {
                    "none"
                } else {
                    updated
                }
            )
        }
    }

    fun restore(context: Context) {
        if (!isEnabled(context)) {
            return
        }

        val value = Settings.System.getString(
            context.contentResolver,
            SETTINGS_KEY
        )

        applyProp(
            if (value.isNullOrBlank()) {
                "none"
            } else {
                value
            }
        )
    }

    private fun applyProp(value: String) {
        val safeValue = value.ifBlank { "none" }

        val clazz = Class.forName(
            "android.os.SystemProperties"
        )

        val method = clazz.getMethod(
            "set",
            String::class.java,
            String::class.java
        )

        method.invoke(
            null,
            PROP,
            safeValue
        )
    }
}
