#
# Copyright (C) 2020 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Add common definitions for Qualcomm
$(call inherit-product, hardware/qcom-caf/common/common.mk)

# Enable project quotas and casefolding for emulated storage without sdcardfs
$(call inherit-product, $(SRC_TARGET_DIR)/product/emulated_storage.mk)

# Enforce generic ramdisk allow list
$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_ramdisk.mk)

# Enable virtual AB with vendor ramdisk
$(call inherit-product, $(SRC_TARGET_DIR)/product/virtual_ab_ota/launch_with_vendor_ramdisk.mk)

# Setup dalvik vm configs
$(call inherit-product, frameworks/native/build/phone-xhdpi-6144-dalvik-heap.mk)

# Inherit from the proprietary version
$(call inherit-product, vendor/xiaomi/sm8450-common/sm8450-common-vendor.mk)

# A/B
AB_OTA_POSTINSTALL_CONFIG += \
    RUN_POSTINSTALL_system=true \
    POSTINSTALL_PATH_system=system/bin/otapreopt_script \
    FILESYSTEM_TYPE_system=ext4 \
    POSTINSTALL_OPTIONAL_system=true

AB_OTA_POSTINSTALL_CONFIG += \
    RUN_POSTINSTALL_vendor=true \
    POSTINSTALL_PATH_vendor=bin/checkpoint_gc \
    FILESYSTEM_TYPE_vendor=ext4 \
    POSTINSTALL_OPTIONAL_vendor=true

PRODUCT_PACKAGES += \
    checkpoint_gc \
    otapreopt_script

# ANT+
PRODUCT_PACKAGES += \
    com.dsi.ant@1.0.vendor:64

# API
PRODUCT_SHIPPING_API_LEVEL := 31
BOARD_SHIPPING_API_LEVEL := 31

# Audio
PRODUCT_PACKAGES += \
    android.hardware.audio@7.0-impl:64 \
    android.hardware.audio.effect@7.0-impl:64 \
    android.hardware.audio.service \
    android.hardware.soundtrigger@2.3-impl:64 \
    vendor.qti.hardware.AGMIPC@1.0-service \
    vendor.qti.hardware.pal@1.0-impl:64

PRODUCT_PACKAGES += \
    audio.bluetooth.default:64 \
    audio.primary.taro:64 \
    audio.r_submix.default:64 \
    audio.usb.default:64 \
    sound_trigger.primary.taro:64

PRODUCT_PACKAGES += \
    audioadsprpcd

PRODUCT_PACKAGES += \
    libagm_compress_plugin:64 \
    libagm_mixer_plugin:64 \
    libagm_pcm_plugin:64 \
    libagmclient:64 \
    libagmmixer:64 \
    libaudioroute.vendor:64 \
    libbatterylistener:64 \
    libfmpal:64 \
    libpalclient:64 \
    libqcompostprocbundle:64 \
    libqcomvisualizer:64 \
    libqcomvoiceprocessing:64 \
    libsndcardparser:64 \
    libtinycompress:64 \
    libvolumelistener:64

$(foreach sku, taro diwali cape ukee parrot, \
    $(eval PRODUCT_COPY_FILES += \
        $(LOCAL_PATH)/audio/audio_effects.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/sku_$(sku)/audio_effects.xml \
        $(LOCAL_PATH)/audio/audio_policy_configuration.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/sku_$(sku)/audio_policy_configuration.xml \
    ))

PRODUCT_COPY_FILES += \
    frameworks/av/services/audiopolicy/config/audio_policy_volumes.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_volumes.xml \
    frameworks/av/services/audiopolicy/config/default_volume_tables.xml:$(TARGET_COPY_OUT_VENDOR)/etc/default_volume_tables.xml \
    frameworks/av/services/audiopolicy/config/r_submix_audio_policy_configuration.xml:$(TARGET_COPY_OUT_VENDOR)/etc/r_submix_audio_policy_configuration.xml \
    frameworks/native/data/etc/android.hardware.audio.low_latency.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.audio.low_latency.xml \
    frameworks/native/data/etc/android.hardware.audio.pro.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.audio.pro.xml \
    frameworks/native/data/etc/android.software.midi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.midi.xml

