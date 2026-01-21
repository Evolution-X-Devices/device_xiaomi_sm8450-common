/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.thermal.ThermalUtils.ThermalState

/**
 * Dropdown menu for selecting thermal profiles.
 */
@Composable
fun ThermalDropdownMenu(
    expanded: Boolean,
    currentState: ThermalState,
    onDismiss: () -> Unit,
    onStateSelected: (ThermalState) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        ThermalState.values().forEach { state ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(state.label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state == currentState) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = { onStateSelected(state) }
            )
        }
    }
}
