#=============================================================================
# Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
# All Rights Reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.
#=============================================================================

rev=`cat /sys/devices/soc0/revision`
ddr_type=`od -An -tx /proc/device-tree/memory/ddr_device_type`
ddr_type4="07"
ddr_type5="08"

# Core control parameters for gold
echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
echo 30 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/task_thres

# Core control parameters for gold+
echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/min_cpus
echo 60 > /sys/devices/system/cpu/cpu7/core_ctl/busy_up_thres
echo 30 > /sys/devices/system/cpu/cpu7/core_ctl/busy_down_thres
echo 100 > /sys/devices/system/cpu/cpu7/core_ctl/offline_delay_ms
echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/task_thres

# Controls how many more tasks should be eligible to run on gold CPUs
# w.r.t number of gold CPUs available to trigger assist (max number of
# tasks eligible to run on previous cluster minus number of CPUs in
# the previous cluster).
#
# Setting to 1 by default which means there should be at least
# 4 tasks eligible to run on gold cluster (tasks running on gold cores
# plus misfit tasks on silver cores) to trigger assitance from gold+.
echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/nr_prev_assist_thresh

# Disable Core control on silver
echo 0 > /sys/devices/system/cpu/cpu0/core_ctl/enable

# Setting b.L scheduler parameters
echo 85 85 > /proc/sys/walt/sched_downmigrate
echo 95 95 > /proc/sys/walt/sched_upmigrate
echo 85 > /proc/sys/walt/sched_group_downmigrate
echo 100 > /proc/sys/walt/sched_group_upmigrate
echo 1 > /proc/sys/walt/sched_walt_rotate_big_tasks
echo 400000000 > /proc/sys/walt/sched_coloc_downmigrate_ns
echo 39000000 39000000 39000000 39000000 39000000 39000000 39000000 5000000 > /proc/sys/walt/sched_coloc_busy_hyst_cpu_ns
echo 240 > /proc/sys/walt/sched_coloc_busy_hysteresis_enable_cpus
echo 10 10 10 10 10 10 10 95 > /proc/sys/walt/sched_coloc_busy_hyst_cpu_busy_pct
echo 5000000 5000000 5000000 5000000 5000000 5000000 5000000 2000000 > /proc/sys/walt/sched_util_busy_hyst_cpu_ns
echo 255 > /proc/sys/walt/sched_util_busy_hysteresis_enable_cpus
echo 15 15 15 15 15 15 15 15 > /proc/sys/walt/sched_util_busy_hyst_cpu_util

# set the threshold for low latency task boost feature which prioritize
# binder activity tasks
echo 325 > /proc/sys/walt/walt_low_latency_task_threshold

# cpuset parameters
echo 0-3 > /dev/cpuset/background/cpus
echo 0-2 > /dev/cpuset/system-background/cpus

# Turn off scheduler boost at the end
echo 0 > /proc/sys/walt/sched_boost

# Reset the RT boost, which is 1024 (max) by default.
echo 0 > /proc/sys/kernel/sched_util_clamp_min_rt_default

# configure governor settings for silver cluster
echo "walt" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
echo 0 > /sys/devices/system/cpu/cpufreq/policy0/walt/down_rate_limit_us
echo 0 > /sys/devices/system/cpu/cpufreq/policy0/walt/up_rate_limit_us
echo 1228800 > /sys/devices/system/cpu/cpufreq/policy0/walt/hispeed_freq
echo 556800 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
echo 1804800 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq
echo 1 > /sys/devices/system/cpu/cpufreq/policy0/walt/pl

# configure input boost settings
echo 1132800 0 0 0 0 0 0 0 > /proc/sys/walt/input_boost/input_boost_freq
echo 100 > /proc/sys/walt/input_boost/input_boost_ms

# configure governor settings for gold cluster
echo "walt" > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor
echo 0 > /sys/devices/system/cpu/cpufreq/policy4/walt/down_rate_limit_us
echo 0 > /sys/devices/system/cpu/cpufreq/policy4/walt/up_rate_limit_us
echo 1555200 > /sys/devices/system/cpu/cpufreq/policy4/walt/hispeed_freq
echo 1 > /sys/devices/system/cpu/cpufreq/policy4/walt/pl

