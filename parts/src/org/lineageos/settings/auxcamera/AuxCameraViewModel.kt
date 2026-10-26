/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lineageos.settings.R

class AuxCameraViewModel(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val utils: AuxCameraUtils,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuxCameraUiState())

    val uiState: StateFlow<AuxCameraUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun toggleEnabled(enabled: Boolean) {
        utils.setEnabled(context, enabled)

        _uiState.update {
            it.copy(isEnabled = enabled)
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        val applied = utils.setAppEnabled(
            context,
            packageName,
            enabled,
        )

        if (!applied) {
            Toast.makeText(
                context,
                R.string.aux_camera_limit_reached,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        _uiState.update { state ->
            state.copy(
                apps = state.apps.map {
                    if (it.packageName == packageName) {
                        it.copy(enabled = enabled)
                    } else {
                        it
                    }
                },
            )
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabledApps = utils.getApps(context)

            val apps = launcherApps
                .getActivityList(null, Process.myUserHandle())
                .asSequence()
                .distinctBy { it.componentName.packageName }
                .filterNot {
                    it.componentName.packageName == context.packageName
                }
                .map { info ->
                    AuxCameraAppState(
                        packageName = info.componentName.packageName,
                        label = info.label.toString(),
                        icon = info.getIcon(0),
                        enabled =
                            info.componentName.packageName in enabledApps,
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()

            _uiState.update {
                it.copy(
                    isEnabled = utils.isEnabled(context),
                    apps = apps,
                )
            }
        }
    }
}

class AuxCameraViewModelFactory(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val utils: AuxCameraUtils,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuxCameraViewModel(
            context,
            launcherApps,
            utils,
        ) as T
}
