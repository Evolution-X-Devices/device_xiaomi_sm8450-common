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
    'hardware/qcom-caf/sm8450',
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
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'com.qualcomm.qti.dpm.api@1.0',
    ): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
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
    'vendor/bin/qcc-trd': blob_fixup()
        .replace_needed(
            'libgrpc++_unsecure.so', 'libgrpc++_unsecure_prebuilt.so'
        ),
    (
       'vendor/etc/media_codecs_cape.xml',
       'vendor/etc/media_codecs_diwali_v0.xml',
       'vendor/etc/media_codecs_diwali_v1.xml',
       'vendor/etc/media_codecs_diwali_v2.xml',
       'vendor/etc/media_codecs_taro.xml',
       'vendor/etc/media_codecs_ukee.xml',
    ): blob_fixup()
        .regex_replace('.+media_codecs_(google_audio|google_c2|google_telephony|vendor_audio).+\n', '')
        .regex_replace(r'(?s)(<MediaCodecs.*?>)',r'\1\n    <Include href="media_codecs_dolby_audio.xml" />'),
    'vendor/lib64/vendor.libdpmframework.so': blob_fixup()
        .add_needed('libhidlbase_shim.so'),
        ('vendor/lib/c2.dolby.avc.dec.so', 'vendor/lib/c2.dolby.avc.sec.dec.so', 'vendor/lib/c2.dolby.hevc.dec.so', 'vendor/lib/c2.dolby.hevc.sec.dec.so'): blob_fixup()
        .add_needed('libstagefright_foundation-v33.so'),
        ('vendor/bin/hw/dolbycodec2'): blob_fixup()
        .add_needed('libstagefright_foundation-v33.so'),
        ('vendor/lib/c2.dolby.client.so'): blob_fixup()
        .add_needed('libcodec2_hidl_shim.so'),
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
