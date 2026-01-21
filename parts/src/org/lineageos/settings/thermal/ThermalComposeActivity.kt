/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.lineageos.settings.thermal.ui.ThermalScreen
import org.lineageos.settings.thermal.ui.theme.ThermalTheme
import org.lineageos.settings.utils.Logging

/**
 * Thermal profile settings activity using Jetpack Compose.
 */
class ThermalComposeActivity : ComponentActivity() {

    private lateinit var thermalUtils: ThermalUtils
    private lateinit var launcherApps: LauncherApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        Logging.d(TAG, "onCreate")
        
        // Initialize dependencies
        thermalUtils = ThermalUtils.getInstance(this)
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        setContent {
            ThermalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ThermalViewModel = viewModel(
                        factory = ThermalViewModelFactory(
                            context = applicationContext,
                            thermalUtils = thermalUtils,
                            launcherApps = launcherApps
                        )
                    )
                    
                    ThermalScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logging.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "ThermalComposeActivity"
    }
}
