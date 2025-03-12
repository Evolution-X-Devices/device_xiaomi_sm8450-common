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

public class GameBarMemInfo {

    public static String getRamUsage() {
        long memTotal = 0;
        long memAvailable = 0;

        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    memTotal = parseMemValue(line);
                } else if (line.startsWith("MemAvailable:")) {
                    memAvailable = parseMemValue(line);
                }
                if (memTotal > 0 && memAvailable > 0) {
                    break;
                }
            }
        } catch (IOException e) {
            return "N/A";
        }

        if (memTotal == 0) {
            return "N/A";
        }

        long usedKb = (memTotal - memAvailable);
        long usedMb = usedKb / 1024;
        return String.valueOf(usedMb);
    }

    private static long parseMemValue(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return 0;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