# Automotive
PRODUCT_PACKAGES += \
    android.hardware.automotive.vehicle@2.0-manager-lib:64

# Bluetooth
PRODUCT_PACKAGES += \
    android.hardware.bluetooth@1.0.vendor:64 \
    android.hardware.bluetooth.audio-impl:64 \
    vendor.qti.hardware.bluetooth_audio@2.1.vendor:64 \
    vendor.qti.hardware.btconfigstore@1.0.vendor:64 \
    vendor.qti.hardware.btconfigstore@2.0.vendor:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.bluetooth_le.xml

# Boot control
PRODUCT_PACKAGES += \
    android.hardware.boot@1.2-impl-qti:64 \
    android.hardware.boot@1.2-impl-qti.recovery:64 \
    android.hardware.boot@1.2-service

PRODUCT_PACKAGES_DEBUG += \
    bootctl:64

# Camera
PRODUCT_PACKAGES += \
    android.hardware.camera.provider@2.7.vendor:64 \
    camera.device@1.0-impl:64 \
    libcamera2ndk_vendor:64 \
    libexif.vendor:64 \
    libjpeg.vendor:64 \
    liblz4.vendor:64 \
    vendor.qti.hardware.camera.aon@1.0.vendor:64 \
    vendor.qti.hardware.camera.postproc@1.0.vendor:64

PRODUCT_PACKAGES += \
    libMegviiFacepp-0.5.2:64 \
    libmegface:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.camera.flash-autofocus.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.front.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.front.xml \
    frameworks/native/data/etc/android.hardware.camera.full.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.full.xml \
    frameworks/native/data/etc/android.hardware.camera.raw.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.raw.xml

# Capabilityconfigstore
PRODUCT_PACKAGES += \
    vendor.qti.hardware.capabilityconfigstore@1.0.vendor:64

# Display
PRODUCT_PACKAGES += \
    android.hardware.graphics.mapper@4.0-impl-qti-display \
    vendor.qti.hardware.display.allocator-service \
    vendor.qti.hardware.display.composer-service

PRODUCT_PACKAGES += \
    init.qti.display_boot.rc \
    init.qti.display_boot.sh

PRODUCT_PACKAGES += \
    vendor.display.config@1.11.vendor:64 \
    vendor.qti.hardware.display.config-V2-ndk.vendor:64 \
    vendor.qti.hardware.display.mapper@2.0.vendor:64

PRODUCT_COPY_FILES += \
    hardware/qcom-caf/sm8450/display/config/snapdragon_color_libs_config.xml:$(TARGET_COPY_OUT_VENDOR)/etc/snapdragon_color_libs_config.xml

# DRM
PRODUCT_PACKAGES += \
    android.hardware.drm@1.4.vendor:64 \
    android.hardware.drm-service.clearkey \
    libdrm.vendor:64

# Dumpstate
PRODUCT_PACKAGES += \
    android.hardware.dumpstate@1.1.vendor:64

# Fingerprint
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint@2.3-service.xiaomi \
    libudfpshandler:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.fingerprint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.fingerprint.xml

# Fstab
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/charger_fw_fstab.qti:$(TARGET_COPY_OUT_VENDOR)/etc/charger_fw_fstab.qti

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.qcom \
    $(LOCAL_PATH)/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_RECOVERY)/root/first_stage_ramdisk/fstab.qcom \
    $(LOCAL_PATH)/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.qcom

# Gatekeeper
PRODUCT_PACKAGES += \
    android.hardware.gatekeeper@1.0.vendor:64

# GPS
PRODUCT_PACKAGES += \
    android.hardware.gnss@2.1-impl-qti:64 \
    android.hardware.gnss-aidl-impl-qti:64 \
    android.hardware.gnss-aidl-service-qti

