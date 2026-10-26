/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import org.lineageos.settings.auxcamera.AuxCameraUtils
import org.lineageos.settings.saturation.SaturationUtils
import org.lineageos.settings.thermal.ThermalUtils

/**
 * Live subtitles for the Parts entries in Settings. Settings calls the method named by the first
 * path segment of the `com.android.settings.summary_uri` meta-data and shows the returned
 * `com.android.settings.summary` instead of the static summary.
 */
class SummaryProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val summary = when (method) {
            METHOD_THERMAL -> context.getString(
                if (ThermalUtils.getInstance(context).enabled) R.string.tile_on else R.string.tile_off
            )
            METHOD_AUX_CAMERA -> context.getString(
                if (AuxCameraUtils.isEnabled(context)) R.string.tile_on else R.string.tile_off
            )
            METHOD_SATURATION -> "${SaturationUtils.get(context)}%"
            else -> return null
        }
        return Bundle().apply { putString(KEY_SUMMARY, summary) }
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ): Int = 0

    companion object {
        private const val KEY_SUMMARY = "com.android.settings.summary"
        private const val METHOD_THERMAL = "thermalSummary"
        private const val METHOD_AUX_CAMERA = "auxCameraSummary"
        private const val METHOD_SATURATION = "saturationSummary"
    }
}
