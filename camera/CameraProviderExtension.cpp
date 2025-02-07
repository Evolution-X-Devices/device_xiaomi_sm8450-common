/*
 * Copyright (C) 2024 LibreMobileOS Foundation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "CameraProviderExtension.h"

#include <fstream>

#define TORCH_BRIGHTNESS "brightness"
#define TORCH_MAX_BRIGHTNESS "max_brightness"
#define TOGGLE_SWITCH "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-02/c42d000.qcom,spmi:qcom,pm8350c@2:qcom,flash_led@ee00/leds/led:switch_2/brightness"
static std::string kTorchLedPath = "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-02/c42d000.qcom,spmi:qcom,pm8350c@2:qcom,flash_led@ee00/leds/led:torch_0";

/**
 * Write value to path and close file.
 */
template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);
    file << value;
}

/**
 * Read value from the path and close file.
 */
template <typename T>
static T get(const std::string& path, const T& def) {
    std::ifstream file(path);
    T result;

    file >> result;
    return file.fail() ? def : result;
}

bool supportsTorchStrengthControlExt() {
    return true;
}

bool supportsSetTorchModeExt() {
    return false;
}

int32_t getTorchDefaultStrengthLevelExt() {
    // Our default value is 75. This corresponds to 15%.
    // As we have changed the maximum value, 59% now corresponds to 75.
    return 59;
}

int32_t getTorchMaxStrengthLevelExt() {
    // 255 out of 500 is a sane brightness.
    // Let's cap it to 255 as max, we can go much higher, but I don't want to test this.
    return 255;
}

int32_t getTorchStrengthLevelExt() {
    // We write same value in the both LEDs,
    // so get from one.
    auto node = kTorchLedPath + "/" + TORCH_BRIGHTNESS;
    return get(node, 0);
}

void setTorchStrengthLevelExt(int32_t torchStrength, bool enabled) {
    set(TOGGLE_SWITCH, 0);
    auto node = kTorchLedPath + "/" + TORCH_BRIGHTNESS;
    set(node, torchStrength);
    if (enabled)
        set(TOGGLE_SWITCH, 255);
}

void setTorchModeExt(bool enabled) {
    int32_t strength = getTorchDefaultStrengthLevelExt();
    setTorchStrengthLevelExt(enabled ? strength : 0, enabled);
}
