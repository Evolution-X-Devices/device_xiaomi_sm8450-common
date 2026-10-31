/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charge

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import org.lineageos.settings.utils.FileUtils

/**
 * Wired fast charge boost, written to the nodes HyperOS uses (SecurityCenter through the
 * MiCharge HAL): sport_mode 1/0 and thermal_message/charger_temp 8/0.
 *
 * Optionally marks the charger as verified (pd_verifed) when it never reached fastchg_mode 1.
 * Every node is checked first, so a kernel that lacks one just skips it.
 */
object ChargeBoostUtils {
    private const val TAG = "ChargeBoost"

    const val KEY_BOOST = "charge_boost_enabled"
    const val KEY_FORCE_AUTH = "charge_boost_force_auth"

    private const val NODE_SPORT_MODE = "/sys/class/qcom-battery/sport_mode"
    private const val NODE_FASTCHG_MODE = "/sys/class/qcom-battery/fastchg_mode"
    private const val NODE_PD_VERIFED = "/sys/class/qcom-battery/pd_verifed"
    private const val NODE_CHARGER_TEMP = "/sys/class/thermal/thermal_message/charger_temp"

    // HyperOS tells the thermal driver about boost with charger_temp 8, and 0 without it.
    private const val CHARGER_TEMP_BOOST = "8"

    // Battery temperature guard, in tenths of a degree Celsius.
    const val TEMP_HOT = 420
    const val TEMP_RESUME = 400

    /** Set by [ChargeBoostService] while the battery is above the temperature guard. */
    @Volatile
    var hot = false

    fun isSupported(): Boolean =
        FileUtils.fileExists(NODE_SPORT_MODE) || FileUtils.fileExists(NODE_CHARGER_TEMP)

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_BOOST, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_BOOST, enabled).apply()
        applyBoost(context)
    }

    fun isForceAuthEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_FORCE_AUTH, false)

    fun setForceAuthEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_FORCE_AUTH, enabled).apply()
        nudgeAuthentication(context)
    }

    private fun readFastChargeMode(): String? =
        if (FileUtils.fileExists(NODE_FASTCHG_MODE)) {
            FileUtils.readOneLine(NODE_FASTCHG_MODE)?.trim()
        } else {
            null
        }

    fun applyBoost(context: Context) {
        val on = isEnabled(context) && !hot
        val sport = FileUtils.fileExists(NODE_SPORT_MODE) &&
                FileUtils.writeLine(NODE_SPORT_MODE, if (on) "1" else "0")
        val temp = FileUtils.fileExists(NODE_CHARGER_TEMP) &&
                FileUtils.writeLine(NODE_CHARGER_TEMP, if (on) CHARGER_TEMP_BOOST else "0")
        Log.i(
            TAG, "Boost ${if (on) "on" else "off"} (sport_mode=$sport, charger_temp=$temp, " +
                    "fastchg_mode=${readFastChargeMode()})"
        )
    }

    /**
     * Marks the charger as verified when it never reached the Xiaomi fast mode. Only runs while
     * the boost is on and the battery is not hot, and only when fastchg_mode can be read.
     */
    fun nudgeAuthentication(context: Context) {
        if (!isForceAuthEnabled(context) || !isEnabled(context) || hot) return
        if (!FileUtils.fileExists(NODE_FASTCHG_MODE)) {
            Log.i(TAG, "No fastchg_mode node, not forcing charger authentication")
            return
        }
        val mode = FileUtils.readOneLine(NODE_FASTCHG_MODE)?.trim()
        if (mode == "1") return
        if (!FileUtils.fileExists(NODE_PD_VERIFED)) {
            Log.i(TAG, "fastchg_mode is $mode but there is no pd_verifed node")
            return
        }
        val ok = FileUtils.writeLine(NODE_PD_VERIFED, "1")
        Log.i(TAG, "fastchg_mode was $mode, wrote pd_verifed: $ok")
    }

    fun startService(context: Context) {
        if (isSupported()) {
            context.startService(Intent(context, ChargeBoostService::class.java))
        }
    }
}
