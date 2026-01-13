/*
 * Copyright (c) 2012-2019, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * *    * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//#define LOG_NDEBUG 0

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <sys/eventfd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <pthread.h>

#define LOG_TAG "QTI PowerHAL"
#include <hardware/hardware.h>
#include <hardware/power.h>
#include <log/log.h>

#include "hint-data.h"
#include "performance.h"
#include "power-common.h"
#include "utils.h"

#define MAX_LENGTH 64

static struct hint_handles handles[NUM_HINTS];
static int handleER = 0;

static const char* fb_idle_paths[] = {"/sys/class/drm/card0/device/idle_state",
                                      "/sys/class/graphics/fb0/idle_state"};

static pthread_t tid;
static pthread_once_t once = PTHREAD_ONCE_INIT;
static pthread_cond_t interaction_cond = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t interaction_lock = PTHREAD_MUTEX_INITIALIZER;

static enum INTERACTION_STATE mState = INTERACTION_STATE_UNINITIALIZED;

static int mIdleFd = 0;
static int mEventFd = 0;

const int kWaitDuration = 100;            /* ms */
const int kMaxLaunchDuration = 5000;      /* ms */
const int kMaxInteractiveDuration = 5000; /* ms */
const int kMinInteractiveDuration = 1000;  /* ms */

static struct timespec s_previous_boost_timespec;
static int s_previous_duration = 0;
static int prev_interaction_handle = -1;

int process_boost(int hint_id, int duration, int type) {
    ALOGV("%s: acquiring perf lock", __func__);
    int boost_handle = perf_hint_enable_with_type(hint_id, duration, type);
    if (!CHECK_HANDLE(boost_handle)) {
        ALOGE("Failed process_boost for boost_handle");
    }
    return boost_handle;
}

bool release_boost(int boost_handle) {
    ALOGV("%s: releasing perf lock %i", __func__, boost_handle);
    if (CHECK_HANDLE(boost_handle)) {
        release_request(boost_handle);
        return true;
    }
    return false;
}

int fb_idle_open(void) {
    int fd;
    int n = sizeof(fb_idle_paths) / sizeof(fb_idle_paths[0]);
    for (int i = 0; i < n; i++) {
        const char* path = fb_idle_paths[i];
        fd = open(path, O_RDONLY);
        if (fd >= 0)
            return fd;
    }
    ALOGE("Unable to open fb idle state path (%d)", errno);
    return -1;
}

void release() {
    pthread_mutex_lock(&interaction_lock);
    if (mState == INTERACTION_STATE_WAITING) {
        if (release_boost(prev_interaction_handle)) {
            prev_interaction_handle = -1;
        }
        mState = INTERACTION_STATE_IDLE;
    } else {
        // clear any wait aborts pending in event fd
        uint64_t val;
        ssize_t ret = read(mEventFd, &val, sizeof(val));

        ALOGW_IF(ret < 0, "%s: failed to clear eventfd (%zd, %d)",
                 __func__, ret, errno);
    }
    pthread_mutex_unlock(&interaction_lock);
}

void abortWaitLocked() {
    uint64_t val = 1;
    ssize_t ret = write(mEventFd, &val, sizeof(val));
    if (ret != sizeof(val))
        ALOGW("Unable to write to event fd (%zd)", ret);
}

void waitForIdle(int32_t wait_ms, int32_t timeout_ms) {
    char data[MAX_LENGTH];
    ssize_t ret;
    struct pollfd pfd[2];

    ALOGV("%s: wait:%d timeout:%d", __func__, wait_ms, timeout_ms);

    pfd[0].fd = mEventFd;
    pfd[0].events = POLLIN;
    pfd[1].fd = mIdleFd;
    pfd[1].events = POLLPRI | POLLERR;

    ret = poll(pfd, 1, wait_ms);
    if (ret > 0) {
        ALOGV("%s: wait aborted", __func__);
        return;
    } else if (ret < 0) {
        ALOGE("%s: error in poll while waiting", __func__);
        return;
    }

    ret = pread(mIdleFd, data, sizeof(data), 0);
    if (!ret) {
        ALOGE("%s: Unexpected EOF!", __func__);
        return;
    }

    if (!strncmp(data, "idle", 4)) {
        ALOGV("%s: already idle", __func__);
        return;
    }

    ret = poll(pfd, 2, timeout_ms);
    if (ret < 0)
        ALOGE("%s: Error on waiting for idle (%zd)", __func__, ret);
    else if (ret == 0)
        ALOGV("%s: timed out waiting for idle", __func__);
    else if (pfd[0].revents)
        ALOGV("%s: wait for idle aborted", __func__);
    else if (pfd[1].revents)
        ALOGV("%s: idle detected", __func__);
}

