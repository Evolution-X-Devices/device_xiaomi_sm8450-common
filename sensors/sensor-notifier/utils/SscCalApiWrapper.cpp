/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "SscCalApiWrapper"

#include "SscCalApi.h"

#include <android-base/logging.h>
#include <dlfcn.h>
#include <cerrno>

SscCalApiWrapper::SscCalApiWrapper()
    : mSscCalApiHandle(nullptr), process_msg(nullptr), init_current_sensors(nullptr) {
    mSscCalApiHandle = dlopen("libssccalapi@2.0.so", RTLD_NOW);
    if (mSscCalApiHandle) {
        init_current_sensors =
                (init_current_sensors_t)dlsym(mSscCalApiHandle, "_Z20init_current_sensorsb");
        if (init_current_sensors == NULL) {
            LOG(ERROR) << "could not find init_current_sensors: " << dlerror();
        }

        process_msg = (process_msg_t)dlsym(mSscCalApiHandle, "_Z11process_msgP8_oem_msg");
        if (process_msg == NULL) {
            LOG(ERROR) << "could not find process_msg: " << dlerror();
        }
    } else {
        LOG(INFO) << "could not dlopen libssccalapi@2.0.so: " << dlerror();
    }
}

SscCalApiWrapper::~SscCalApiWrapper() {
    if (mSscCalApiHandle) dlclose(mSscCalApiHandle);
}

SscCalApiWrapper& SscCalApiWrapper::getInstance() {
    static SscCalApiWrapper instance;
    return instance;
}

void SscCalApiWrapper::initCurrentSensors(bool debug) {
    if (init_current_sensors != NULL) {
        init_current_sensors(debug);
    }
}

int SscCalApiWrapper::processMsg(_oem_msg* msg) {
    if (process_msg != NULL) {
        LOG(DEBUG) << "sending oem_msg for sensor " << msg->sensorType
                   << " with type: " << msg->notifyType << " and value: " << msg->value;
        return process_msg(msg);
    }
    return -ENODEV;
}
