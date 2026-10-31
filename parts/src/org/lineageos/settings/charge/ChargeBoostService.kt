/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charge

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/** Re-applies the boost after each charger handshake and holds it off while the battery is hot. */
class ChargeBoostService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val applyAll = Runnable {
        ChargeBoostUtils.nudgeAuthentication(this)
        ChargeBoostUtils.applyBoost(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> scheduleApply()
                Intent.ACTION_POWER_DISCONNECTED -> handler.removeCallbacks(applyAll)
                Intent.ACTION_BATTERY_CHANGED -> checkTemperature(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val sticky = registerReceiver(receiver, filter)
        if (sticky != null) {
            // Catch a battery that is already hot before the next broadcast arrives.
            checkTemperature(sticky)
            if (sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) > 0) {
                scheduleApply()
            }
        }
    }

    // The charger handshake can reset the nodes, so apply a few times after plug-in.
    private fun scheduleApply() {
        handler.removeCallbacks(applyAll)
        APPLY_DELAYS_MS.forEach { handler.postDelayed(applyAll, it) }
    }

    private fun checkTemperature(status: Intent) {
        val temp = status.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        var hot = ChargeBoostUtils.hot
        if (!hot && temp >= ChargeBoostUtils.TEMP_HOT) {
            hot = true
        } else if (hot && temp <= ChargeBoostUtils.TEMP_RESUME) {
            hot = false
        }
        if (hot != ChargeBoostUtils.hot) {
            ChargeBoostUtils.hot = hot
            Log.i(TAG, "Battery at ${temp / 10f}C, boost ${if (hot) "held off" else "resumed"}")
            ChargeBoostUtils.applyBoost(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(applyAll)
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ChargeBoostService"
        private val APPLY_DELAYS_MS = longArrayOf(2000, 5000, 12000)
    }
}