PRODUCT_PACKAGES += \
    libbatching:64 \
    libgeofencing:64 \
    libgnss:64

PRODUCT_PACKAGES += \
    apdr.conf \
    batching.conf \
    gnss_antenna_info.conf \
    gps.conf \
    izat.conf \
    lowi.conf \
    sap.conf

PRODUCT_PACKAGES += \
    gnss@2.0-base.policy \
    gnss@2.0-xtra-daemon.policy

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.location.gps.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.location.gps.xml

# Graphics
PRODUCT_PACKAGES += \
    libgui_vendor:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.opengles.aep.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.opengles.aep.xml \
    frameworks/native/data/etc/android.hardware.vulkan.compute-0.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.compute-0.xml \
    frameworks/native/data/etc/android.hardware.vulkan.level-1.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.level-1.xml \
    frameworks/native/data/etc/android.hardware.vulkan.version-1_1.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.version-1_1.xml \
    frameworks/native/data/etc/android.software.opengles.deqp.level-2021-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.opengles.deqp.level.xml \
    frameworks/native/data/etc/android.software.vulkan.deqp.level-2021-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.vulkan.deqp.level.xml

# Health
PRODUCT_PACKAGES += \
    android.hardware.health-service.qti \
    android.hardware.health-service.qti_recovery

# HIDL
PRODUCT_PACKAGES += \
    android.hidl.allocator@1.0.vendor:64 \
    android.hidl.memory.block@1.0.vendor:64 \
    libhidltransport.vendor:64 \
    libhwbinder.vendor:64

# Hotword Enrollement
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/privapp-permissions-hotword.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-hotword.xml

# Identity
PRODUCT_PACKAGES += \
    android.hardware.identity-V3-ndk_platform.vendor:64 \
    android.hardware.keymaster-V3-ndk_platform.vendor:64

# IPACM
PRODUCT_PACKAGES += \
    ipacm \
    IPACM_cfg.xml \
    IPACM_Filter_cfg.xml

# IR
PRODUCT_PACKAGES += \
    android.hardware.ir-service.xiaomi

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.consumerir.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.consumerir.xml

# JSON
PRODUCT_PACKAGES += \
    libjson

# Keylayout
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/keylayout/uinput-fpc.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/uinput-fpc.kl \
    $(LOCAL_PATH)/keylayout/uinput-goodix.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/uinput-goodix.kl

# Keymaster
PRODUCT_PACKAGES += \
    android.hardware.keymaster@4.1.vendor:64 \
    android.hardware.security.keymint-V1-ndk_platform.vendor:64 \
    android.hardware.security.secureclock-V1-ndk_platform.vendor:64 \
    android.hardware.security.sharedsecret-V1-ndk_platform.vendor:64 \
    android.hardware.security.rkp-V3-ndk.vendor:64 \
    libkeymaster_messages.vendor:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.keystore.app_attest_key.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.keystore.app_attest_key.xml

# Lineage Health
PRODUCT_PACKAGES += \
    vendor.lineage.health-service.default

# Media
PRODUCT_PACKAGES += \
    libcodec2_hidl@1.0.vendor

PRODUCT_PACKAGES += \
    init.qti.media.rc \
    init.qti.media.sh

# Mlipay
PRODUCT_PACKAGES += \
    vendor.xiaomi.hardware.mlipay@1.1.vendor \
    vendor.xiaomi.hardware.mtdservice@1.0.vendor

# Network
PRODUCT_PACKAGES += \
    android.system.net.netd@1.1.vendor:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.software.ipsec_tunnels.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.ipsec_tunnels.xml

# NFC / Secure Element
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp \
    android.hardware.nfc@1.2.vendor:64 \
    android.hardware.secure_element@1.2.vendor:64 \
    com.android.nfc_extras \
    libchrome.vendor:64

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.se.omapi.uicc.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.se.omapi.uicc.xml

