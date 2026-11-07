/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <cstdint>

// Xiaomi's camera HAL references this four-byte, zero-initialized legacy variable.
std::uint32_t jpeg_mult_override __asm__("_ZN4CamX16ImageFormatUtils18s_jpegMultOverrideE") = 0;
