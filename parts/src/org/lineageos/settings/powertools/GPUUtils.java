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

import android.os.SystemProperties;

public final class GPUUtils {

    public static void setGPUMinFrequency(String frequencyHz) {
        SystemProperties.set("persist.sys.parts.gpu.min_frequency", frequencyHz);
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("persist.sys.parts.gpu.min_clock", String.valueOf(freqLong / 1000000));
        } catch (Exception ignored) {}
    }

    public static void setGPUMaxFrequency(String frequencyHz) {
        SystemProperties.set("persist.sys.parts.gpu.max_frequency", frequencyHz);
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("persist.sys.parts.gpu.max_clock", String.valueOf(freqLong / 1000000));
        } catch (Exception ignored) {}
    }
}
