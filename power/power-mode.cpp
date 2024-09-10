/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <android-base/file.h>
#include <android-base/logging.h>
#include <sys/ioctl.h>
#include <sys/stat.h>

#define SET_CUR_VALUE 0
#define TOUCH_DOUBLETAP_MODE 14
#define TOUCH_MAGIC 't'
#define TOUCH_IOC_SETMODE _IO(TOUCH_MAGIC, SET_CUR_VALUE)
#define TOUCH_DEV_PATH "/dev/xiaomi-touch"
#define TOUCH_ID_PRIMARY 0
#define TOUCH_ID_SECONDARY 1
#define MI_DISP_SECONDARY "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-1"

namespace aidl {
namespace android {
namespace hardware {
namespace power {
namespace impl {

using ::aidl::android::hardware::power::Mode;

bool isDeviceSpecificModeSupported(Mode type, bool* _aidl_return) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE:
            *_aidl_return = true;
            return true;
        default:
            return false;
    }
}

bool setDeviceSpecificMode(Mode type, bool enabled) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE: {
            int fd = open(TOUCH_DEV_PATH, O_RDWR);
            int arg_primary[3] = {TOUCH_ID_PRIMARY, TOUCH_DOUBLETAP_MODE, enabled ? 1 : 0};
            ioctl(fd, TOUCH_IOC_SETMODE, &arg_primary);

            struct stat buffer;
            if (stat(MI_DISP_SECONDARY, &buffer) == 0) {
                int arg_secondary[3] = {TOUCH_ID_SECONDARY, TOUCH_DOUBLETAP_MODE, enabled ? 1 : 0};
                ioctl(fd, TOUCH_IOC_SETMODE, &arg_secondary);
            }

            close(fd);
            return true;
        }
        default:
            return false;
    }
}

}  // namespace impl
}  // namespace power
}  // namespace hardware
}  // namespace android
}  // namespace aidl
