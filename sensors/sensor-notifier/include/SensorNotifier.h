/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <android/frameworks/sensorservice/1.0/ISensorManager.h>
#include <atomic>
#include <thread>

using android::sp;
using android::frameworks::sensorservice::V1_0::IEventQueue;
using android::frameworks::sensorservice::V1_0::IEventQueueCallback;
using android::frameworks::sensorservice::V1_0::ISensorManager;
using android::frameworks::sensorservice::V1_0::Result;

class SensorNotifier {
  public:
    SensorNotifier(sp<ISensorManager> manager);
    virtual ~SensorNotifier();

    void activate();
    void deactivate();

  protected:
    Result initializeSensorQueue(std::string typeAsString, bool wakeup, sp<IEventQueueCallback>);
    virtual void notify() = 0;

    sp<IEventQueue> mQueue;
    int32_t mSensorHandle = -1;
    std::atomic<bool> mActive{false};

  private:
    sp<ISensorManager> mManager;
    std::thread mThread;
};