# configure governor settings for gold+ cluster
echo "walt" > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor
echo 0 > /sys/devices/system/cpu/cpufreq/policy7/walt/down_rate_limit_us
echo 0 > /sys/devices/system/cpu/cpufreq/policy7/walt/up_rate_limit_us
echo 1651200 > /sys/devices/system/cpu/cpufreq/policy7/walt/hispeed_freq
echo 1 > /sys/devices/system/cpu/cpufreq/policy7/walt/pl

# colocation V3 settings
echo 768000 > /sys/devices/system/cpu/cpufreq/policy4/walt/rtg_boost_freq

# configure bus-dcvs
bus_dcvs="/sys/devices/system/cpu/bus_dcvs"

for device in $bus_dcvs/*
do
	cat $device/hw_min_freq > $device/boost_freq
done

for llccbw in $bus_dcvs/LLCC/*bwmon-llcc
do
	echo "4577 7110 9155 12298 14236 15258" > $llccbw/mbps_zones
	echo 4 > $llccbw/sample_ms
	echo 80 > $llccbw/io_percent
	echo 20 > $llccbw/hist_memory
	echo 10 > $llccbw/hyst_length
	echo 30 > $llccbw/down_thres
	echo 0 > $llccbw/guard_band_mbps
	echo 250 > $llccbw/up_scale
	echo 1600 > $llccbw/idle_mbps
	echo 806000 > $llccbw/max_freq
	echo 40 > $llccbw/window_ms
done

for ddrbw in $bus_dcvs/DDR/*bwmon-ddr
do
	echo "1720 2086 2929 3879 6515 7980 12191" > $ddrbw/mbps_zones
	echo 4 > $ddrbw/sample_ms
	echo 80 > $ddrbw/io_percent
	echo 20 > $ddrbw/hist_memory
	echo 10 > $ddrbw/hyst_length
	echo 30 > $ddrbw/down_thres
	echo 0 > $ddrbw/guard_band_mbps
	echo 250 > $ddrbw/up_scale
	echo 1600 > $ddrbw/idle_mbps
	echo 2092000 > $ddrbw/max_freq
	echo 40 > $ddrbw/window_ms
done

for latfloor in $bus_dcvs/*/*latfloor
do
	echo 25000 > $latfloor/ipm_ceil
done

for l3silver in $bus_dcvs/L3/*silver
do
	echo 1708800 > $l3silver/max_freq
done

for l3gold in $bus_dcvs/L3/*gold
do
	echo 4000 > $l3gold/ipm_ceil
	echo 1708800 > $l3gold/max_freq
done

for l3prime in $bus_dcvs/L3/*prime
do
	echo 20000 > $l3prime/ipm_ceil
	echo 1708800 > $l3prime/max_freq
done

for l3pcompute in $bus_dcvs/L3/*prime-compute
do
	echo 1708800 > $l3pcompute/max_freq
done

for ddrprime in $bus_dcvs/DDR/*prime
do
	echo 25 > $ddrprime/freq_scale_pct
	echo 1881 > $ddrprime/freq_scale_limit_mhz
done

for qosgold in $bus_dcvs/DDRQOS/*gold
do
	echo 50 > $qosgold/ipm_ceil
done

#set s2idle as default
echo s2idle > /sys/power/mem_sleep

#Enable LPM
echo N > /sys/devices/system/cpu/qcom_lpm/parameters/sleep_disabled

# Let kernel know our image version/variant/crm_version
if [ -f /sys/devices/soc0/select_image ]; then
	image_version="10:"
	image_version+=`getprop ro.build.id`
	image_version+=":"
	image_version+=`getprop ro.build.version.incremental`
	image_variant=`getprop ro.product.name`
	image_variant+="-"
	image_variant+=`getprop ro.build.type`
	oem_version=`getprop ro.build.version.codename`
	echo 10 > /sys/devices/soc0/select_image
	echo $image_version > /sys/devices/soc0/image_version
	echo $image_variant > /sys/devices/soc0/image_variant
	echo $oem_version > /sys/devices/soc0/image_crm_version
fi

# Change console log level as per console config property
console_config=`getprop persist.vendor.console.silent.config`
case "$console_config" in
	"1")
		echo "Enable console config to $console_config"
		echo 0 > /proc/sys/kernel/printk
	;;
	*)
		echo "Enable console config to $console_config"
	;;
esac

setprop vendor.post_boot.parsed 1
