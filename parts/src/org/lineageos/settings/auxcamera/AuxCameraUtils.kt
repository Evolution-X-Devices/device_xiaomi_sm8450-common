/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera

import android.content.Context
import android.provider.Settings
import org.lineageos.settings.utils.Logging

object AuxCameraUtils {

    private const val TAG = "AuxCameraUtils"

    private const val PROP = "persist.sys.aux_camera_control"
    private const val SETTINGS_KEY = "aux_camera_excluded_apps"
    private const val ENABLED_KEY = "aux_camera_enabled"

    // Keep in sync with vendor.camera.aux.packageexcludelist in properties/vendor.prop
    private const val DEFAULT_EXCLUDED_APPS =
        "com.adobe.scan.android,com.tencent.mm,us.zoom.videomeetings,in.org.npci.upiapp"

    @Volatile
    private var cachedApps: Set<String>? = null

    fun isEnabled(context: Context): Boolean =
        Settings.System.getInt(
            context.contentResolver,
            ENABLED_KEY,
            0,
        ) == 1

    fun setEnabled(context: Context, enabled: Boolean) {
        Settings.System.putInt(
            context.contentResolver,
            ENABLED_KEY,
            if (enabled) 1 else 0,
        )

        if (enabled) {
            restore(context)
        } else {
            applyProp(DEFAULT_EXCLUDED_APPS)
        }
    }

    fun getApps(context: Context): Set<String> {
        cachedApps?.let { return it }

        val value = Settings.System.getString(
            context.contentResolver,
            SETTINGS_KEY,
        ) ?: DEFAULT_EXCLUDED_APPS

        val validApps = buildSet {
            value.split(',')
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { pkg ->
                    runCatching {
                        context.packageManager.getApplicationInfo(pkg, 0)
                    }.onSuccess {
                        add(pkg)
                    }
                }
        }

        val cleaned = validApps.joinToString(",")

        if (cleaned != value) {
            Settings.System.putString(
                context.contentResolver,
                SETTINGS_KEY,
                cleaned,
            )
            Logging.i(TAG, "Cleaned aux camera app list")
        }

        cachedApps = validApps
        return validApps
    }

    fun isAppEnabled(
        context: Context,
        packageName: String,
    ): Boolean = packageName in getApps(context)

    fun setAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        val apps = getApps(context).toMutableSet()

        if (enabled) {
            apps += packageName
        } else {
            apps -= packageName
        }

        cachedApps = apps

        val serialized = apps.joinToString(",")

        Settings.System.putString(
            context.contentResolver,
            SETTINGS_KEY,
            serialized,
        )

        if (isEnabled(context)) {
            applyProp(serialized.ifBlank { "none" })
        }
    }

    fun restore(context: Context) {
        if (!isEnabled(context)) return

        applyProp(
            getApps(context)
                .joinToString(",")
                .ifBlank { "none" },
        )
    }

    private fun applyProp(value: String) {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod(
            "set",
            String::class.java,
            String::class.java,
        )

        method.invoke(
            null,
            PROP,
            value.ifBlank { "none" },
        )
    }
}
