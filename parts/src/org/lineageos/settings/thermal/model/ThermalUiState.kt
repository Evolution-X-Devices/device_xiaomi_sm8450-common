/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal.model

import android.graphics.drawable.Drawable
import org.lineageos.settings.thermal.ThermalUtils.ThermalState

data class ThermalUiState(
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val apps: List<AppThermalState> = emptyList(),
    val error: String? = null
)

data class AppThermalState(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val currentState: ThermalState
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppThermalState) return false
        return packageName == other.packageName &&
                label == other.label &&
                currentState == other.currentState
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + currentState.hashCode()
        return result
    }
}