void* interaction_routine(void *vargp) {
    while (true) {
        pthread_mutex_lock(&interaction_lock);

        while (mState == INTERACTION_STATE_IDLE)
            pthread_cond_wait(&interaction_cond, &interaction_lock);

        if (mState == INTERACTION_STATE_UNINITIALIZED) {
            pthread_mutex_unlock(&interaction_lock);
            return NULL;
        }

        mState = INTERACTION_STATE_WAITING;
        pthread_mutex_unlock(&interaction_lock);

        waitForIdle(kWaitDuration, s_previous_duration);
        release();
    }
    return NULL;
}

static void create_once(void) {
    pthread_create(&tid, NULL, interaction_routine, NULL);
}

void power_init() {
    ALOGI("Initing");

    for (int i = 0; i < NUM_HINTS; i++) {
        handles[i].handle = 0;
        handles[i].ref_count = 0;
    }

    pthread_mutex_lock(&interaction_lock);
    if (mState != INTERACTION_STATE_UNINITIALIZED) {
        pthread_mutex_unlock(&interaction_lock);
        return;
    }

    int fd = fb_idle_open();
    if (fd < 0) {
        pthread_mutex_unlock(&interaction_lock);
        return;
    }
    mIdleFd = fd;

    mEventFd = eventfd(0, EFD_NONBLOCK);
    if (mEventFd < 0) {
        ALOGE("Unable to create event fd (%d)", errno);
        close(mIdleFd);
        pthread_mutex_unlock(&interaction_lock);
        return;
    }

    mState = INTERACTION_STATE_IDLE;
    pthread_once(&once, create_once);
    pthread_mutex_unlock(&interaction_lock);
}

void process_interaction_hint(void* data) {
    struct timespec cur_boost_timespec;
    long long elapsed_time;
    int duration = kMinInteractiveDuration;

    pthread_mutex_lock(&interaction_lock);

    if (data) {
        int input_duration = *((int*)data);
        if (input_duration > duration) {
            duration = (input_duration > kMaxInteractiveDuration) ? kMaxInteractiveDuration
                                                                  : input_duration;
        }
    }

    clock_gettime(CLOCK_MONOTONIC, &cur_boost_timespec);

    elapsed_time = calc_timespan_us(s_previous_boost_timespec, cur_boost_timespec);
    if (mState == INTERACTION_STATE_UNINITIALIZED) {
        // don't hint if it's been less than 250ms since last boost
        // also detect if we're doing anything resembling a fling
        // support additional boosting in case of flings
        if (elapsed_time < 250000 && duration <= kMinInteractiveDuration) {
            pthread_mutex_unlock(&interaction_lock);
            return;
        }
    }

    if (mState != INTERACTION_STATE_IDLE && duration <= s_previous_duration) {
        // don't hint if previous hint's duration covers this hint's duration
        if (elapsed_time <= (s_previous_duration - duration) * 1000) {
            ALOGV("%s: Previous duration (%d) cover this (%d) elapsed: %lld",
                  __func__, s_previous_duration, duration, elapsed_time);
            pthread_mutex_unlock(&interaction_lock);
            return;
        }
    }

    s_previous_boost_timespec = cur_boost_timespec;
    s_previous_duration = duration;

    if (mState == INTERACTION_STATE_UNINITIALIZED) {
        int interaction_handle =
                process_boost(VENDOR_HINT_SCROLL_BOOST, duration, SCROLL_VERTICAL);

        release_boost(prev_interaction_handle);
        prev_interaction_handle = interaction_handle;
        pthread_mutex_unlock(&interaction_lock);
        return;
    }

    if (mState == INTERACTION_STATE_WAITING)
        abortWaitLocked();
    else if (mState == INTERACTION_STATE_IDLE)
        prev_interaction_handle =
                process_boost(VENDOR_HINT_SCROLL_BOOST, INT_MAX, SCROLL_VERTICAL);

    mState = INTERACTION_STATE_INTERACTION;
    pthread_cond_signal(&interaction_cond);
    pthread_mutex_unlock(&interaction_lock);
}

