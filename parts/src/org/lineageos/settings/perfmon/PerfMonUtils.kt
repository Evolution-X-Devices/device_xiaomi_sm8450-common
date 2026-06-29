/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.perfmon

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.UserHandle
import androidx.preference.PreferenceManager
import org.lineageos.settings.utils.Logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PerfMonUtils private constructor(
    private val context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val serviceIntent by lazy {
        Intent(context, PerfMonService::class.java)
    }

    private val _configFlow = MutableStateFlow(getSnapshot())
    val configFlow = _configFlow.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean("perfmon_enabled", false)
        set(value) {
            prefs.edit().putBoolean("perfmon_enabled", value).apply()
            if (value) startService() else stopService()
        }

    var showCompact: Boolean
        get() = prefs.getBoolean("perfmon_compact", false)
        set(value) = prefs.edit().putBoolean("perfmon_compact", value).apply()

    var touchPassthrough: Boolean
        get() = prefs.getBoolean("perfmon_touch_passthrough", false)
        set(value) = prefs.edit().putBoolean("perfmon_touch_passthrough", value).apply()

    var showCpuUsage: Boolean
        get() = prefs.getBoolean("perfmon_show_cpu_usage", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_cpu_usage", value).apply()

    var showCpuTemp: Boolean
        get() = prefs.getBoolean("perfmon_show_cpu_temp", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_cpu_temp", value).apply()

    var showCpuClusters: Boolean
        get() = prefs.getBoolean("perfmon_show_cpu_clusters", false)
        set(value) = prefs.edit().putBoolean("perfmon_show_cpu_clusters", value).apply()

    var showGpuUsage: Boolean
        get() = prefs.getBoolean("perfmon_show_gpu_usage", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_gpu_usage", value).apply()

    var showGpuTemp: Boolean
        get() = prefs.getBoolean("perfmon_show_gpu_temp", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_gpu_temp", value).apply()

    var showGpuSpeed: Boolean
        get() = prefs.getBoolean("perfmon_show_gpu_speed", false)
        set(value) = prefs.edit().putBoolean("perfmon_show_gpu_speed", value).apply()

    var showFps: Boolean
        get() = prefs.getBoolean("perfmon_show_fps", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_fps", value).apply()

    var showRam: Boolean
        get() = prefs.getBoolean("perfmon_show_ram", true)
        set(value) = prefs.edit().putBoolean("perfmon_show_ram", value).apply()

    var overlayAlpha: Float
        get() = prefs.getFloat("perfmon_alpha", 0.70f)
        set(value) = prefs.edit().putFloat("perfmon_alpha", value).apply()

    var textSizeSp: Float
        get() = prefs.getFloat("perfmon_text_size", 13f)
        set(value) = prefs.edit().putFloat("perfmon_text_size", value).apply()

    private fun startService() {
        context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
    }

    private fun stopService() {
        context.stopService(serviceIntent)
    }

    fun startServiceIfEnabled() {
        if (isEnabled) {
            Logging.d("PerfMonUtils", "Boot completed: Starting service")
            startService()
        }
    }

    override fun onSharedPreferenceChanged(
        prefs: SharedPreferences?,
        key: String?
    ) {
        if (key?.startsWith("perfmon_") == true) {
            _configFlow.value = getSnapshot()
        }
    }

    private fun getSnapshot() = PerfMonConfigSnapshot(
        alpha = overlayAlpha,
        textSize = textSizeSp,
        showFps = showFps,
        showCpuUsage = showCpuUsage,
        showCpuTemp = showCpuTemp,
        showCpuClusters = showCpuClusters,
        showGpuUsage = showGpuUsage,
        showGpuTemp = showGpuTemp,
        showGpuSpeed = showGpuSpeed,
        showRam = showRam,
        showCompact = showCompact,
        touchPassthrough = touchPassthrough
    )

    companion object {
        @Volatile
        private var instance: PerfMonUtils? = null

        @JvmStatic
        fun getInstance(context: Context): PerfMonUtils =
            instance ?: synchronized(this) {
                instance ?: PerfMonUtils(
                    context.applicationContext
                ).also {
                    instance = it
                }
            }
    }
}

data class PerfMonConfigSnapshot(
    val alpha: Float,
    val textSize: Float,
    val showFps: Boolean,
    val showCpuUsage: Boolean,
    val showCpuTemp: Boolean,
    val showCpuClusters: Boolean,
    val showGpuUsage: Boolean,
    val showGpuTemp: Boolean,
    val showGpuSpeed: Boolean,
    val showRam: Boolean,
    val showCompact: Boolean,
    val touchPassthrough: Boolean
)
