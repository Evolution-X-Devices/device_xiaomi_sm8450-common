/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "LightNotifier"

#include "LightNotifier.h"

#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-base/unique_fd.h>
#include <fcntl.h>
#include <poll.h>

#include "SensorNotifierUtils.h"
#include "SscCalApi.h"

static const std::string kDisplayPrimary = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary";
static const std::string kDisplaySecondary =
        "/sys/devices/platform/soc/soc:qcom,dsi-display-secondary";

static const std::vector<std::pair<std::string, notify_t>> kDisplayNodes = {
        {"panel_power_state", POWER_STATE},
        {"dynamic_fps", DISPLAY_FREQUENCY},
        {"dbv", BRIGHTNESS},
};

LightNotifier::LightNotifier(sp<ISensorManager> manager) : SensorNotifier(manager) {
    std::stringstream lightSensorsPrimary(
            android::base::GetProperty("ro.vendor.sensors.notifier.light_sensors.primary", ""));
    std::stringstream lightSensorsSecondary(
            android::base::GetProperty("ro.vendor.sensors.notifier.light_sensors.secondary", ""));

    std::string sensor;
    while (std::getline(lightSensorsPrimary, sensor, ',')) {
        mLightSensorsPrimary.push_back(std::stoi(sensor));
    }
    while (std::getline(lightSensorsSecondary, sensor, ',')) {
        mLightSensorsSecondary.push_back(std::stoi(sensor));
    }
}

LightNotifier::~LightNotifier() {
    deactivate();
}

void LightNotifier::notify() {
    if (mLightSensorsPrimary.empty() && mLightSensorsSecondary.empty()) {
        LOG(DEBUG) << "no light sensors to notify defined, skip light notifications";
        mActive = false;
        return;
    }

    std::vector<std::pair<std::string, std::vector<int>*>> displays;
    if (!mLightSensorsPrimary.empty()) {
        displays.emplace_back(kDisplayPrimary, &mLightSensorsPrimary);
    }
    if (!mLightSensorsSecondary.empty()) {
        displays.emplace_back(kDisplaySecondary, &mLightSensorsSecondary);
    }

    std::vector<android::base::unique_fd> fds;
    std::vector<struct pollfd> pollfds;
    std::vector<std::pair<notify_t, std::vector<int>*>> targets;
    for (const auto& [path, sensors] : displays) {
        for (const auto& [node, notifyType] : kDisplayNodes) {
            std::string nodePath = path + "/" + node;
            android::base::unique_fd fd(open(nodePath.c_str(), O_RDONLY));
            if (fd.get() == -1) {
                PLOG(ERROR) << "failed to open " << nodePath;
                continue;
            }

            pollfds.push_back({.fd = fd.get(), .events = POLLERR | POLLPRI, .revents = 0});
            targets.emplace_back(notifyType, sensors);
            fds.push_back(std::move(fd));
        }
    }

    if (pollfds.empty()) {
        mActive = false;
        return;
    }

    _oem_msg msg;

    // Read once so that the next poll only returns on changes
    for (size_t i = 0; i < pollfds.size(); i++) {
        pollfds[i].revents = POLLPRI;
    }

    while (mActive) {
        for (size_t i = 0; i < pollfds.size(); i++) {
            if (!(pollfds[i].revents & (POLLERR | POLLPRI))) {
                continue;
            }

            int value;
            if (!readInt(pollfds[i].fd, &value)) {
                continue;
            }

            for (const auto sensorId : *targets[i].second) {
                msg.sensorType = sensorId;
                msg.notifyType = targets[i].first;
                msg.notifyTypeFloat = targets[i].first;
                msg.value = value;
                msg.unknown1 = 1;
                msg.unknown2 = 5;

                SscCalApiWrapper::getInstance().processMsg(&msg);
            }
        }

        int rc = poll(pollfds.data(), pollfds.size(), -1);
        if (rc < 0) {
            PLOG(ERROR) << "failed to poll display nodes";
        }
    }
}
