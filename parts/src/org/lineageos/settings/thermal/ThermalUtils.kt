/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.provider.MediaStore
import android.telecom.DefaultDialerManager.getDefaultDialerApplication
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import org.lineageos.settings.R
import org.lineageos.settings.utils.dlog
import org.lineageos.settings.utils.FileUtils
import com.android.settingslib.applications.AppUtils.isBrowserApp

/** Helper utility class for thermal profiles. */
class ThermalUtils
private constructor(
    private val context: Context,
) {
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val serviceIntent = Intent(context, ThermalService::class.java)

    var enabled: Boolean = sharedPrefs.getBoolean(THERMAL_ENABLED, false)
        set(value) {
            if (field == value) return
            field = value
            sharedPrefs.edit().putBoolean(THERMAL_ENABLED, value).apply()
            if (value) {
                startService()
            } else {
                setDefaultThermalProfile()
                stopService()
            }
        }

    var value: String = readValue()
        set(value) {
            if (field == value) return
            field = value
            writeValue(value)
        }

    fun startService() {
        if (enabled) {
            dlog(TAG, "startService")
            context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }
    }

    fun stopService() {
        dlog(TAG, "stopService")
        context.stopService(serviceIntent)
    }

    private fun writeValue(value: String) {
        dlog(TAG, "writing pref value: $value")
        sharedPrefs.edit().putString(THERMAL_CONTROL, value).apply()
    }

    private fun readValue(): String = sharedPrefs.getString(THERMAL_CONTROL, null) ?: DEFAULT_VALUE

    fun writePackage(packageName: String, mode: Int) {
        dlog(TAG, "writePackage: $packageName -> $mode")
        var newValue = value.replace("$packageName,", "")
        val modes = newValue.split(":").toMutableList()
        modes[mode] += "$packageName,"
        value = modes.joinToString(":")
    }

    fun getStateForPackage(packageName: String): ThermalState {
        val modes = value.split(":")
        return ThermalState.values().find { state -> modes[state.id].contains("$packageName,") }
            ?: getDefaultStateForPackage(packageName)
    }

    fun resetProfiles() {
        dlog(TAG, "resetProfiles")
        value = DEFAULT_VALUE
    }

    fun setDefaultThermalProfile() {
        dlog(TAG, "setDefaultThermalProfile")
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_OFF)
    }

    fun setThermalProfile(packageName: String) {
        if (packageName.isEmpty()) {
            dlog(TAG, "setThermalProfile: packageName is empty")
            return
        }
        val state = getStateForPackage(packageName)
        dlog(TAG, "setThermalProfile: $packageName -> $state")
        FileUtils.writeLine(THERMAL_SCONFIG, state.config)
    }

    private fun getDefaultStateForPackage(packageName: String): ThermalState {
        runCatching { context.packageManager.getApplicationInfo(packageName, 0) }
            .onSuccess {
                when (it.category) {
                    ApplicationInfo.CATEGORY_GAME -> return ThermalState.GAMING
                    ApplicationInfo.CATEGORY_VIDEO -> return ThermalState.VIDEO
                    ApplicationInfo.CATEGORY_MAPS -> return ThermalState.NAVIGATION
                }
            }
            .onFailure {
                return ThermalState.DEFAULT
            }

        return when {
            NAVIGATION_PACKAGES.contains(packageName) -> ThermalState.NAVIGATION
            VIDEO_CALL_PACKAGES.contains(packageName) -> ThermalState.VIDEOCALL
            BENCHMARKING_APPS.contains(packageName) -> ThermalState.BENCHMARK
            getDefaultDialerApplication(context) == packageName -> ThermalState.DIALER
            isBrowserApp(context, packageName, UserHandle.myUserId()) -> ThermalState.BROWSER
            isCameraApp(packageName) -> ThermalState.CAMERA
            else -> ThermalState.DEFAULT
        }
    }

    private fun isCameraApp(packageName: String): Boolean {
        val cameraIntent =
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setPackage(packageName)
        val activities =
            context.packageManager.queryIntentActivitiesAsUser(
                cameraIntent,
                PackageManager.MATCH_ALL,
                UserHandle.myUserId(),
            )
        return activities.any { it.activityInfo != null }
    }

    enum class ThermalState(
        val id: Int,
        val config: String,
        val prefix: String,
        @StringRes val label: Int,
    ) {
        BENCHMARK(
            0,
            "10", // thermal-nolimits.conf
            "thermal.benchmark=",
            R.string.thermal_benchmark,
        ),
        BROWSER(
            1,
            "11", // thermal-class0.conf
            "thermal.browser=",
            R.string.thermal_browser,
        ),
        CAMERA(
            2,
            "12", // thermal-camera.conf
            "thermal.camera=",
            R.string.thermal_camera,
        ),
        DIALER(
            3,
            "8", // thermal-phone.conf
            "thermal.dialer=",
            R.string.thermal_dialer,
        ),
        GAMING(
            4,
            "13", // thermal-tgame.conf
            "thermal.gaming=",
            R.string.thermal_gaming,
        ),
        NAVIGATION(
            5,
            "19", // thermal-navigation.conf
            "thermal.navigation=",
            R.string.thermal_navigation,
        ),
        VIDEOCALL(
            6,
            "4", // thermal-videochat.conf
            "thermal.streaming=",
            R.string.thermal_streaming,
        ),
        VIDEO(
            7,
            "21", // thermal-video.conf
            "thermal.video=",
            R.string.thermal_video,
        ),
        DEFAULT(
            8,
            "0", // thermal-normal.conf
            "thermal.default=",
            R.string.thermal_default,
        ),
    }

    companion object {
        private const val TAG = "ThermalUtils"
        private const val THERMAL_CONTROL = "thermal_control_v2"
        private const val THERMAL_ENABLED = "thermal_enabled"

        private const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"
        private const val THERMAL_STATE_OFF = "20" // thermal-mgame.conf

        // Empty value to store if shared preference is null
        private val DEFAULT_VALUE = ThermalState.values().map { it.prefix }.joinToString(":")

        private val NAVIGATION_PACKAGES =
            arrayOf(
                "com.google.android.apps.maps",
                "com.google.android.apps.mapslite",
                "com.waze",
            )
        private val VIDEO_CALL_PACKAGES =
            arrayOf(
                "com.google.android.apps.tachyon",
                "us.zoom.videomeetings",
                "com.microsoft.teams",
                "com.skype.raider",
            )
        private val BENCHMARKING_APPS =
            arrayOf(
                "com.primatelabs.geekbench5",
                "com.primatelabs.geekbench6",
                "com.antutu.ABenchMark",
                "com.futuremark.dmandroid.application",
                "com.futuremark.pcmark.android.benchmark",
                "com.glbenchmark.glbenchmark27",
                "com.texts.throttlebench",
                "skynet.cputhrottlingtest",
            )

        @Volatile private var instance: ThermalUtils? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: ThermalUtils(context.applicationContext).also { instance = it }
                }
    }
}
