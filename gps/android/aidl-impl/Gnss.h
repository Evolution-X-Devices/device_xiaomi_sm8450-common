/*
 * Copyright (c) 2021, The Linux Foundation. All rights reserved.
 * Not a Contribution
 */
/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2_0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2_0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_HARDWARE_GNSS_AIDL_GNSS_H
#define ANDROID_HARDWARE_GNSS_AIDL_GNSS_H


#include <aidl/android/hardware/gnss/BnGnss.h>
#include <aidl/android/hardware/gnss/IGnssCallback.h>
#include <aidl/android/hardware/gnss/BnGnssPowerIndication.h>
#include <aidl/android/hardware/gnss/BnGnssMeasurementInterface.h>
#include <aidl/android/hardware/gnss/BnGnssConfiguration.h>
#include <aidl/android/hardware/gnss/BnGnssPsds.h>
#include "location_api/GnssAPIClient.h"
#include "GnssConfiguration.h"
#include "GnssPowerIndication.h"
#include "GnssMeasurementInterface.h"

namespace android {
namespace hardware {
namespace gnss {
namespace aidl {
namespace implementation {

using ::aidl::android::hardware::gnss::GnssConstellationType;
using ::aidl::android::hardware::gnss::BnGnss;
using ::aidl::android::hardware::gnss::IGnssCallback;
using ::aidl::android::hardware::gnss::IGnssPowerIndication;
using ::aidl::android::hardware::gnss::IGnssMeasurementInterface;
using ::std::shared_ptr;

struct Gnss : public BnGnss {
    Gnss();
    ~Gnss();

    ndk::ScopedAStatus setCallback(const shared_ptr<IGnssCallback>& callback) override;
    ndk::ScopedAStatus close() override;
    ::ndk::ScopedAStatus getExtensionPsds(
            shared_ptr<::aidl::android::hardware::gnss::IGnssPsds>* _aidl_return __unused) override {
        return ndk::ScopedAStatus(AStatus_fromExceptionCode(EX_UNSUPPORTED_OPERATION));
    }
    ::ndk::ScopedAStatus getExtensionGnssConfiguration(
            shared_ptr<::aidl::android::hardware::gnss::IGnssConfiguration>* _aidl_return) override;
    ::ndk::ScopedAStatus getExtensionGnssPowerIndication(
            shared_ptr<IGnssPowerIndication>* _aidl_return) override;
    ::ndk::ScopedAStatus getExtensionGnssMeasurement(
            shared_ptr<IGnssMeasurementInterface>* _aidl_return) override;

    // These methods are not part of the IGnss base class.
    GnssAPIClient* getApi();
    ndk::ScopedAStatus updateConfiguration(GnssConfig& gnssConfig);
    void handleClientDeath();

private:
    GnssAPIClient* mApi;
    shared_ptr<GnssConfiguration> mGnssConfiguration = nullptr;
    shared_ptr<GnssPowerIndication> mGnssPowerIndication = nullptr;
    shared_ptr<GnssMeasurementInterface> mGnssMeasurementInterface = nullptr;
    GnssConfig mPendingConfig;


    shared_ptr<IGnssCallback> mGnssCallback = nullptr;
};

}  // namespace implementation
}  // namespace aidl
}  // namespace gnss
}  // namespace hardware
}  // namespace android

#endif  // ANDROID_HARDWARE_GNSS_AIDL_GNSS_H
