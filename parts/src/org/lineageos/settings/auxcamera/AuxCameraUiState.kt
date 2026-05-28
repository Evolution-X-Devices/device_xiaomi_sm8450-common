/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera

import android.graphics.drawable.Drawable

data class AuxCameraUiState(
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val apps: List<AuxCameraAppState> = emptyList(),
    val error: String? = null
)

data class AuxCameraAppState(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val enabled: Boolean
)
