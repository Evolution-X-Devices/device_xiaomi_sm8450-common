/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <cstdint>

enum notify_t {
    BRIGHTNESS = 17,
    DC_STATE = 18,
    DISPLAY_FREQUENCY = 20,
    REPORT_VALUE = 201,
    POWER_STATE = 202,
};

struct _oem_msg {
    uint32_t sensorType;
    notify_t notifyType;
    float unknown1;
    float unknown2;
    float notifyTypeFloat;
    float value;
    float unused[64];
};

typedef void (*init_current_sensors_t)(bool debug);
typedef void (*process_msg_t)(_oem_msg* msg);

class SscCalApiWrapper {
  public:
    static SscCalApiWrapper& getInstance();

    void initCurrentSensors(bool debug);
    void processMsg(_oem_msg* msg);

  private:
    SscCalApiWrapper();
    ~SscCalApiWrapper();

    void* mSscCalApiHandle;
    process_msg_t process_msg;
    init_current_sensors_t init_current_sensors;
};
