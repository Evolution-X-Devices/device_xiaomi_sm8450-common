#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/sm8450-common',
    'hardware/qcom-caf/sm8450-6.6',
    'hardware/qcom-caf/wlan',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/dataservices',
]


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'vendor.qti.hardware.dpmservice@1.0',
        'vendor.qti.hardware.dpmservice@1.1',
        'vendor.qti.hardware.qccsyshal@1.0',
        'vendor.qti.hardware.qccsyshal@1.1',
        'vendor.qti.hardware.qccvndhal@1.0',
        'vendor.qti.imsrtpservice@3.0',
        'vendor.qti.diaghal@1.0',
        'vendor.qti.hardware.wifidisplaysession_aidl-V1-ndk',
        'com.qualcomm.qti.dpm.api@1.0',
    ): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
    'system/framework/WfdCommon.jar': blob_fixup()
        .apktool_patch('blob-patches/WfdCommon.patch'),
    'system_ext/etc/seccomp_policy/tcmd.policy': blob_fixup()
        .add_line_if_missing('lseek: 1'),
    'system_ext/lib64/libwfdservice.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V4-cpp.so', 'android.media.audio.common.types-V5-cpp.so'),
    'system_ext/lib64/vendor.qti.hardware.qccsyshal@1.2-halimpl.so': blob_fixup()
        .replace_needed('libprotobuf-cpp-full.so', 'libprotobuf-cpp-full-21.7.so'),
    (
        'vendor/bin/hw/android.hardware.security.keymint-service-qti',
        'vendor/lib64/libqtikeymint.so',
    ): blob_fixup()
        .replace_needed(
            'android.hardware.security.keymint-V1-ndk_platform.so',
            'android.hardware.security.keymint-V1-ndk.so',
        )
        .replace_needed(
            'android.hardware.security.secureclock-V1-ndk_platform.so',
            'android.hardware.security.secureclock-V1-ndk.so',
        )
        .replace_needed(
            'android.hardware.security.sharedsecret-V1-ndk_platform.so',
            'android.hardware.security.sharedsecret-V1-ndk.so',
        )
        .add_needed('android.hardware.security.rkp-V1-ndk.so'),
    (
        'vendor/bin/hw/vendor.qti.media.c2@1.0-service',
        'vendor/bin/hw/vendor.qti.media.c2audio@1.0-service',
    ): blob_fixup()
        .sig_replace(
            '00 2a 80 52',
            '00 2c 80 52',
        ),
    'vendor/bin/qcc-trd': blob_fixup()
        .replace_needed(
            'libgrpc++_unsecure.so', 'libgrpc++_unsecure_prebuilt.so'
        ),
    'vendor/etc/display/DPU8__.xml': blob_fixup()
        .regex_replace('.*<Param62>.*\n', '')
        .regex_replace('Param63', 'Param70')
        .regex_replace('Param64', 'Param71')
        .regex_replace('Param65', 'Param72')
        .regex_replace('Param66', 'Param73'),
    (
       'vendor/etc/media_codecs_cape.xml',
       'vendor/etc/media_codecs_diwali_v0.xml',
       'vendor/etc/media_codecs_diwali_v1.xml',
       'vendor/etc/media_codecs_diwali_v2.xml',
       'vendor/etc/media_codecs_taro.xml',
       'vendor/etc/media_codecs_ukee.xml',
    ): blob_fixup()
        .regex_replace('.+media_codecs_(google_audio|google_c2|google_telephony|vendor_audio).+\n', ''),
    (
        'vendor/etc/media_cape/video_system_specs.json',
        'vendor/etc/media_diwali_v0/video_system_specs.json',
        'vendor/etc/media_diwali_v1/video_system_specs.json',
        'vendor/etc/media_diwali_v2/video_system_specs.json',
        'vendor/etc/media_taro/video_system_specs.json',
        'vendor/etc/media_ukee/video_system_specs.json',
    ): blob_fixup().regex_replace('.*"libqc2vppfilter.so"\n', ''),
    'vendor/etc/seccomp_policy/c2audio.vendor.ext-arm64.policy': blob_fixup()
        .add_line_if_missing('getppid: 1')
        .add_line_if_missing('setsockopt: 1')
        .add_line_if_missing('uname: 1'),
    'vendor/etc/vintf/manifest/c2_manifest_vendor.xml': blob_fixup()
        .regex_replace('.+dolby.+\n', ''),
    (
        'vendor/lib64/libdpps.so',
        'vendor/lib64/libsnapdragoncolor-manager.so',
    ): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    'vendor/lib64/hw/vulkan.adreno.so': blob_fixup()
        .binary_regex_replace( # BV CP_WAIT_TIMESTAMP onchip 1 -> CP_NOP
              b'\x03\x80\x94\x70\x12\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00',
              b'\x03\x80\x10\x70\x12\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00',
        )
        .binary_regex_replace( # BV CP_WAIT_TIMESTAMP onchip 3 -> CP_NOP
              b'\x03\x80\x94\x70\x12\x00\x00\x00\x03\x00\x00\x00\x00\x00\x00\x00',
              b'\x03\x80\x10\x70\x12\x00\x00\x00\x03\x00\x00\x00\x00\x00\x00\x00',
        ),
    'vendor/lib64/libcamximageformatutils.so': blob_fixup()
        .replace_needed('android.hardware.graphics.allocator-V1-ndk.so', 'android.hardware.graphics.allocator-V2-ndk.so'),
    'vendor/lib64/vendor.qti.hardware.camera.postproc@1.0-service-impl.so': blob_fixup()
        .replace_needed('android.hardware.camera.common-V2-ndk.so', 'android.hardware.camera.common-V1-ndk.so')
        .replace_needed('android.hardware.graphics.allocator-V1-ndk.so', 'android.hardware.graphics.allocator-V2-ndk.so'),
    'vendor/lib64/libdisplayqos.so': blob_fixup()
        .sig_replace('68 00 b0 52', 'a8 00 b0 52')
        .sig_replace(
            '44 50 55 38 33 30 2e 78 6d 6c',
            '44 50 55 38 5f 5f 2e 78 6d 6c',
        ),
    'vendor/lib64/libqcodec2_core.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
    'vendor/lib64/libwfdmmsrc_proprietary.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V2-ndk.so', 'android.media.audio.common.types-V3-ndk.so'),
    'vendor/lib64/vendor.libdpmframework.so': blob_fixup()
        .add_needed('libhidlbase_shim.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'sm8450-common',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
