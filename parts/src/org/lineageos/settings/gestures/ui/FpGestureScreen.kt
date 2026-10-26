/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.gestures.ui

import android.view.HapticFeedbackConstants
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import org.lineageos.settings.R
import org.lineageos.settings.gestures.FpGestureUtils
import org.lineageos.settings.ui.ExpressiveFooter
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveRow
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveScreen
import org.lineageos.settings.ui.ExpressiveSwitchRow

private val ACTIONS = listOf(
    1 to R.string.action_screenshot,
    2 to R.string.action_assistant,
    3 to R.string.action_play_pause,
    4 to R.string.action_notifications,
    5 to R.string.action_camera,
    6 to R.string.action_flashlight,
    7 to R.string.action_mute,
    8 to R.string.action_vibrate,
    9 to R.string.action_volume,
    10 to R.string.action_sleep,
)

@Composable
fun FpGestureScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    var isEnabled by remember { mutableStateOf(FpGestureUtils.isEnabled(context)) }
    var selectedAction by remember { mutableIntStateOf(FpGestureUtils.getAction(context)) }
    var screenOffEnabled by remember { mutableStateOf(FpGestureUtils.isScreenOffEnabled(context)) }
    var screenOffAction by remember {
        mutableIntStateOf(FpGestureUtils.getScreenOffAction(context))
    }

    ExpressiveScaffold(
        title = stringResource(R.string.fp_gestures_title),
        onBack = onBackPressed,
    ) { padding ->
        ExpressiveScreen(modifier = Modifier.padding(padding)) {
            ExpressiveGroup(title = "") {
                item {
                    ExpressiveSwitchRow(
                        titleText = stringResource(R.string.fp_double_tap_enable),
                        summaryText = stringResource(R.string.fp_double_tap_action),
                        isChecked = isEnabled,
                        onChange = {
                            isEnabled = it
                            FpGestureUtils.setEnabled(context, it)
                        },
                    )
                }
            }

            if (isEnabled) {
                ExpressiveGroup(title = "") {
                    ACTIONS.forEach { (id, labelRes) ->
                        item {
                            ExpressiveRow(
                                title = stringResource(labelRes),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    selectedAction = id
                                    FpGestureUtils.setAction(context, id)
                                },
                                trailing = {
                                    RadioButton(selected = selectedAction == id, onClick = null)
                                },
                            )
                        }
                    }
                }
                ExpressiveFooter(text = stringResource(R.string.fp_double_tap_footer))

                ExpressiveGroup(title = stringResource(R.string.fp_double_tap_screen_off_title)) {
                    item {
                        ExpressiveSwitchRow(
                            titleText = stringResource(R.string.fp_double_tap_screen_off_enable),
                            summaryText = stringResource(R.string.fp_double_tap_screen_off_summary),
                            isChecked = screenOffEnabled,
                            onChange = {
                                screenOffEnabled = it
                                FpGestureUtils.setScreenOffEnabled(context, it)
                            },
                        )
                    }
                    if (screenOffEnabled) {
                        FpGestureUtils.SCREEN_OFF_ACTIONS.forEach { id ->
                            item {
                                ExpressiveRow(
                                    title = stringResource(FpGestureUtils.getActionLabel(id)),
                                    onClick = {
                                        view.performHapticFeedback(
                                            HapticFeedbackConstants.CONTEXT_CLICK)
                                        screenOffAction = id
                                        FpGestureUtils.setScreenOffAction(context, id)
                                    },
                                    trailing = {
                                        RadioButton(
                                            selected = screenOffAction == id, onClick = null)
                                    },
                                )
                            }
                        }
                    }
                }
                if (screenOffEnabled) {
                    ExpressiveFooter(text = stringResource(R.string.fp_double_tap_screen_off_footer))
                }
            }
        }
    }
}
