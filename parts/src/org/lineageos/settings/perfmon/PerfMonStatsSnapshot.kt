/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon

import androidx.compose.runtime.Immutable

@Immutable
data class PerfMonStatsSnapshot(
    val fps: String = "0",
    val cpuTotal: String = "0%",
    val cpuTemp: String = "0°C",
    val cpuClusters: List<String> = emptyList(),
    val gpuUsage: String = "0%",
    val gpuTemp: String = "0°C",
    val gpuSpeed: String = "0MHz",
    val ramUsage: String = "0MB"
)
