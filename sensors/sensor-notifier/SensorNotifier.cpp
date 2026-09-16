/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "SensorNotifier"

#include "SensorNotifier.h"

#include <android-base/logging.h>

using android::hardware::sensors::V1_0::SensorFlagBits;
using android::hardware::sensors::V1_0::SensorInfo;

SensorNotifier::SensorNotifier(sp<ISensorManager> manager) : mManager(manager) {}

SensorNotifier::~SensorNotifier() {
    if (mQueue != nullptr) {
        /*
         * Free the event queue.
         * kernel calls decStrong() on server side implementation of IEventQueue,
         * hence resources (including the callback) are freed as well.
         */
        mQueue = nullptr;
    }
}

Result SensorNotifier::initializeSensorQueue(std::string typeAsString, bool wakeup,
                                             sp<IEventQueueCallback> callback) {
    Result res;
    std::vector<SensorInfo> sensorList;

    mManager->getSensorList([&sensorList, &res](const auto& l, auto r) {
        sensorList = l;
        res = r;
    });
    if (res != Result::OK) {
        LOG(ERROR) << "failed to get sensors list";
        return res;
    }
    auto it = std::find_if(sensorList.begin(), sensorList.end(),
                           [this, &typeAsString, &wakeup](const SensorInfo& sensor) {
                               return (sensor.typeAsString == typeAsString) &&
                                      ((sensor.flags & SensorFlagBits::WAKE_UP) == wakeup);
                           });

    if (it != sensorList.end()) {
        mSensorHandle = it->sensorHandle;
    } else {
        LOG(ERROR) << "failed to get " << typeAsString << " sensor with wake-up: " << wakeup;
        return Result::NOT_EXIST;
    }

    mManager->createEventQueue(callback, [this, &res](const auto& q, auto r) {
        this->mQueue = q;
        res = r;
    });
    if (res != Result::OK) {
        LOG(ERROR) << "failed to create event queue";
        return res;
    }

    return Result::OK;
}

void SensorNotifier::activate() {
    if (mActive) {
        return;
    }
    mActive = true;
    mThread = std::thread(&SensorNotifier::notify, this);
}

void SensorNotifier::deactivate() {
    if (!mActive) {
        return;
    }
    mActive = false;
    if (mThread.joinable()) {
        mThread.join();
    }
    if (mQueue != nullptr) {
        mQueue->disableSensor(mSensorHandle);
    }
}
