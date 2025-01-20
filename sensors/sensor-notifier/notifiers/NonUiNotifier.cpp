/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "NonUiNotifier"

#include "NonUiNotifier.h"

#include <android-base/logging.h>
#include <android-base/unique_fd.h>
#include <linux/xiaomi_touch.h>
#include <poll.h>
#include <sys/ioctl.h>

#include "SensorNotifierUtils.h"

static const std::string kTouchDevice = "/dev/xiaomi-touch";

using android::hardware::Return;
using android::hardware::Void;
using android::hardware::sensors::V1_0::Event;

namespace {

class NonUiSensorCallback : public IEventQueueCallback {
  public:
    NonUiSensorCallback() {
        touch_fd_ = android::base::unique_fd(open(kTouchDevice.c_str(), O_RDWR));
        if (touch_fd_.get() == -1) {
            LOG(ERROR) << "failed to open " << kTouchDevice;
        }
    }

    Return<void> onEvent(const Event& e) {
        int buf[MAX_BUF_SIZE] = {0, Touch_Nonui_Mode, static_cast<int>(e.u.scalar)};
        ioctl(touch_fd_.get(), TOUCH_IOC_SET_CUR_VALUE, &buf);

        return Void();
    }

  private:
    android::base::unique_fd touch_fd_;
};

}  // namespace

NonUiNotifier::NonUiNotifier(sp<ISensorManager> manager) : SensorNotifier(manager) {
    initializeSensorQueue("xiaomi.sensor.nonui", true, new NonUiSensorCallback());
}

NonUiNotifier::~NonUiNotifier() {
    deactivate();
}

void NonUiNotifier::notify() {
    Result res;

    // Enable states of touchscreen sensors
    const std::vector<const char*> paths = {
            "/sys/class/touch/touch_dev/fod_longpress_gesture_enabled",
            "/sys/class/touch/touch_dev/gesture_single_tap_enabled",
            "/sys/class/touch/touch_dev/gesture_double_tap_enabled"};

    pollfd* pollfds = new pollfd[paths.size()];
    for (size_t i = 0; i < paths.size(); ++i) {
        int fd = open(paths[i], O_RDONLY);
        if (fd < 0) {
            LOG(ERROR) << "failed to open " << paths[i] << " , err: " << fd;
            mActive = false;
            return;
        }

        pollfds[i].fd = fd;
        pollfds[i].events = POLLPRI;
    }

    while (mActive) {
        int rc = poll(pollfds, paths.size(), -1);
        if (rc < 0) {
            LOG(ERROR) << "failed to poll, err: " << rc;
            continue;
        }

        bool enabled = false;
        for (size_t i = 0; i < paths.size(); ++i) {
            enabled = enabled || readBool(pollfds[i].fd);
        }
        if (enabled) {
            res = mQueue->enableSensor(mSensorHandle, 20000 /* sample period */, 0 /* latency */);
            if (res != Result::OK) {
                LOG(ERROR) << "failed to enable sensor";
            }
        } else {
            res = mQueue->disableSensor(mSensorHandle);
            if (res != Result::OK) {
                LOG(DEBUG) << "failed to disable sensor";
            }
        }
    }
}