ifeq ($(TARGET_NFC_SUPPORTED_SKUS),)
TARGET_COPY_OUT_NFC_SKU_PERMISSIONS := $(TARGET_COPY_OUT_VENDOR)/etc/permissions/
else
$(foreach sku, $(TARGET_NFC_SUPPORTED_SKUS), \
    $(eval TARGET_COPY_OUT_NFC_SKU_PERMISSIONS += $(TARGET_COPY_OUT_ODM)/etc/permissions/sku_$(sku)))
endif

$(foreach sku_out, $(TARGET_COPY_OUT_NFC_SKU_PERMISSIONS), \
    $(eval PRODUCT_COPY_FILES += \
        frameworks/native/data/etc/android.hardware.nfc.ese.xml:$(sku_out)/android.hardware.nfc.ese.xml \
        frameworks/native/data/etc/android.hardware.nfc.hce.xml:$(sku_out)/android.hardware.nfc.hce.xml \
        frameworks/native/data/etc/android.hardware.nfc.hcef.xml:$(sku_out)/android.hardware.nfc.hcef.xml \
        frameworks/native/data/etc/android.hardware.nfc.uicc.xml:$(sku_out)/android.hardware.nfc.uicc.xml \
        frameworks/native/data/etc/android.hardware.nfc.xml:$(sku_out)/android.hardware.nfc.xml \
        frameworks/native/data/etc/android.hardware.se.omapi.ese.xml:$(sku_out)/etc/permissions/android.hardware.se.omapi.ese.xml \
        frameworks/native/data/etc/com.android.nfc_extras.xml:$(sku_out)/com.android.nfc_extras.xml \
        frameworks/native/data/etc/com.nxp.mifare.xml:$(sku_out)/com.nxp.mifare.xml))

# Overlays
PRODUCT_PACKAGES += \
    CarrierConfigResCommon \
    FrameworksResCommon \
    SettingsResCommon \
    SystemUIResCommon \
    TelecommResCommon \
    TelephonyResCommon \
    WifiResCommon

PRODUCT_PACKAGES += \
    DialerResXiaomi \
    FrameworksResTarget \
    FrameworksResXiaomi \
    LineageResXiaomi \
    SettingsProviderResXiaomi \
    SettingsResXiaomi \
    WifiResTarget \
    WifiResTarget_cape \
    WifiResTarget_spf

# Partitions
PRODUCT_USE_DYNAMIC_PARTITIONS := true

# Perf
PRODUCT_PACKAGES += \
    vendor.qti.hardware.perf@2.3.vendor:64

# Power
PRODUCT_PACKAGES += \
    android.hardware.power-service-qti

PRODUCT_COPY_FILES += \
    vendor/qcom/opensource/power/config/taro/powerhint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/powerhint.xml

# Protobuf
PRODUCT_PACKAGES += \
    libprotobuf-cpp-full-3.9.1-vendorcompat:64 \
    libprotobuf-cpp-lite-3.9.1-vendorcompat:64

# QMI
PRODUCT_PACKAGES += \
    libcurl.vendor:64 \
    libjsoncpp.vendor:64 \
    libnetutils.vendor:64 \
    libsqlite.vendor:64

# QTI
PRODUCT_PACKAGES += \
    libqti_vndfwk_detect.vendor:64 \
    libvndfwk_detect_jni.qti_vendor:64 \
    vendor.qti.hardware.systemhelper@1.0.vendor:64

# QXR
PRODUCT_PACKAGES += \
    android.hardware.common-V2-ndk_platform.vendor:64

# Recovery
PRODUCT_PACKAGES += \
    fastbootd

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/init.recovery.qcom.rc:$(TARGET_COPY_OUT_RECOVERY)/root/init.recovery.qcom.rc

# RenderScript
PRODUCT_PACKAGES += \
    android.hardware.renderscript@1.0-impl:64

# RIL
PRODUCT_PACKAGES += \
    android.hardware.radio.config@1.3.vendor:64 \
    android.hardware.radio.deprecated@1.0.vendor:64 \

