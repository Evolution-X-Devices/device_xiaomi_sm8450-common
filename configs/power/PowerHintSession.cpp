/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

#include "PowerHintSession.h"
#include <android-base/logging.h>
#define LOG_TAG "QTI PowerHAL"

std::shared_ptr<aidl::android::hardware::power::IPowerHintSession> setPowerHintSession() {
    std::shared_ptr<aidl::android::hardware::power::IPowerHintSession> mPowerSession =
            ndk::SharedRefBase::make<PowerHintSessionImpl>();
    return mPowerSession;
}

int64_t getSessionPreferredRate() {
    return 16666666L;
}

ndk::ScopedAStatus PowerHintSessionImpl::updateTargetWorkDuration(int64_t in_targetDurationNanos) {
    return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus PowerHintSessionImpl::reportActualWorkDuration(
        const std::vector<::aidl::android::hardware::power::WorkDuration>& in_durations) {
    return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus PowerHintSessionImpl::pause() {
    return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus PowerHintSessionImpl::resume() {
    return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus PowerHintSessionImpl::close() {
    return ndk::ScopedAStatus::ok();
}
