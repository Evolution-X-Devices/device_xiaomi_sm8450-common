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

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GameDataExport {

    private static GameDataExport sInstance;
    public static synchronized GameDataExport getInstance() {
        if (sInstance == null) {
            sInstance = new GameDataExport();
        }
        return sInstance;
    }

    private boolean mCapturing = false;

    private final List<String[]> mStatsRows = new ArrayList<>();

    private static final String[] CSV_HEADER = {
            "DateTime",
            "PackageName",
            "FPS",
            "Battery_Temp",
            "CPU_Usage",
            "CPU_Temp",
            "GPU_Usage",
            "GPU_Clock",
            "GPU_Temp"
    };

    private GameDataExport() {
    }

    public void startCapture() {
        mCapturing = true;
        mStatsRows.clear();
        mStatsRows.add(CSV_HEADER);
    }

    public void stopCapture() {
        mCapturing = false;
    }

    public boolean isCapturing() {
        return mCapturing;
    }

    public void addOverlayData(String dateTime,
                               String packageName,
                               String fps,
                               String batteryTemp,
                               String cpuUsage,
                               String cpuTemp,
                               String gpuUsage,
                               String gpuClock,
                               String gpuTemp) {
        if (!mCapturing) return;

        String[] row = {
                dateTime,
                packageName,
                fps,
                batteryTemp,
                cpuUsage,
                cpuTemp,
                gpuUsage,
                gpuClock,
                gpuTemp
        };
        mStatsRows.add(row);
    }

    public void exportDataToCsv() {
        if (mStatsRows.size() <= 1) {
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File outFile = new File(Environment.getExternalStorageDirectory(), "GameBar_log_" + timeStamp + ".csv");

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFile, true));
            for (String[] row : mStatsRows) {
                bw.write(toCsvLine(row));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException ignored) {
        } finally {
            if (bw != null) {
                try { bw.close(); } catch (IOException ignored) {}
            }
        }
    }

    private String toCsvLine(String[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            sb.append(columns[i]);
            if (i < columns.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