PRODUCT_PACKAGES += \
    librmnetctl

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors@2.1-service.xiaomi-multihal \
    android.frameworks.sensorservice@1.0.vendor:64

PRODUCT_PACKAGES += \
    sensors.xiaomi:64

PRODUCT_PACKAGES += \
    sensor-notifier

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/sensors/hals.conf:$(TARGET_COPY_OUT_ODM)/etc/sensors/hals.conf

$(foreach sku, taro diwali cape ukee, \
    $(eval PRODUCT_COPY_FILES += \
        frameworks/native/data/etc/android.hardware.sensor.accelerometer.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.accelerometer.xml \
        frameworks/native/data/etc/android.hardware.sensor.compass.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.compass.xml \
        frameworks/native/data/etc/android.hardware.sensor.gyroscope.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.gyroscope.xml \
        frameworks/native/data/etc/android.hardware.sensor.light.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.light.xml \
        frameworks/native/data/etc/android.hardware.sensor.proximity.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.proximity.xml \
        frameworks/native/data/etc/android.hardware.sensor.stepcounter.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.stepcounter.xml \
        frameworks/native/data/etc/android.hardware.sensor.stepdetector.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/sku_$(sku)/android.hardware.sensor.stepdetector.xml \
    ))

# Servicetracker
PRODUCT_PACKAGES += \
    vendor.qti.hardware.servicetracker@1.2.vendor:64

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH) \
    hardware/xiaomi

# Telephony
PRODUCT_PACKAGES += \
    extphonelib \
    extphonelib-product \
    extphonelib.xml \
    extphonelib_product.xml \
    ims-ext-common \
    ims_ext_common.xml \
    qti-telephony-hidl-wrapper \
    qti-telephony-hidl-wrapper-prd \
    qti_telephony_hidl_wrapper.xml \
    qti_telephony_hidl_wrapper_prd.xml \
    qti-telephony-utils \
    qti-telephony-utils-prd \
    qti_telephony_utils.xml \
    qti_telephony_utils_prd.xml \
    telephony-ext \
    xiaomi-telephony-stub

PRODUCT_BOOT_JARS += \
    telephony-ext \
    xiaomi-telephony-stub

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.cdma.xml \
    frameworks/native/data/etc/android.hardware.telephony.gsm.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/native/data/etc/android.hardware.telephony.ims.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.ims.xml \
    frameworks/native/data/etc/android.software.sip.voip.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.sip.voip.xml

# Thermal
PRODUCT_PACKAGES += \
    android.hardware.thermal@2.0-service.qti-v2

# Touchscreen
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml

# Ueventd
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/ueventd.qcom.rc:$(TARGET_COPY_OUT_VENDOR)/etc/ueventd.rc \
    $(LOCAL_PATH)/rootdir/etc/ueventd-odm.rc:$(TARGET_COPY_OUT_ODM)/etc/ueventd.rc

# Update engine
PRODUCT_PACKAGES += \
    update_engine \
    update_engine_sideload \
    update_verifier

PRODUCT_PACKAGES_DEBUG += \
    update_engine_client

# USB
PRODUCT_PACKAGES += \
    android.hardware.usb@1.3-service-qti \
    android.hardware.usb.gadget@1.2-service-qti \
    libusbhost.vendor:64

PRODUCT_PACKAGES += \
    init.qcom.usb.rc \
    init.qcom.usb.sh \
    usb_compositions.conf

PRODUCT_SOONG_NAMESPACES += vendor/qcom/opensource/usb/etc

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.usb.host.xml

# Vendor service manager
PRODUCT_PACKAGES += \
    vndservicemanager

