//
// SPDX-FileCopyrightText: The LineageOS Project
// SPDX-License-Identifier: Apache-2.0
//

#include <codec2/hidl/1.2/ComponentStore.h>

// RefBase is inherited virtually and so sits at the end of the object, where
// any growth moves mRefs past the allocation the QTI c2 blobs make with this
// size baked in. Repatch the MOVZ immediate in extract-files.py to match.
static_assert(
        sizeof(android::hardware::media::c2::V1_2::utils::ComponentStore) == 272,
        "sizeof(ComponentStore) changed; repatch the QTI c2 service blobs");
