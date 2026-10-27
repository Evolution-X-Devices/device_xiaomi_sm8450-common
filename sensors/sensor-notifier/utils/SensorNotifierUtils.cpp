/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "SensorNotifierUtils"

#include "SensorNotifierUtils.h"

#include <android-base/logging.h>
#include <unistd.h>

#include <cstdlib>

bool readBool(int fd) {
    char c;
    int rc;

    rc = lseek(fd, 0, SEEK_SET);
    if (rc) {
        LOG(ERROR) << "failed to seek fd, err: " << rc;
        return false;
    }

    rc = read(fd, &c, sizeof(char));
    if (rc != 1) {
        LOG(ERROR) << "failed to read bool from fd, err: " << rc;
        return false;
    }

    return c != '0';
}

bool readInt(int fd, int* value) {
    char buf[16];

    if (lseek(fd, 0, SEEK_SET) < 0) {
        PLOG(ERROR) << "failed to seek fd";
        return false;
    }

    ssize_t rc = read(fd, buf, sizeof(buf) - 1);
    if (rc <= 0) {
        PLOG(ERROR) << "failed to read int from fd";
        return false;
    }
    buf[rc] = '\0';

    *value = atoi(buf);
    return true;
}
