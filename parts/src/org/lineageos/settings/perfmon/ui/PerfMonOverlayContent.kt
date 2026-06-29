/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.settings.perfmon.PerfMonConfigSnapshot
import org.lineageos.settings.perfmon.PerfMonStatsSnapshot
import kotlinx.coroutines.flow.StateFlow

private val OverlayShape = RoundedCornerShape(10.dp)

@Composable
fun PerfMonOverlayContent(
    statsFlow: StateFlow<PerfMonStatsSnapshot>,
    configFlow: StateFlow<PerfMonConfigSnapshot>
) {
    val stats by statsFlow.collectAsState()
    val config by configFlow.collectAsState()

    val backgroundColor = remember(config.alpha) {
        Color.Black.copy(alpha = config.alpha)
    }

    Column(
        modifier = Modifier
            .background(backgroundColor, OverlayShape)
            .padding(8.dp)
    ) {
        if (config.showCompact) {
            CompactOverlay(stats, config)
        } else {
            ExpandedOverlay(stats, config)
        }
    }
}

@Composable
private fun CompactOverlay(
    stats: PerfMonStatsSnapshot,
    config: PerfMonConfigSnapshot
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (config.showFps) {
            MiniItem("FPS", stats.fps)
        }

        if (config.showCpuUsage) {
            val cpuValue = remember(
                stats.cpuTotal,
                stats.cpuTemp,
                config.showCpuTemp
            ) {
                buildString {
                    append(cleanUsage(stats.cpuTotal))
                    if (config.showCpuTemp) {
                        append(" | ")
                        append(stats.cpuTemp)
                    }
                }
            }

            MiniItem("CPU", cpuValue)
        }
    }

    if (config.showGpuUsage || config.showRam) {
        Spacer(Modifier.height(2.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (config.showGpuUsage) {
                val gpuValue = remember(
                    stats.gpuUsage,
                    stats.gpuTemp,
                    config.showGpuTemp
                ) {
                    buildString {
                        append(cleanUsage(stats.gpuUsage))
                        if (config.showGpuTemp) {
                            append(" | ")
                            append(stats.gpuTemp)
                        }
                    }
                }

                MiniItem("GPU", gpuValue)
            }

            if (config.showRam) {
                MiniItem("RAM", stats.ramUsage)
            }
        }
    }
}

@Composable
private fun ExpandedOverlay(
    stats: PerfMonStatsSnapshot,
    config: PerfMonConfigSnapshot
) {
    val textSize = config.textSize.sp
    val secondarySize = (config.textSize * 0.85f).sp

    if (config.showFps) {
        MetricRow("FPS", stats.fps, Color(0xFF4CAF50), textSize)
    }

    if (config.showCpuUsage) {
        val cpuValue = remember(
            stats.cpuTotal,
            stats.cpuTemp,
            config.showCpuTemp
        ) {
            buildString {
                append(cleanUsage(stats.cpuTotal))
                if (config.showCpuTemp) {
                    append(" | ")
                    append(stats.cpuTemp)
                }
            }
        }

        MetricRow("CPU", cpuValue, Color(0xFF58A6FF), textSize)

        if (config.showCpuClusters && stats.cpuClusters.isNotEmpty()) {
            CpuClusterSection(
                clusters = stats.cpuClusters,
                textSize = secondarySize
            )
        }
    }

    if (config.showGpuUsage) {
        val gpuValue = remember(
            stats.gpuUsage,
            stats.gpuTemp,
            config.showGpuTemp
        ) {
            buildString {
                append(cleanUsage(stats.gpuUsage))
                if (config.showGpuTemp) {
                    append(" | ")
                    append(stats.gpuTemp)
                }
            }
        }

        MetricRow("GPU", gpuValue, Color(0xFFFF8626), textSize)

        if (config.showGpuSpeed) {
            Text(
                text = "speed:${stats.gpuSpeed.replace(" ", "")}",
                color = Color(0xFFFFB347),
                fontSize = secondarySize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (config.showRam) {
        MetricRow("RAM", stats.ramUsage, Color(0xFFFFC107), textSize)
    }
}

@Composable
private fun CpuClusterSection(
    clusters: List<String>,
    textSize: TextUnit
) {
    Column(modifier = Modifier.padding(top = 2.dp)) {
        for (i in clusters.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = clusters[i].replace(" ", ""),
                    color = Color(0xFF86D3FF),
                    fontSize = textSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                if (i + 1 < clusters.size) {
                    Text(
                        text = clusters[i + 1].replace(" ", ""),
                        color = Color(0xFF86D3FF),
                        fontSize = textSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun cleanUsage(value: String): String = buildString {
    value.forEach {
        if (it.isDigit()) append(it)
    }
    append('%')
}

@Composable
private fun MiniItem(
    label: String,
    value: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color,
    size: TextUnit
) {
    Row(
        modifier = Modifier.padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )

        Text(
            text = value,
            color = color,
            fontSize = size,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}
