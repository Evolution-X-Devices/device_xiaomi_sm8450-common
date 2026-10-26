/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera.ui

import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.settingslib.spa.framework.theme.SettingsSpace
import org.lineageos.settings.R
import org.lineageos.settings.auxcamera.AuxCameraViewModel
import org.lineageos.settings.ui.CheckCloseSwitch
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveRow
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveSegment
import org.lineageos.settings.ui.ExpressiveSwitchRow
import org.lineageos.settings.ui.expressiveInset

@Composable
fun AuxCameraScreen(
    viewModel: AuxCameraViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ExpressiveScaffold(
        title = stringResource(R.string.aux_camera_title),
        onBack = onBackPressed,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                top = SettingsSpace.extraSmall4,
                bottom = SettingsSpace.medium1,
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall1),
        ) {
            item {
                ExpressiveGroup(title = "") {
                    item {
                        ExpressiveSwitchRow(
                            titleText = stringResource(R.string.aux_camera_enable),
                            summaryText = stringResource(R.string.aux_camera_summary),
                            isChecked = uiState.isEnabled,
                            onChange = { viewModel.toggleEnabled(it) },
                        )
                    }
                }
            }

            itemsIndexed(uiState.apps, key = { _, app -> app.packageName }) { index, app ->
                ExpressiveSegment(index = index, count = uiState.apps.size) {
                    ExpressiveRow(
                        title = app.label,
                        modifier = Modifier.expressiveInset(),
                        enabled = uiState.isEnabled,
                        onClick = { viewModel.toggleApp(app.packageName, !app.enabled) },
                        leading = {
                            AndroidView(
                                factory = { context ->
                                    ImageView(context).apply { setImageDrawable(app.icon) }
                                },
                                modifier = Modifier.size(40.dp),
                            )
                        },
                        trailing = {
                            CheckCloseSwitch(
                                checked = app.enabled,
                                onCheckedChange = { viewModel.toggleApp(app.packageName, it) },
                                enabled = uiState.isEnabled,
                            )
                        },
                    )
                }
            }
        }
    }
}