void process_activity_launch_hint(void* data) {
    static int launch_handle = -1;
    static int launch_mode = 0;

    // release lock early if launch has finished
    if (!data) {
        if (release_boost(launch_handle)) {
            launch_handle = -1;
        }
        launch_mode = 0;
        return;
    }

    if (!launch_mode) {
        launch_handle = process_boost(VENDOR_HINT_FIRST_LAUNCH_BOOST,
                                                   kMaxLaunchDuration, LAUNCH_BOOST_V1);
        if (!CHECK_HANDLE(launch_handle)) {
            ALOGE("Failed to perform launch boost");
            return;
        }
        launch_mode = 1;
    }
}

int __attribute__((weak)) power_hint_override(power_hint_t hint, void* data) {
    return HINT_NONE;
}

void power_hint(power_hint_t hint, void* data) {
    /* Check if this hint has been overridden. */
    if (power_hint_override(hint, data) == HINT_HANDLED) {
        /* The power_hint has been handled. We can skip the rest. */
        return;
    }
    switch (hint) {
        case POWER_HINT_VR_MODE:
            ALOGI("VR mode power hint not handled in power_hint_override");
            break;
        // fall through below, hints will fail if not defined in powerhint.xml
        case POWER_HINT_SUSTAINED_PERFORMANCE:
        case POWER_HINT_VIDEO_ENCODE:
            if (data) {
                if (handles[hint].ref_count == 0)
                    handles[hint].handle = perf_hint_enable((AOSP_DELTA + hint), 0);

                if (handles[hint].handle > 0) handles[hint].ref_count++;
            } else {
                if (handles[hint].handle > 0) {
                    if (--handles[hint].ref_count == 0) {
                        release_request(handles[hint].handle);
                        handles[hint].handle = 0;
                    }
                } else {
                    ALOGE("Lock for hint: %X was not acquired, cannot be released", hint);
                }
            }
            break;
        case POWER_HINT_INTERACTION:
            process_interaction_hint(data);
            break;
        case POWER_HINT_LAUNCH:
            process_activity_launch_hint(data);
            break;
        default:
            break;
    }
}

bool is_expensive_rendering_supported() {
    char property[PROPERTY_VALUE_MAX];
    strlcpy(property, perf_get_property("vendor.perf.expensive_rendering", "0").value,
            PROPERTY_VALUE_MAX);
    return atoi(property) == 1 ? true : false;
}

void set_expensive_rendering(bool enabled) {
    if (enabled) {
        handleER = perf_hint_enable(PERF_HINT_EXPENSIVE_RENDERING, 0);
    } else if (handleER > 0) {
        release_request(handleER);
    }
}

int __attribute__((weak)) set_interactive_override(int on) {
    return HINT_HANDLED;
}

#ifdef SET_INTERACTIVE_EXT
extern void power_set_interactive_ext(int on);
#endif

void set_interactive(int on) {
    static int display_hint_sent;

    if (!on) {
        /* Send Display OFF hint to perf HAL */
        perf_hint_enable(VENDOR_HINT_DISPLAY_OFF, 0);
    } else {
        /* Send Display ON hint to perf HAL */
        perf_hint_enable(VENDOR_HINT_DISPLAY_ON, 0);
    }

    /**
     * Ignore consecutive display-off hints
     * Consecutive display-on hints are already handled
     */
    if (display_hint_sent && !on) return;

    display_hint_sent = !on;

#ifdef SET_INTERACTIVE_EXT
    power_set_interactive_ext(on);
#endif

    if (set_interactive_override(on) == HINT_HANDLED) {
        return;
    } else {
        ALOGI("Hint not handled in set_interactive_override");
    }
}
