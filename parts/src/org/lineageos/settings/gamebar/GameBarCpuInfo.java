/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.gamebar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameBarCpuInfo {

    private static long sPrevIdle = -1;
    private static long sPrevTotal = -1;

    private static final String CPU_TEMP_PATH = "/sys/class/thermal/thermal_zone0/temp";

    public static String getCpuUsage() {
        String line = readLine("/proc/stat");
        if (line == null || !line.startsWith("cpu ")) return "N/A";
        String[] parts = line.split("\\s+");
        if (parts.length < 8) return "N/A";

        try {
            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = Long.parseLong(parts[5]);
            long irq     = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);
            long steal   = parts.length > 8 ? Long.parseLong(parts[8]) : 0;

            long total = user + nice + system + idle + iowait + irq + softirq + steal;

            if (sPrevTotal != -1 && total != sPrevTotal) {
                long diffTotal = total - sPrevTotal;
                long diffIdle  = idle - sPrevIdle;
                long usage = 100 * (diffTotal - diffIdle) / diffTotal;
                sPrevTotal = total;
                sPrevIdle  = idle;
                return String.valueOf(usage);
            } else {

                sPrevTotal = total;
                sPrevIdle  = idle;
                return "N/A";
            }
        } catch (NumberFormatException e) {
            return "N/A";
        }
    }

    public static List<String> getCpuFrequencies() {
        List<String> result = new ArrayList<>();
        String cpuDirPath = "/sys/devices/system/cpu/";
        java.io.File cpuDir = new java.io.File(cpuDirPath);
        java.io.File[] files = cpuDir.listFiles((dir, name) -> name.matches("cpu\\d+"));
        if (files == null || files.length == 0) {
            return result;
        }

        List<java.io.File> cpuFolders = new ArrayList<>();
        Collections.addAll(cpuFolders, files);
        cpuFolders.sort(Comparator.comparingInt(GameBarCpuInfo::extractCpuNumber));

        for (java.io.File cpu : cpuFolders) {
            String freqPath = cpu.getAbsolutePath() + "/cpufreq/scaling_cur_freq";
            String freqStr = readLine(freqPath);
            if (freqStr != null && !freqStr.isEmpty()) {
                try {
                    int khz = Integer.parseInt(freqStr.trim());
                    int mhz = khz / 1000;
                    result.add(cpu.getName() + ": " + mhz + " MHz");
                } catch (NumberFormatException e) {
                    result.add(cpu.getName() + ": N/A");
                }
            } else {
                result.add(cpu.getName() + ": offline or frequency not available");
            }
        }
        return result;
    }

    public static String getCpuTemp() {
        String line = readLine(CPU_TEMP_PATH);
        if (line == null) return "N/A";
        line = line.trim();
        try {
            float raw = Float.parseFloat(line);
            float c   = raw / 1000f;
            return String.format("%.1f", c);
        } catch (NumberFormatException e) {
            return "N/A";
        }
    }

    private static int extractCpuNumber(java.io.File cpuFolder) {
        String name = cpuFolder.getName().replace("cpu", "");
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
