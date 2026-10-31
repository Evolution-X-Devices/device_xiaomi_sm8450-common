/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.ui.ExpressiveFooter
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveScreen
import org.lineageos.settings.ui.ExpressiveSwitchRow
import org.lineageos.settings.ui.ExpressiveTheme

class ChargeBoostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val supported = ChargeBoostUtils.isSupported()

        setContent {
            val context = LocalContext.current
            var boost by remember { mutableStateOf(ChargeBoostUtils.isEnabled(context)) }
            var forceAuth by remember { mutableStateOf(ChargeBoostUtils.isForceAuthEnabled(context)) }

            ExpressiveTheme {
                ExpressiveScaffold(
                    title = stringResource(R.string.charge_boost_title),
                    onBack = { finish() },
                ) { padding ->
                    ExpressiveScreen(modifier = Modifier.padding(padding)) {
                        ExpressiveGroup(title = "") {
                            item {
                                ExpressiveSwitchRow(
                                    titleText = stringResource(R.string.charge_boost_title),
                                    summaryText = stringResource(
                                        if (supported) R.string.charge_boost_switch_summary
                                        else R.string.charge_boost_unsupported
                                    ),
                                    isChecked = boost && supported,
                                    isEnabled = supported,
                                    onChange = {
                                        ChargeBoostUtils.setEnabled(context, it)
                                        boost = it
                                    },
                                )
                            }
                            item {
                                ExpressiveSwitchRow(
                                    titleText = stringResource(R.string.charge_boost_force_auth_title),
                                    summaryText = stringResource(R.string.charge_boost_force_auth_summary),
                                    isChecked = forceAuth && supported,
                                    isEnabled = supported && boost,
                                    onChange = {
                                        ChargeBoostUtils.setForceAuthEnabled(context, it)
                                        forceAuth = it
                                    },
                                )
                            }
                        }
                        ExpressiveFooter(stringResource(R.string.charge_boost_footer))
                    }
                }
            }
        }
    }
}
