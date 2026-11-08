/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <cstddef>
#include <cstdint>

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

}  // namespace

extern "C" int getFormat(const XiaomiGrallocProperties&, std::uint32_t&)
        __asm__("_ZN4CamX16ImageFormatUtils9GetFormatERNS_17GrallocPropertiesERNS_6FormatE");

// Only camera.qcom.so's import is redirected; newer CamX callers use the original ABI.
extern "C" int getXiaomiFormat(const XiaomiGrallocProperties&, std::uint32_t&)
        __asm__("_ZN4CamX16ImageFormatUtils9GetXiaomiERNS_17GrallocPropertiesERNS_6FormatE");

int getXiaomiFormat(const XiaomiGrallocProperties& properties, std::uint32_t& format) {
    const int result = getFormat(properties, format);
    if (result != 0 || properties.isInternal != 1) {
        return result;
    }

    constexpr std::uint32_t implementationDefined = 0x22;
    constexpr std::uint32_t flexibleYuv420 = 0x23;
    constexpr std::uint32_t nv12 = 3;
    constexpr std::uint32_t nv21 = 4;

    if (properties.forceNV12 == 1 &&
        (properties.pixelFormat == implementationDefined ||
         properties.pixelFormat == flexibleYuv420)) {
        format = nv12;
    } else if (properties.forceNV21 == 1 && properties.pixelFormat == flexibleYuv420) {
        format = nv21;
    }
    return result;
}
