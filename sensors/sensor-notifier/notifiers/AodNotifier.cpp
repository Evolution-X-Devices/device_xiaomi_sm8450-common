/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "AodNotifier"

#include "AodNotifier.h"

#include <android-base/logging.h>
#include <android-base/unique_fd.h>
#include <display/drm/mi_disp.h>
#include <poll.h>
#include <sys/ioctl.h>

#include "SensorNotifierUtils.h"

static const std::string kDispFeatureDevice = "/dev/mi_display/disp_feature";

using android::hardware::Return;
using android::hardware::Void;
using android::hardware::sensors::V1_0::Event;

namespace {

void requestDozeBrightness(int fd, __u32 doze_brightness, __u32 disp_id) {
    disp_doze_brightness_req req;
    req.base.flag = 0;
    req.base.disp_id = disp_id;
    req.doze_brightness = doze_brightness;
    ioctl(fd, MI_DISP_IOCTL_SET_DOZE_BRIGHTNESS, &req);
}

class AodSensorCallback : public IEventQueueCallback {
  public:
    AodSensorCallback() {
        disp_fd_ = android::base::unique_fd(open(kDispFeatureDevice.c_str(), O_RDWR));
        if (disp_fd_.get() == -1) {
            LOG(ERROR) << "failed to open " << kDispFeatureDevice;
        }
    }

    Return<void> onEvent(const Event& e) {
        for (auto& display : AodNotifier::activeDisplays) {
            requestDozeBrightness(disp_fd_.get(),
                                  (e.u.scalar == 3 || e.u.scalar == 5) ? DOZE_BRIGHTNESS_LBM
                                                                       : DOZE_BRIGHTNESS_HBM,
                                  display);
        }
        return Void();
    }

  private:
    android::base::unique_fd disp_fd_;
};

}  // namespace

AodNotifier::AodNotifier(sp<ISensorManager> manager) : SensorNotifier(manager) {
    initializeSensorQueue("xiaomi.sensor.aod", true, new AodSensorCallback());
}

AodNotifier::~AodNotifier() {
    deactivate();
}

void AodNotifier::notify() {

    android::base::unique_fd disp_fd_ =
            android::base::unique_fd(open(kDispFeatureDevice.c_str(), O_RDWR));
    if (disp_fd_.get() == -1) {
        LOG(ERROR) << "failed to open " << kDispFeatureDevice;
    }

    const std::vector<disp_display_type> displays = {MI_DISP_PRIMARY, MI_DISP_SECONDARY};

    // Register for power events
    for (const disp_display_type& display : displays) {
        disp_event_req req;
        req.base.flag = 0;
        req.base.disp_id = display;
        req.type = MI_DISP_EVENT_POWER;
        ioctl(disp_fd_.get(), MI_DISP_IOCTL_REGISTER_EVENT, &req);
    }

    struct pollfd dispEventPoll = {
            .fd = disp_fd_.get(),
            .events = POLLIN,
            .revents = 0,
    };

    while (mActive) {
        int rc = poll(&dispEventPoll, 1, -1);
        if (rc < 0) {
            LOG(ERROR) << "failed to poll " << kDispFeatureDevice << ", err: " << rc;
            continue;
        }

        std::shared_ptr<disp_event_resp> response = parseDispEvent(disp_fd_.get());
        if (response == nullptr) {
            continue;
        }

        if (response->base.type != MI_DISP_EVENT_POWER) {
            LOG(ERROR) << "unexpected display event: " << response->base.type;
            continue;
        }

        int value = response->data[0];
        LOG(VERBOSE) << "received data: " << std::bitset<8>(value);

        switch (response->data[0]) {
            case MI_DISP_POWER_LP1:
                FALLTHROUGH_INTENDED;
            case MI_DISP_POWER_LP2:
                activeDisplays.insert(response->base.disp_id);
                if (!mQueue->enableSensor(mSensorHandle, 20000 /* sample period */,
                    0 /* latency */).isOk()) {
                    LOG(ERROR) << "failed to enable sensor";
                }
                break;
            case MI_DISP_POWER_ON:
                requestDozeBrightness(disp_fd_.get(), DOZE_TO_NORMAL, response->base.disp_id);
                FALLTHROUGH_INTENDED;
            default:
                activeDisplays.erase(response->base.disp_id);
                if (activeDisplays.empty()) {
                    if (!mQueue->disableSensor(mSensorHandle).isOk()) {
                        LOG(DEBUG) << "failed to disable sensor";
                    }
                }
                break;
        }
    }
}