# Vendor init
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/init.qcom.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/hw/init.qcom.rc \
    $(LOCAL_PATH)/rootdir/etc/init.qti.kernel.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/hw/init.qti.kernel.rc \
    $(LOCAL_PATH)/rootdir/etc/init.target.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/hw/init.target.rc \
    $(LOCAL_PATH)/rootdir/etc/init.touch_report.rc:$(TARGET_COPY_OUT_RECOVERY)/root/vendor/etc/init/init.touch_report.rc \
    $(LOCAL_PATH)/rootdir/etc/init.touch_report.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.touch_report.rc \
    $(LOCAL_PATH)/rootdir/etc/init.xiaomi_sm8450.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.xiaomi_sm8450.rc \

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/bin/init.class_main.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.class_main.sh \
    $(LOCAL_PATH)/rootdir/bin/init.kernel.post_boot-cape.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.kernel.post_boot-cape.sh \
    $(LOCAL_PATH)/rootdir/bin/init.kernel.post_boot-diwali.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.kernel.post_boot-diwali.sh \
    $(LOCAL_PATH)/rootdir/bin/init.kernel.post_boot-taro.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.kernel.post_boot-taro.sh \
    $(LOCAL_PATH)/rootdir/bin/init.kernel.post_boot.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.kernel.post_boot.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qcom.class_core.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qcom.class_core.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qcom.early_boot.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qcom.early_boot.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qcom.post_boot.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qcom.post_boot.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qcom.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qcom.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qti.kernel.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qti.kernel.sh \
    $(LOCAL_PATH)/rootdir/bin/init.qti.write.sh:$(TARGET_COPY_OUT_VENDOR)/bin/init.qti.write.sh \
    $(LOCAL_PATH)/rootdir/bin/vendor_modprobe.sh:$(TARGET_COPY_OUT_VENDOR)/bin/vendor_modprobe.sh

# Verified boot
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.software.verified_boot.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.verified_boot.xml

# Vibrator
PRODUCT_PACKAGES += \
    vendor.qti.hardware.vibrator.service

PRODUCT_COPY_FILES += \
    vendor/qcom/opensource/vibrator/excluded-input-devices.xml:$(TARGET_COPY_OUT_VENDOR)/etc/excluded-input-devices.xml

# WiFi
PRODUCT_PACKAGES += \
    android.hardware.wifi-service \
    android.hardware.wifi.hostapd@1.0.vendor \
    hostapd \
    hostapd_cli \
    libwifi-hal-qcom:64 \
    libwpa_client:64 \
    wpa_cli \
    wpa_supplicant \
    wpa_supplicant.conf

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/wlan/p2p_supplicant_overlay.conf:$(TARGET_COPY_OUT_VENDOR)/etc/wifi/p2p_supplicant_overlay.conf \
    $(LOCAL_PATH)/wlan/vendor_cmd.xml:$(TARGET_COPY_OUT_VENDOR)/etc/wifi/vendor_cmd.xml \
    $(LOCAL_PATH)/wlan/WCNSS_qcom_cfg_qca6490.ini:$(TARGET_COPY_OUT_VENDOR)/etc/wifi/qca6490/WCNSS_qcom_cfg.ini \
    $(LOCAL_PATH)/wlan/WCNSS_qcom_cfg_qca6750.ini:$(TARGET_COPY_OUT_VENDOR)/etc/wifi/qca6750/WCNSS_qcom_cfg.ini \
    $(LOCAL_PATH)/wlan/wpa_supplicant_overlay.conf:$(TARGET_COPY_OUT_VENDOR)/etc/wifi/wpa_supplicant_overlay.conf

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.wifi.aware.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.aware.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.wifi.passpoint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.passpoint.xml \
    frameworks/native/data/etc/android.hardware.wifi.rtt.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.rtt.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.xml

# WiFi Display
PRODUCT_PACKAGES += \
    libnl:64 \
    libwfdaac_vendor:64

# WiFi firmware symlinks
PRODUCT_PACKAGES += \
    firmware_WCNSS_qcom_cfg.ini_symlink \
    firmware_qca6490_WCNSS_qcom_cfg.ini_symlink \
    firmware_qca6490_wlan_mac.bin_symlink \
    firmware_qca6750_WCNSS_qcom_cfg.ini_symlink \
    firmware_qca6750_wlan_mac.bin_symlink
