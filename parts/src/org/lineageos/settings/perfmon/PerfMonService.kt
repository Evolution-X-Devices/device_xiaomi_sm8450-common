/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.lineageos.settings.R
import org.lineageos.settings.perfmon.ui.PerfMonOverlayContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PerfMonService : LifecycleService(), SavedStateRegistryOwner {

    companion object {
        private const val NOTIFICATION_ID = 9081

        private const val LOOP_INTERVAL = 250L
        private const val FPS_INTERVAL = 250L
        private const val FAST_INTERVAL = 1000L
        private const val SLOW_INTERVAL = 3000L
        private const val IDLE_INTERVAL = 2000L
    }

    private lateinit var windowManager: WindowManager
    private lateinit var utils: PerfMonUtils

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val _statsFlow = MutableStateFlow(PerfMonStatsSnapshot())
    private val statsFlow = _statsFlow.asStateFlow()

    private var monitorJob: Job? = null
    private var isDisplayAttached = true

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        utils = PerfMonUtils.getInstance(this)

        createNotificationChannel()
        registerDisplayReceiver()
        lifecycleScope.launch {
            utils.configFlow.collect { config ->
                updateTouchPassthrough(config.touchPassthrough)
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)

        if (!utils.isEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (composeView == null) {
            showOverlay()
        }

        startMonitoring()

        return START_STICKY
    }

    private fun showOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PerfMonService)
            setViewTreeSavedStateRegistryOwner(this@PerfMonService)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )

            setContent {
                PerfMonOverlayContent(
                    statsFlow = statsFlow,
                    configFlow = utils.configFlow
                )
            }
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            if (utils.touchPassthrough) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 120
        }

        attachDragListener(composeView!!)
        windowManager.addView(composeView, layoutParams)
    }
    private fun updateTouchPassthrough(enabled: Boolean) {
        layoutParams?.let { params ->
            params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            if (enabled) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

            composeView?.let {
                windowManager.updateViewLayout(it, params)
                }
        }
    }
    private fun startMonitoring() {
        if (monitorJob?.isActive == true || !isDisplayAttached) return

        monitorJob = lifecycleScope.launch(Dispatchers.IO) {
            var lastFpsPoll = 0L
            var lastFastPoll = 0L
            var lastSlowPoll = 0L

            while (isActive) {
                val config = utils.configFlow.value

                if (!config.hasEnabledMetrics()) {
                    delay(IDLE_INTERVAL)
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                var snapshot = _statsFlow.value

                if (config.showFps &&
                    now - lastFpsPoll >= FPS_INTERVAL
                ) {
                    snapshot = snapshot.copy(
                        fps = PerfMonHardwareMonitors.readFps()
                    )
                    lastFpsPoll = now
                }

                if (now - lastFastPoll >= FAST_INTERVAL) {
                    snapshot = updateFastMetrics(snapshot, config)
                    lastFastPoll = now
                }

                if (now - lastSlowPoll >= SLOW_INTERVAL) {
                    snapshot = updateSlowMetrics(snapshot, config)
                    lastSlowPoll = now
                }

                if (snapshot != _statsFlow.value) {
                    _statsFlow.value = snapshot
                }

                delay(LOOP_INTERVAL)
            }
        }
    }

    private fun updateFastMetrics(
        snapshot: PerfMonStatsSnapshot,
        config: PerfMonConfigSnapshot
    ): PerfMonStatsSnapshot {
        return snapshot.copy(
            cpuTotal = if (config.showCpuUsage)
                PerfMonHardwareMonitors.readCpuUsage()
            else snapshot.cpuTotal,

            gpuUsage = if (config.showGpuUsage)
                PerfMonHardwareMonitors.readGpuUsage()
            else snapshot.gpuUsage,

            gpuSpeed = if (config.showGpuSpeed)
                "${PerfMonHardwareMonitors.readGpuClock()}MHz"
            else snapshot.gpuSpeed
        )
    }

    private fun updateSlowMetrics(
        snapshot: PerfMonStatsSnapshot,
        config: PerfMonConfigSnapshot
    ): PerfMonStatsSnapshot {
        return snapshot.copy(
            cpuTemp = if (config.showCpuTemp)
                PerfMonHardwareMonitors.readCpuTemp()
            else snapshot.cpuTemp,

            cpuClusters = if (config.showCpuClusters)
                PerfMonHardwareMonitors.readCpuFrequencies()
            else snapshot.cpuClusters,

            gpuTemp = if (config.showGpuTemp)
                PerfMonHardwareMonitors.readGpuTemp()
            else snapshot.gpuTemp,

            ramUsage = if (config.showRam)
                PerfMonHardwareMonitors.readRamUsage()
            else snapshot.ramUsage
        )
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "perfmon_channel",
                "Performance Monitor",
                NotificationManager.IMPORTANCE_MIN
            )

            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        startForeground(
            NOTIFICATION_ID,
            Notification.Builder(this, "perfmon_channel")
                .setContentTitle(getString(R.string.perfmon_title))
                .setContentText("Overlay is active")
                .setSmallIcon(R.drawable.ic_perfmon_settings)
                .build()
        )
    }

    private fun registerDisplayReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        registerReceiver(
            displayReceiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isDisplayAttached = false
                    stopMonitoring()
                }

                Intent.ACTION_SCREEN_ON -> {
                    isDisplayAttached = true
                    if (utils.isEnabled) startMonitoring()
                }
            }
        }
    }

    private fun attachDragListener(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams!!.x
                    startY = layoutParams!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX =
                        startX + (event.rawX - touchX).toInt()

                    val newY =
                        startY + (event.rawY - touchY).toInt()

                    if (layoutParams!!.x != newX ||
                        layoutParams!!.y != newY
                    ) {
                        layoutParams!!.x = newX
                        layoutParams!!.y = newY
                        windowManager.updateViewLayout(view, layoutParams)
                    }

                    true
                }

                else -> false
            }
        }
    }

    override fun onDestroy() {
        stopMonitoring()

        runCatching {
            unregisterReceiver(displayReceiver)
        }

        PerfMonHardwareMonitors.closeAll()

        composeView?.let {
            it.disposeComposition()
            windowManager.removeViewImmediate(it)
        }

        composeView = null

        super.onDestroy()
    }
}

private fun PerfMonConfigSnapshot.hasEnabledMetrics() =
    showFps ||
        showCpuUsage ||
        showCpuTemp ||
        showCpuClusters ||
        showGpuUsage ||
        showGpuTemp ||
        showGpuSpeed ||
        showRam
