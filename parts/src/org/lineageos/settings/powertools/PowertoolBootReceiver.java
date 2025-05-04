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

package org.lineageos.settings.powertools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;

public class PowertoolBootReceiver extends BroadcastReceiver {

    private static final String GPU_DEFAULT_MIN = "220000000";
    private static final String GPU_DEFAULT_MAX = "580000000";

    private static final String CPU_LITTLE_DEFAULT_MIN = "300000";
    private static final String CPU_LITTLE_DEFAULT_MAX = "1804800";
    private static final String CPU_LITTLE_DEFAULT_GOV = "walt";

    private static final String CPU_BIG_DEFAULT_MIN = "633600";
    private static final String CPU_BIG_DEFAULT_MAX = "2496000";
    private static final String CPU_BIG_DEFAULT_GOV = "walt";

    private static final String CPU_PRIME_DEFAULT_MIN = "787200";
    private static final String CPU_PRIME_DEFAULT_MAX = "2918400";
    private static final String CPU_PRIME_DEFAULT_GOV = "walt";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean gpuEnabled = prefs.getBoolean("gpu_enable", false);
            if (gpuEnabled) {
                String gpuMin = prefs.getString("gpu_min_frequency", GPU_DEFAULT_MIN);
                String gpuMax = prefs.getString("gpu_max_frequency", GPU_DEFAULT_MAX);
                GPUUtils.setGPUMinFrequency(gpuMin);
                GPUUtils.setGPUMaxFrequency(gpuMax);
            } else {
                GPUUtils.setGPUMinFrequency(GPU_DEFAULT_MIN);
                GPUUtils.setGPUMaxFrequency(GPU_DEFAULT_MAX);
            }
            boolean cpuEnabled = prefs.getBoolean("cpu_enable", false);
            if (cpuEnabled) {
                String littleMin = prefs.getString("cpu_little_min_frequency", CPU_LITTLE_DEFAULT_MIN);
                String littleMax = prefs.getString("cpu_little_max_frequency", CPU_LITTLE_DEFAULT_MAX);
                String littleGov = prefs.getString("cpu_little_governor", CPU_LITTLE_DEFAULT_GOV);
                CPUUtils.setCPULittleFreq(littleMin, littleMax, littleGov);

                String bigMin = prefs.getString("cpu_big_min_frequency", CPU_BIG_DEFAULT_MIN);
                String bigMax = prefs.getString("cpu_big_max_frequency", CPU_BIG_DEFAULT_MAX);
                String bigGov = prefs.getString("cpu_big_governor", CPU_BIG_DEFAULT_GOV);
                CPUUtils.setCPUBigFreq(bigMin, bigMax, bigGov);

                String primeMin = prefs.getString("cpu_prime_min_frequency", CPU_PRIME_DEFAULT_MIN);
                String primeMax = prefs.getString("cpu_prime_max_frequency", CPU_PRIME_DEFAULT_MAX);
                String primeGov = prefs.getString("cpu_prime_governor", CPU_PRIME_DEFAULT_GOV);
                CPUUtils.setCPUPrimeFreq(primeMin, primeMax, primeGov);
            } else {
                CPUUtils.setCPULittleFreq(CPU_LITTLE_DEFAULT_MIN, CPU_LITTLE_DEFAULT_MAX, CPU_LITTLE_DEFAULT_GOV);
                CPUUtils.setCPUBigFreq(CPU_BIG_DEFAULT_MIN, CPU_BIG_DEFAULT_MAX, CPU_BIG_DEFAULT_GOV);
                CPUUtils.setCPUPrimeFreq(CPU_PRIME_DEFAULT_MIN, CPU_PRIME_DEFAULT_MAX, CPU_PRIME_DEFAULT_GOV);
            }
        }
    }
}
