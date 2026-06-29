/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfmon

import java.io.File
import java.io.RandomAccessFile
import java.util.HashMap

object PerfMonHardwareMonitors {

    private const val CPU_STAT_PATH = "/proc/stat"
    private const val MEMINFO_PATH = "/proc/meminfo"
    private const val CPU_TEMP_PATH = "/sys/class/thermal/thermal_zone0/temp"
    private const val GPU_USAGE_PATH = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
    private const val GPU_CLOCK_PATH = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    private const val GPU_TEMP_PATH = "/sys/class/kgsl/kgsl-3d0/temp"
    private const val FPS_PATH = "/sys/class/drm/sde-crtc-0/measured_fps"

    private val cachedReaders = HashMap<String, RandomAccessFile>(16)

    private val cpuFreqPaths: List<String> by lazy {
        File("/sys/devices/system/cpu")
            .listFiles { _, name ->
                name.startsWith("cpu") &&
                    name.length > 3 &&
                    name.substring(3).all(Char::isDigit)
            }
            ?.sortedBy { it.name.substring(3).toInt() }
            ?.map { "${it.absolutePath}/cpufreq/scaling_cur_freq" }
            ?: emptyList()
    }

    private var prevIdle = -1L
    private var prevTotal = -1L

    fun readFps(): String {
        return try {
            val line = readNodeString(FPS_PATH)
            if (line.isEmpty()) return "N/A"
            val start = line.indexOf("fps:")
            if (start == -1) return "N/A"
            val valueStart = start + 5
            val valueEnd = line.indexOf(' ', valueStart)
            val value = if (valueEnd == -1) line.substring(valueStart)
            else line.substring(valueStart, valueEnd)
            value.toFloatOrNull()?.toInt()?.toString() ?: "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    fun readCpuUsage(): String {
        val line = readNodeString(CPU_STAT_PATH)
        if (!line.startsWith("cpu ")) return "N/A"

        return try {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 8) return "N/A"

            var total = 0L
            for (i in 1..7) {
                total += parts[i].toLong()
            }

            val idle = parts[4].toLong()

            if (prevTotal == -1L) {
                prevTotal = total
                prevIdle = idle
                return "N/A"
            }

            val diffTotal = total - prevTotal
            val diffIdle = idle - prevIdle

            prevTotal = total
            prevIdle = idle

            if (diffTotal <= 0L) return "N/A"

            "${100 * (diffTotal - diffIdle) / diffTotal}%"

        } catch (_: Exception) {
            "N/A"
        }
    }

    fun readCpuTemp(): String {
        val raw = readNodeString(CPU_TEMP_PATH).toFloatOrNull() ?: return "N/A"

        return if (raw > 1000f) {
            "%.1f°C".format(raw / 1000f)
        } else {
            "${raw.toInt()}°C"
        }
    }

    fun readCpuFrequencies(): List<String> {
        val result = ArrayList<String>(cpuFreqPaths.size)

        cpuFreqPaths.forEachIndexed { index, path ->
            val freq = readNodeString(path).toLongOrNull()
            if (freq != null) {
                result.add("cpu$index: ${freq / 1000}MHz")
            }
        }

        return result
    }

    fun readGpuUsage(): String {
        val value = readNodeString(GPU_USAGE_PATH)
        return if (value.isEmpty()) "N/A" else value.removeSuffix("%")
    }

    fun readGpuClock(): String {
        return runCatching {
            (readNodeString(GPU_CLOCK_PATH).toLong() / 1_000_000).toString()
        }.getOrDefault("N/A")
    }

    fun readGpuTemp(): String {
        val raw = readNodeString(GPU_TEMP_PATH).toFloatOrNull() ?: return "N/A"

        return if (raw > 1000f) {
            "%.1f°C".format(raw / 1000f)
        } else {
            "${raw.toInt()}°C"
        }
    }

    fun readRamUsage(): String {
        return try {
            val reader = cachedReaders.getOrPut(MEMINFO_PATH) {
                RandomAccessFile(MEMINFO_PATH, "r")
            }

            reader.seek(0)

            var memTotal = 0L
            var memAvailable = 0L

            repeat(10) {
                val line = reader.readLine() ?: return@repeat

                when {
                    line.startsWith("MemTotal:") -> {
                        memTotal = line.filter(Char::isDigit).toLongOrNull() ?: 0L
                    }

                    line.startsWith("MemAvailable:") -> {
                        memAvailable = line.filter(Char::isDigit).toLongOrNull() ?: 0L
                    }
                }

                if (memTotal != 0L && memAvailable != 0L) {
                    return "${(memTotal - memAvailable) / 1024}MB"
                }
            }

            "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    private fun readNodeString(path: String): String {
        return try {
            val reader = cachedReaders.getOrPut(path) {
                RandomAccessFile(path, "r")
            }

            reader.seek(0)
            reader.readLine()?.trim().orEmpty()

        } catch (_: Exception) {
            ""
        }
    }

    fun closeAll() {
        cachedReaders.values.forEach {
            runCatching { it.close() }
        }
        cachedReaders.clear()
    }
}
