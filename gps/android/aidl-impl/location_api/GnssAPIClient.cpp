/* Copyright (c) 2017-2021, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation, nor the names of its
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
 *
 */

#define LOG_NDEBUG 0
#define LOG_TAG "LocSvc_GnssAPIClient"

#include <log_util.h>
#include <loc_cfg.h>

#include "GnssAPIClient.h"
#include <LocContext.h>

namespace android {
namespace hardware {
namespace gnss {
namespace aidl {
namespace implementation {

GnssAPIClient::GnssAPIClient(const shared_ptr<IGnssCallback>& gpsCb) :
    LocationAPIClientBase(),
    mTracking(false),
    mControlClient(new LocationAPIControlClient()),
    mLocationCapabilitiesMask(0),
    mLocationCapabilitiesCached(false),
    mGnssCbIface(gpsCb) {
    LOC_LOGD("%s]: (%p)", __FUNCTION__, &gpsCb);
    initLocationOptions();
}

GnssAPIClient::~GnssAPIClient() {
    LOC_LOGD("%s]: ()", __FUNCTION__);
    if (mControlClient) {
        delete mControlClient;
        mControlClient = nullptr;
    }
}

void GnssAPIClient::setCallbacks() {
    LocationCallbacks locationCallbacks;
    memset(&locationCallbacks, 0, sizeof(LocationCallbacks));
    locationCallbacks.size = sizeof(LocationCallbacks);
    if (mGnssCbIface != nullptr) {
        locationCallbacks.capabilitiesCb = [this](LocationCapabilitiesMask capabilitiesMask) {
            onCapabilitiesCb(capabilitiesMask);
        };
    }
    locAPISetCallbacks(locationCallbacks);
}

// for GpsInterface
void GnssAPIClient::gnssUpdateCallbacks(const shared_ptr<IGnssCallback>& gpsCb) {
    mMutex.lock();
    mGnssCbIface = gpsCb;
    mMutex.unlock();

    if (gpsCb != nullptr) {
        setCallbacks();
    }
}

void GnssAPIClient::initLocationOptions() {
    // set default LocationOptions.
    memset(&mTrackingOptions, 0, sizeof(TrackingOptions));
    mTrackingOptions.size = sizeof(TrackingOptions);
    mTrackingOptions.minInterval = 1000;
    mTrackingOptions.minDistance = 0;
    mTrackingOptions.mode = GNSS_SUPL_MODE_STANDALONE;
}

bool GnssAPIClient::gnssStart() {
    LOC_LOGD("%s]: ()", __FUNCTION__);
    mMutex.lock();
    mTracking = true;
    mMutex.unlock();
    locAPIStartTracking(mTrackingOptions);
    return true;
}

bool GnssAPIClient::gnssStop() {
    LOC_LOGD("%s]: ()", __FUNCTION__);
    mMutex.lock();
    mTracking = false;
    mMutex.unlock();
    locAPIStopTracking();
    return true;
}

void GnssAPIClient::requestCapabilities() {
    // only send capablities if it's already cached, otherwise the first time LocationAPI
    // is initialized, capabilities will be sent by LocationAPI
    if (mLocationCapabilitiesCached) {
        onCapabilitiesCb(mLocationCapabilitiesMask);
    }
}

void GnssAPIClient::gnssEnable(LocationTechnologyType techType) {
    LOC_LOGD("%s]: (%0d)", __FUNCTION__, techType);
    if (mControlClient == nullptr) {
        return;
    }
    mControlClient->locAPIEnable(techType);
}

void GnssAPIClient::gnssDisable() {
    LOC_LOGD("%s]: ()", __FUNCTION__);
    if (mControlClient == nullptr) {
        return;
    }
    mControlClient->locAPIDisable();
}

void GnssAPIClient::gnssConfigurationUpdate(const GnssConfig& gnssConfig) {
    LOC_LOGD("%s]: (%02x)", __FUNCTION__, gnssConfig.flags);
    if (mControlClient == nullptr) {
        return;
    }
    mControlClient->locAPIGnssUpdateConfig(gnssConfig);
}

// callbacks
void GnssAPIClient::onCapabilitiesCb(LocationCapabilitiesMask capabilitiesMask) {
    LOC_LOGD("%s]: (0x%" PRIx64 ")", __FUNCTION__, capabilitiesMask);
    mLocationCapabilitiesMask = capabilitiesMask;
    mLocationCapabilitiesCached = true;
    mMutex.lock();
    auto gnssCbIface(mGnssCbIface);
    mMutex.unlock();

    uint32_t capabilities = 0;
    if (capabilitiesMask & LOCATION_CAPABILITIES_CONSTELLATION_ENABLEMENT_BIT) {
        capabilities |= IGnssCallback::CAPABILITY_SATELLITE_BLOCKLIST;
    }
    // CORRELATION_VECTOR not supported.
    if (capabilitiesMask & LOCATION_CAPABILITIES_QWES_SV_POLYNOMIAL_BIT) {
        capabilities |= IGnssCallback::CAPABILITY_SATELLITE_PVT;
    }
    if (capabilitiesMask & LOCATION_CAPABILITIES_MEASUREMENTS_CORRECTION_BIT) {
        capabilities |= IGnssCallback::CAPABILITY_MEASUREMENT_CORRECTIONS_FOR_DRIVING;
    }

    if (gnssCbIface != nullptr) {
        auto r = gnssCbIface->gnssSetCapabilitiesCb(capabilities);
        if (!r.isOk()) {
            LOC_LOGe("Error from AIDL gnssSetCapabilitiesCb");
        }
    }
}

}  // namespace implementation
}  // namespace aidl
}  // namespace gnss
}  // namespace hardware
}  // namespace android
