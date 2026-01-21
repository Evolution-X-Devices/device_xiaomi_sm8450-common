/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.settings.thermal.model.AppThermalState
import org.lineageos.settings.thermal.model.ThermalUiState
import org.lineageos.settings.thermal.ThermalUtils.ThermalState
import org.lineageos.settings.utils.Logging

class ThermalViewModel(
    private val context: Context,
    private val thermalUtils: ThermalUtils,
    private val launcherApps: LauncherApps
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThermalUiState())
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            if (user != Process.myUserHandle()) return
            Logging.d(TAG, "onPackageRemoved: $packageName")
            removeApp(packageName)
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            if (user != Process.myUserHandle()) return
            Logging.d(TAG, "onPackageAdded: $packageName")
            viewModelScope.launch {
                addNewApp(packageName)
            }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            // No action needed
        }

        override fun onPackagesAvailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            // No action needed
        }

        override fun onPackagesUnavailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            // No action needed
        }
    }

    init {
        // Initialize with current thermal state
        _uiState.update { it.copy(isEnabled = thermalUtils.enabled) }
        
        // Load apps if thermal is enabled
        if (thermalUtils.enabled) {
            loadApps()
        }
        
        // Register launcher apps callback on IO thread to avoid blocking
        viewModelScope.launch(Dispatchers.IO) {
            try {
                launcherApps.registerCallback(launcherAppsCallback)
                Logging.d(TAG, "LauncherApps callback registered")
            } catch (e: Exception) {
                Logging.d(TAG, "Failed to register callback: ${e.message}")
            }
        }
    }

    fun toggleThermalEnabled(enabled: Boolean) {
        Logging.d(TAG, "toggleThermalEnabled: $enabled")
        thermalUtils.enabled = enabled
        _uiState.update { it.copy(isEnabled = enabled) }
        
        if (enabled && _uiState.value.apps.isEmpty()) {
            loadApps()
        }
    }

    fun updateAppThermalState(packageName: String, state: ThermalState) {
        Logging.d(TAG, "updateAppThermalState: $packageName -> $state")
        thermalUtils.writePackage(packageName, state.id)
        
        // Update UI state
        _uiState.update { currentState ->
            currentState.copy(
                apps = currentState.apps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(currentState = state)
                    } else {
                        app
                    }
                }
            )
        }
    }

    fun resetProfiles() {
        Logging.d(TAG, "resetProfiles")
        thermalUtils.resetProfiles()
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val apps = withContext(Dispatchers.IO) {
                    launcherApps
                        .getActivityList(null, Process.myUserHandle())
                        .distinctBy { it.componentName.packageName }
                        .map { info ->
                            AppThermalState(
                                packageName = info.componentName.packageName,
                                label = info.label.toString(),
                                icon = info.getIcon(0),
                                currentState = thermalUtils.getStateForPackage(
                                    info.componentName.packageName
                                )
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                }
                
                Logging.d(TAG, "Loaded ${apps.size} apps")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        apps = apps,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Logging.d(TAG, "Error loading apps: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun removeApp(packageName: String) {
        _uiState.update { currentState ->
            currentState.copy(
                apps = currentState.apps.filter { it.packageName != packageName }
            )
        }
    }

    private suspend fun addNewApp(packageName: String) {
        withContext(Dispatchers.IO) {
            val info = launcherApps.getActivityList(packageName, Process.myUserHandle())
                .firstOrNull() ?: return@withContext
            
            val newApp = AppThermalState(
                packageName = info.componentName.packageName,
                label = info.label.toString(),
                icon = info.getIcon(0),
                currentState = thermalUtils.getStateForPackage(info.componentName.packageName)
            )
            
            _uiState.update { currentState ->
                // Check if app already exists
                if (currentState.apps.any { it.packageName == packageName }) {
                    return@update currentState
                }
                
                // Insert in sorted position
                val updatedApps = (currentState.apps + newApp)
                    .sortedBy { it.label.lowercase() }
                
                currentState.copy(apps = updatedApps)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        launcherApps.unregisterCallback(launcherAppsCallback)
        Logging.d(TAG, "ViewModel cleared")
    }

    companion object {
        private const val TAG = "ThermalViewModel"
    }
}

/**
 * Factory for creating ThermalViewModel with dependencies.
 */
class ThermalViewModelFactory(
    private val context: Context,
    private val thermalUtils: ThermalUtils,
    private val launcherApps: LauncherApps
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThermalViewModel::class.java)) {
            return ThermalViewModel(context, thermalUtils, launcherApps) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
