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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.settings.R;

public class PowertoolsSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener,
                   SharedPreferences.OnSharedPreferenceChangeListener {

    // Power Profile
    private static final String KEY_POWER_PROFILE_MODE = "power_profile_mode";
    private ListPreference mPowerProfilePreference;
    private PowerProfileUtil mPowerProfileUtil;

    // GPU settings keys
    private static final String KEY_GPU_ENABLE = "gpu_enable";
    private static final String KEY_GPU_MIN_FREQ = "gpu_min_frequency";
    private static final String KEY_GPU_MAX_FREQ = "gpu_max_frequency";
    private SwitchPreferenceCompat mGpuEnablePref;
    private ListPreference mGpuMinFreqPref;
    private ListPreference mGpuMaxFreqPref;
    private String[] mGpuFreqEntries;
    private String[] mGpuFreqValues;
    private static final String GPU_DEFAULT_MIN = "220000000";
    private static final String GPU_DEFAULT_MAX = "580000000";

    // CPU settings keys
    private static final String KEY_CPU_ENABLE = "cpu_enable";
    private static final String KEY_CPU_LITTLE_MIN_FREQ = "cpu_little_min_frequency";
    private static final String KEY_CPU_LITTLE_MAX_FREQ = "cpu_little_max_frequency";
    private static final String KEY_CPU_LITTLE_GOVERNOR = "cpu_little_governor";
    private static final String KEY_CPU_BIG_MIN_FREQ = "cpu_big_min_frequency";
    private static final String KEY_CPU_BIG_MAX_FREQ = "cpu_big_max_frequency";
    private static final String KEY_CPU_BIG_GOVERNOR = "cpu_big_governor";
    private static final String KEY_CPU_PRIME_MIN_FREQ = "cpu_prime_min_frequency";
    private static final String KEY_CPU_PRIME_MAX_FREQ = "cpu_prime_max_frequency";
    private static final String KEY_CPU_PRIME_GOVERNOR = "cpu_prime_governor";

    private static final String CPU_LITTLE_DEFAULT_MIN = "300000";
    private static final String CPU_LITTLE_DEFAULT_MAX = "1804800";
    private static final String CPU_LITTLE_DEFAULT_GOV = "walt";
    private static final String CPU_BIG_DEFAULT_MIN = "633600";
    private static final String CPU_BIG_DEFAULT_MAX = "2496000";
    private static final String CPU_BIG_DEFAULT_GOV = "walt";
    private static final String CPU_PRIME_DEFAULT_MIN = "787200";
    private static final String CPU_PRIME_DEFAULT_MAX = "2918400";
    private static final String CPU_PRIME_DEFAULT_GOV = "walt";

    // CPU Little Preferences
    private ListPreference mCpuLittleMinFreqPref;
    private ListPreference mCpuLittleMaxFreqPref;
    private ListPreference mCpuLittleGovernorPref;
    // CPU Big Preferences
    private ListPreference mCpuBigMinFreqPref;
    private ListPreference mCpuBigMaxFreqPref;
    private ListPreference mCpuBigGovernorPref;
    // CPU Prime Preferences
    private ListPreference mCpuPrimeMinFreqPref;
    private ListPreference mCpuPrimeMaxFreqPref;
    private ListPreference mCpuPrimeGovernorPref;

    private String[] mCpuLittleFreqEntries, mCpuLittleFreqValues;
    private String[] mCpuBigFreqEntries, mCpuBigFreqValues;
    private String[] mCpuPrimeFreqEntries, mCpuPrimeFreqValues;
    private String[] mCpuGovernorEntries, mCpuGovernorValues;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.powertools_settings, rootKey);

        mPowerProfilePreference = findPreference(KEY_POWER_PROFILE_MODE);
        mPowerProfileUtil = new PowerProfileUtil(requireContext());
        if (mPowerProfilePreference != null) {
            int currentMode = mPowerProfileUtil.getManagedMode();
            mPowerProfilePreference.setValue(String.valueOf(currentMode));
            mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            mPowerProfilePreference.setOnPreferenceChangeListener(this);
        }

        // GPU preferences
        mGpuEnablePref = findPreference(KEY_GPU_ENABLE);
        mGpuMinFreqPref = findPreference(KEY_GPU_MIN_FREQ);
        mGpuMaxFreqPref = findPreference(KEY_GPU_MAX_FREQ);
        mGpuFreqEntries = getResources().getStringArray(R.array.gpu_frequency_entries);
        mGpuFreqValues = getResources().getStringArray(R.array.gpu_frequency_values);
        if (mGpuEnablePref != null) {
            mGpuEnablePref.setOnPreferenceChangeListener(this);
        }
        if (mGpuMinFreqPref != null) {
            mGpuMinFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mGpuMaxFreqPref != null) {
            mGpuMaxFreqPref.setOnPreferenceChangeListener(this);
        }

        // CPU preferences
        SwitchPreferenceCompat cpuEnablePref = findPreference(KEY_CPU_ENABLE);
        if (cpuEnablePref != null) {
            cpuEnablePref.setOnPreferenceChangeListener(this);
        }

        mCpuLittleMinFreqPref = findPreference(KEY_CPU_LITTLE_MIN_FREQ);
        mCpuLittleMaxFreqPref = findPreference(KEY_CPU_LITTLE_MAX_FREQ);
        mCpuLittleGovernorPref = findPreference(KEY_CPU_LITTLE_GOVERNOR);
        if (mCpuLittleMinFreqPref != null) {
            mCpuLittleMinFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuLittleMaxFreqPref != null) {
            mCpuLittleMaxFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuLittleGovernorPref != null) {
            mCpuLittleGovernorPref.setOnPreferenceChangeListener(this);
        }

        mCpuBigMinFreqPref = findPreference(KEY_CPU_BIG_MIN_FREQ);
        mCpuBigMaxFreqPref = findPreference(KEY_CPU_BIG_MAX_FREQ);
        mCpuBigGovernorPref = findPreference(KEY_CPU_BIG_GOVERNOR);
        if (mCpuBigMinFreqPref != null) {
            mCpuBigMinFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuBigMaxFreqPref != null) {
            mCpuBigMaxFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuBigGovernorPref != null) {
            mCpuBigGovernorPref.setOnPreferenceChangeListener(this);
        }

        mCpuPrimeMinFreqPref = findPreference(KEY_CPU_PRIME_MIN_FREQ);
        mCpuPrimeMaxFreqPref = findPreference(KEY_CPU_PRIME_MAX_FREQ);
        mCpuPrimeGovernorPref = findPreference(KEY_CPU_PRIME_GOVERNOR);
        if (mCpuPrimeMinFreqPref != null) {
            mCpuPrimeMinFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuPrimeMaxFreqPref != null) {
            mCpuPrimeMaxFreqPref.setOnPreferenceChangeListener(this);
        }
        if (mCpuPrimeGovernorPref != null) {
            mCpuPrimeGovernorPref.setOnPreferenceChangeListener(this);
        }

        mCpuLittleFreqEntries = getResources().getStringArray(R.array.cpu_little_freq_entries);
        mCpuLittleFreqValues = getResources().getStringArray(R.array.cpu_little_freq_values);
        mCpuBigFreqEntries = getResources().getStringArray(R.array.cpu_big_freq_entries);
        mCpuBigFreqValues = getResources().getStringArray(R.array.cpu_big_freq_values);
        mCpuPrimeFreqEntries = getResources().getStringArray(R.array.cpu_prime_freq_entries);
        mCpuPrimeFreqValues = getResources().getStringArray(R.array.cpu_prime_freq_values);
        mCpuGovernorEntries = getResources().getStringArray(R.array.cpu_governor_entries);
        mCpuGovernorValues = getResources().getStringArray(R.array.cpu_governor_values);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateUI() {
        if (mPowerProfilePreference != null) {
            boolean masterEnabled = mPowerProfileUtil.isMasterEnabled();
            if (masterEnabled) {
                mPowerProfilePreference.setEnabled(false);
                mPowerProfilePreference.setSummary(getString(R.string.powerprofile_tile_disabled_subtitle));
            } else {
                mPowerProfilePreference.setEnabled(true);
                mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            }
        }
        // GPU section
        boolean gpuEnabled = (mGpuEnablePref != null && mGpuEnablePref.isChecked());
        if (mGpuMinFreqPref != null) {
            mGpuMinFreqPref.setEnabled(gpuEnabled);
        }
        if (mGpuMaxFreqPref != null) {
            mGpuMaxFreqPref.setEnabled(gpuEnabled);
        }

        // CPU section
        SwitchPreferenceCompat cpuEnablePref = findPreference(KEY_CPU_ENABLE);
        boolean cpuEnabled = (cpuEnablePref != null && cpuEnablePref.isChecked());
        if (findPreference(KEY_CPU_LITTLE_MIN_FREQ) != null) {
            findPreference(KEY_CPU_LITTLE_MIN_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_LITTLE_MAX_FREQ) != null) {
            findPreference(KEY_CPU_LITTLE_MAX_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_LITTLE_GOVERNOR) != null) {
            findPreference(KEY_CPU_LITTLE_GOVERNOR).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_BIG_MIN_FREQ) != null) {
            findPreference(KEY_CPU_BIG_MIN_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_BIG_MAX_FREQ) != null) {
            findPreference(KEY_CPU_BIG_MAX_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_BIG_GOVERNOR) != null) {
            findPreference(KEY_CPU_BIG_GOVERNOR).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_PRIME_MIN_FREQ) != null) {
            findPreference(KEY_CPU_PRIME_MIN_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_PRIME_MAX_FREQ) != null) {
            findPreference(KEY_CPU_PRIME_MAX_FREQ).setEnabled(cpuEnabled);
        }
        if (findPreference(KEY_CPU_PRIME_GOVERNOR) != null) {
            findPreference(KEY_CPU_PRIME_GOVERNOR).setEnabled(cpuEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Power Profile mode change
        if (KEY_POWER_PROFILE_MODE.equals(preference.getKey())) {
            int mode = Integer.parseInt((String) newValue);
            mPowerProfileUtil.setMode(mode);
            mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            Toast.makeText(getActivity(),
                    "Applied successfully power profile to " + mPowerProfileUtil.getModeLabel(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // GPU enable switch
        if (preference == mGpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                GPUUtils.setGPUMinFrequency(GPU_DEFAULT_MIN);
                GPUUtils.setGPUMaxFrequency(GPU_DEFAULT_MAX);
                Toast.makeText(getActivity(),
                        "GPU disabled, defaults restored",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(),
                        "GPU optimizer enabled",
                        Toast.LENGTH_SHORT).show();
            }
            if (mGpuMinFreqPref != null) mGpuMinFreqPref.setEnabled(enable);
            if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setEnabled(enable);
            return true;
        }

        // GPU min frequency change
        if (preference == mGpuMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mGpuMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: GPU min frequency cannot exceed max frequency",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMinFrequency(newMin);
            Toast.makeText(getActivity(),
                    "GPU min frequency -> " + formatFrequency(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // GPU max frequency change
        if (preference == mGpuMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mGpuMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: GPU max frequency cannot be lower than min frequency",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMaxFrequency(newMax);
            Toast.makeText(getActivity(),
                    "GPU max frequency -> " + formatFrequency(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU enable switch
        if (KEY_CPU_ENABLE.equals(preference.getKey())) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                CPUUtils.setCPULittleFreq(
                        CPU_LITTLE_DEFAULT_MIN, CPU_LITTLE_DEFAULT_MAX, CPU_LITTLE_DEFAULT_GOV);
                CPUUtils.setCPUBigFreq(
                        CPU_BIG_DEFAULT_MIN, CPU_BIG_DEFAULT_MAX, CPU_BIG_DEFAULT_GOV);
                CPUUtils.setCPUPrimeFreq(
                        CPU_PRIME_DEFAULT_MIN, CPU_PRIME_DEFAULT_MAX, CPU_PRIME_DEFAULT_GOV);
                Toast.makeText(getActivity(),
                        "CPU optimizer disabled, defaults restored",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(),
                        "CPU optimizer enabled",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        // CPU Little cluster changes
        if (preference == mCpuLittleMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuLittleMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU little min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(newMin, mCpuLittleMaxFreqPref.getValue(),
                    mCpuLittleGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU little min frequency -> " + formatFrequency(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuLittleMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuLittleMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU little max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(), newMax,
                    mCpuLittleGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU little max frequency -> " + formatFrequency(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuLittleGovernorPref) {
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(),
                    mCpuLittleMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU little governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU Big cluster changes
        if (preference == mCpuBigMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuBigMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU big min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(newMin, mCpuBigMaxFreqPref.getValue(),
                    mCpuBigGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU big min frequency -> " + formatFrequency(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuBigMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuBigMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU big max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(), newMax,
                    mCpuBigGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU big max frequency -> " + formatFrequency(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuBigGovernorPref) {
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(),
                    mCpuBigMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU big governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU Prime cluster changes
        if (preference == mCpuPrimeMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuPrimeMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU prime min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(newMin, mCpuPrimeMaxFreqPref.getValue(),
                    mCpuPrimeGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU prime min frequency -> " + formatFrequency(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuPrimeMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuPrimeMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU prime max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(), newMax,
                    mCpuPrimeGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU prime max frequency -> " + formatFrequency(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuPrimeGovernorPref) {
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(),
                    mCpuPrimeMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU prime governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        return true;
    }

    private String formatFrequency(long freqHz) {
        long mhz = freqHz / 1_000_000;
        return mhz + "MHz";
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateUI();
    }
}
