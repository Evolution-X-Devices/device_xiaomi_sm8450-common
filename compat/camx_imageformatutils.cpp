/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <cstddef>
#include <cstdint>
#include <dlfcn.h>

// Xiaomi's camera HAL references this four-byte, zero-initialized legacy variable.
std::uint32_t jpeg_mult_override __asm__("_ZN4CamX16ImageFormatUtils18s_jpegMultOverrideE") = 0;

namespace {

// Xiaomi extends CamX::GrallocProperties with two stream-format overrides.
struct XiaomiGrallocProperties {
    std::uint32_t pixelFormat;
    std::uint32_t padding;
    std::uint64_t usage;
    std::uint32_t reserved0[3];
    std::uint32_t isInternal;
    std::uint32_t reserved1[4];
    std::uint32_t forceNV12;
    std::uint32_t forceNV21;
};

static_assert(offsetof(XiaomiGrallocProperties, isInternal) == 0x1c);
static_assert(offsetof(XiaomiGrallocProperties, forceNV12) == 0x30);
static_assert(offsetof(XiaomiGrallocProperties, forceNV21) == 0x34);

using GetFormatFn = int (*)(XiaomiGrallocProperties&, std::uint32_t&);

GetFormatFn realGetFormat() {
    static const GetFormatFn fn = [] {
        void* h = dlopen("libcamximageformatutils.so", RTLD_NOW | RTLD_NOLOAD);
        void* p = h ? dlsym(h,
            "_ZN4CamX16ImageFormatUtils9GetFormatERNS_17GrallocPropertiesERNS_6FormatE") : nullptr;
        return reinterpret_cast<GetFormatFn>(p);
    }();
    return fn;
}

}  // namespace

// Intercepts the original CamX GetFormat symbol without modifying camera.qcom.so.
extern "C" int shimGetFormat(XiaomiGrallocProperties&, std::uint32_t&)
        __asm__("_ZN4CamX16ImageFormatUtils9GetFormatERNS_17GrallocPropertiesERNS_6FormatE");

int shimGetFormat(XiaomiGrallocProperties& properties, std::uint32_t& format) {
    const auto real = realGetFormat();
    if (!real) {
        return -1;
    }
    const int result = real(properties, format);
    if (result != 0 || properties.isInternal != 1) {
        return result;
    }

    constexpr std::uint32_t implementationDefined = 0x22;
    constexpr std::uint32_t flexibleYuv420 = 0x23;
    constexpr std::uint32_t nv12 = 3;
    constexpr std::uint32_t nv21 = 4;

    // Mirrors the stock GetFormat: for internal flexible YUV420 streams the default is NV12,
    // forceNV12 wins, and NV21 is chosen by forceNV21 or when usage has all of bits 0, 1 and 17.
    constexpr std::uint64_t nv21UsageMask = 0x20003;

    if (properties.pixelFormat == flexibleYuv420) {
        if (properties.forceNV12 == 1) {
            format = nv12;
        } else if (properties.forceNV21 == 1 ||
                   (properties.usage & nv21UsageMask) == nv21UsageMask) {
            format = nv21;
        } else {
            format = nv12;
        }
    } else if (properties.pixelFormat == implementationDefined && properties.forceNV12 == 1) {
        format = nv12;
    }
    return result;
}
