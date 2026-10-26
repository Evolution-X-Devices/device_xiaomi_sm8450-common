/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.preference.PreferenceManager
import org.lineageos.settings.Constants

/** Display saturation: stored in the default shared preferences, applied through SurfaceFlinger. */
object SaturationUtils {
    private const val TAG = "SaturationUtils"
    private const val DEFAULT = 100
    private const val TRANSACTION_SET_SATURATION = 1022
    private const val INTERFACE = "android.ui.ISurfaceComposer"

    @JvmStatic
    fun get(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context).getInt(Constants.KEY_SATURATION, DEFAULT)

    @JvmStatic
    fun save(context: Context, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putInt(Constants.KEY_SATURATION, value).apply()
    }

    /** Applies the saved value, e.g. after boot. */
    @JvmStatic
    fun restore(context: Context) = apply(get(context))

    @JvmStatic
    fun apply(value: Int) {
        val saturation = if (value == DEFAULT) 1.001f else value / 100.0f
        val surfaceFlinger: IBinder? = ServiceManager.getService("SurfaceFlinger")
        if (surfaceFlinger == null) {
            Log.e(TAG, "SurfaceFlinger service not found")
            return
        }
        val data = Parcel.obtain()
        try {
            data.writeInterfaceToken(INTERFACE)
            data.writeFloat(saturation)
            surfaceFlinger.transact(TRANSACTION_SET_SATURATION, data, null, 0)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to apply saturation", e)
        } finally {
            data.recycle()
        }
    }
}
