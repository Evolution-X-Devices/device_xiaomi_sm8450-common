/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.ui.graphics.toArgb
import org.lineageos.settings.thermal.ThermalUtils.ThermalState
import org.lineageos.settings.thermal.model.AppThermalState


@Composable
fun AppThermalItem(
    app: AppThermalState,
    onStateChanged: (ThermalState) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App icon and name
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    drawable = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
            
            // Thermal state dropdown
            Box {
                // Rounded chip button for the dropdown selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)) // Makes it rounded
                        .background(MaterialTheme.colorScheme.secondaryContainer) // Sets the background color with better contrast
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp), // Padding around the content
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(app.currentState.label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select thermal profile",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                ThermalDropdownMenu(
                    expanded = expanded,
                    currentState = app.currentState,
                    onDismiss = { expanded = false },
                    onStateSelected = { newState ->
                        onStateChanged(newState)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun AppIcon(
    drawable: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(drawable)
                this.contentDescription = contentDescription
            }
        },
        modifier = modifier
    )
}
