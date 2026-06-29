/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import org.lineageos.settings.R
import org.lineageos.settings.perfmon.PerfMonUtils
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveScreen
import org.lineageos.settings.ui.ExpressiveSliderRow
import org.lineageos.settings.ui.ExpressiveSwitchRow

@Composable
fun PerfMonScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { PerfMonUtils.getInstance(context) }

    var isEnabled by remember { mutableStateOf(utils.isEnabled) }
    var alpha by remember { mutableFloatStateOf(utils.overlayAlpha) }
    var textSize by remember { mutableFloatStateOf(utils.textSizeSp) }
    var showCompact by remember { mutableStateOf(utils.showCompact) }
    var showFps by remember { mutableStateOf(utils.showFps) }
    var showCpuUsage by remember { mutableStateOf(utils.showCpuUsage) }
    var showCpuTemp by remember { mutableStateOf(utils.showCpuTemp) }
    var showCpuClusters by remember { mutableStateOf(utils.showCpuClusters) }
    var showGpuUsage by remember { mutableStateOf(utils.showGpuUsage) }
    var showGpuTemp by remember { mutableStateOf(utils.showGpuTemp) }
    var showGpuSpeed by remember { mutableStateOf(utils.showGpuSpeed) }
    var showRam by remember { mutableStateOf(utils.showRam) }
    var touchPassthrough by remember { mutableStateOf(utils.touchPassthrough) }

    ExpressiveScaffold(
        title = stringResource(R.string.perfmon_title),
        onBack = onBackPressed,
    ) { padding ->
        ExpressiveScreen(modifier = Modifier.padding(padding)) {
            ExpressiveGroup(title = "") {
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_enable),
                        summaryText = stringResource(R.string.perfmon_summary),
                        isChecked = isEnabled,
                        onChange = {
                            isEnabled = it
                            utils.isEnabled = it
                        },
                    )
                }
            }

            ExpressiveGroup(title = stringResource(R.string.perfmon_appearance)) {
                item {
                    ExpressiveSliderRow(
                        title = stringResource(R.string.perfmon_bg_transparency),
                        value = (alpha * 100).toInt(),
                        valueRange = 10..100,
                        valueLabel = { "$it%" },
                        onValueChange = {
                            alpha = it / 100f
                            utils.overlayAlpha = alpha
                        },
                    )
                }
                item {
                    ExpressiveSliderRow(
                        title = stringResource(R.string.perfmon_text_size),
                        value = textSize.toInt(),
                        valueRange = 10..24,
                        enabled = !showCompact,
                        onValueChange = {
                            textSize = it.toFloat()
                            utils.textSizeSp = textSize
                        },
                    )
                }
            }

            ExpressiveGroup(title = stringResource(R.string.perfmon_metrics)) {
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_touch_passthrough),
                        summaryText = null,
                        isChecked = touchPassthrough,
                        onChange = { touchPassthrough = it; utils.touchPassthrough = it },
                    )
                }
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_compact_mode),
                        summaryText = null,
                        isChecked = showCompact,
                        onChange = { showCompact = it; utils.showCompact = it },
                    )
                }
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_show_fps),
                        summaryText = null,
                        isChecked = showFps,
                        onChange = { showFps = it; utils.showFps = it },
                    )
                }
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_show_cpu),
                        summaryText = null,
                        isChecked = showCpuUsage,
                        onChange = { showCpuUsage = it; utils.showCpuUsage = it },
                    )
                }
                if (showCpuUsage) {
                    item {
                        ExpressiveSwitchRow(
                            titleText = stringResource(R.string.perfmon_show_cpu_temp),
                            summaryText = null,
                            isChecked = showCpuTemp,
                            onChange = { showCpuTemp = it; utils.showCpuTemp = it },
                        )
                    }
                    if (!showCompact) {
                        item {
                            ExpressiveSwitchRow(
                                titleText = stringResource(R.string.perfmon_show_cpu_clusters),
                                summaryText = null,
                                isChecked = showCpuClusters,
                                onChange = { showCpuClusters = it; utils.showCpuClusters = it },
                            )
                        }
                    }
                }
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_show_gpu),
                        summaryText = null,
                        isChecked = showGpuUsage,
                        onChange = { showGpuUsage = it; utils.showGpuUsage = it },
                    )
                }
                if (showGpuUsage) {
                    item {
                        ExpressiveSwitchRow(
                            titleText = stringResource(R.string.perfmon_show_gpu_temp),
                            summaryText = null,
                            isChecked = showGpuTemp,
                            onChange = { showGpuTemp = it; utils.showGpuTemp = it },
                        )
                    }
                    if (!showCompact) {
                        item {
                            ExpressiveSwitchRow(
                                titleText = stringResource(R.string.perfmon_show_gpu_speed),
                                summaryText = null,
                                isChecked = showGpuSpeed,
                                onChange = { showGpuSpeed = it; utils.showGpuSpeed = it },
                            )
                        }
                    }
                }
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.perfmon_show_ram),
                        summaryText = null,
                        isChecked = showRam,
                        onChange = { showRam = it; utils.showRam = it },
                    )
                }
            }
        }
    }
}
