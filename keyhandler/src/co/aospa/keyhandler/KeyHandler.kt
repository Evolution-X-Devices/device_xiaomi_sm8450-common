/*
 * Copyright (C) 2023 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.aospa.keyhandler

import android.content.Context
import android.view.KeyEvent
import android.util.Log

import com.android.internal.os.DeviceKeyHandler

class KeyHandler(
    private val context: Context
) : DeviceKeyHandler {

    private val hasSideFps = context.resources.getBoolean(
            com.android.internal.R.bool.config_is_powerbutton_fps)
    private val fpDoubleTapHandler = if (hasSideFps) FpDoubleTapHandler(context) else null

    override fun handleKeyEvent(event: KeyEvent): KeyEvent? {
        return when (event.scanCode) {
            KEYCODE_FP_DOUBLE_TAP_FPC,
            KEYCODE_FP_DOUBLE_TAP_GOODIX -> {
                fpDoubleTapHandler?.handleEvent(event).also {
                    dlog("handled fp double tap event: $event")
                }
                null
            }
            else -> event
        }
    }

    companion object {
        private const val TAG = "XiaomiKeyHandler"
        private const val KEYCODE_FP_DOUBLE_TAP_FPC = 106
        private const val KEYCODE_FP_DOUBLE_TAP_GOODIX = 306

        fun dlog(msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, msg)
            }
        }
    }

}
