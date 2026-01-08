/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.utils

import android.util.Log

private const val MAIN_TAG = "XMParts"

fun dlog(tag: String, msg: String) {
    if (Log.isLoggable(MAIN_TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
        Log.d("$MAIN_TAG-$tag", msg)
    }
}
