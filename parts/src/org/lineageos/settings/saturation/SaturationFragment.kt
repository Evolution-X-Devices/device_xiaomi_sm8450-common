/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.saturation

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.settingslib.widget.LayoutPreference
import org.lineageos.settings.Constants
import org.lineageos.settings.CustomSeekBarPreference
import org.lineageos.settings.R
import org.lineageos.settings.utils.TileUtils

class SaturationFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private var mViewArrowPrevious: View? = null
    private var mViewArrowNext: View? = null
    private var mViewPager: ViewPager? = null
    private var mDotIndicators: Array<ImageView>? = null
    private var mViewPagerImages: Array<View?>? = null
    private var mSaturationPreference: CustomSeekBarPreference? = null
    private var mSurfaceFlinger: IBinder? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.saturation, rootKey)
        setHasOptionsMenu(true)

        val preview = findPreference<LayoutPreference>(Constants.KEY_SATURATION_PREVIEW)
        preview?.let { addViewPager(it) }

        val sharedPrefs = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        mSaturationPreference = findPreference(Constants.KEY_SATURATION)
        mSaturationPreference?.setOnPreferenceChangeListener(this)

        val seekBarValue = sharedPrefs?.getInt(Constants.KEY_SATURATION, 100) ?: 100
        updateSaturation(seekBarValue)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.saturation_menu, menu)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference == mSaturationPreference) {
            val seekBarValue = newValue as Int
            updateSaturation(seekBarValue)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.add_tile) {
            TileUtils.requestAddTileService(
                context,
                SaturationTileService::class.java,
                R.string.saturation_title,
                R.drawable.ic_saturation_tile
            )
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateSaturation(seekBarValue: Int) {
        val saturation = if (seekBarValue == 100) 1.001f else seekBarValue / 100.0f
        mSurfaceFlinger?.let {
            try {
                val data = Parcel.obtain()
                data.writeInterfaceToken("android.ui.ISurfaceComposer")
                data.writeFloat(saturation)
                it.transact(1022, data, null, 0)
                data.recycle()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger")
    }

    fun restoreSaturationSetting(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val seekBarValue = sharedPrefs.getInt(Constants.KEY_SATURATION, 100)
        updateSaturation(seekBarValue)
    }

    private fun addViewPager(preview: LayoutPreference) {
        mViewPager = preview.findViewById(R.id.viewpager)
        val drawables = intArrayOf(
            R.drawable.image_preview1,
            R.drawable.image_preview2,
            R.drawable.image_preview3
        )
        mViewPagerImages = Array(drawables.size) { getLayoutInflater().inflate(R.layout.image_layout, null) }
        for (idx in drawables.indices) {
            mViewPagerImages?.get(idx)?.let { imageView ->
                imageView.findViewById<ImageView>(R.id.imageView)?.setImageResource(drawables[idx])
            }
        }
        mViewPager?.adapter = ImagePreviewPagerAdapter(mViewPagerImages!!)
        mViewArrowPrevious = preview.findViewById(R.id.arrow_previous)
        mViewArrowPrevious?.setOnClickListener { mViewPager?.setCurrentItem(mViewPager?.currentItem?.minus(1) ?: 0, true) }
        mViewArrowNext = preview.findViewById(R.id.arrow_next)
        mViewArrowNext?.setOnClickListener { mViewPager?.setCurrentItem(mViewPager?.currentItem?.plus(1) ?: 0, true) }
        mViewPager?.addOnPageChangeListener(createPageListener())
        val viewGroup = preview.findViewById<ViewGroup>(R.id.viewGroup)
        mDotIndicators = Array(mViewPagerImages?.size ?: 0) { ImageView(context) }
        mDotIndicators?.forEach { dot ->
            val lp = ViewGroup.MarginLayoutParams(12, 12)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            viewGroup.addView(dot)
        }
        updateIndicator(mViewPager?.currentItem ?: 0)
    }

    private fun createPageListener(): ViewPager.OnPageChangeListener {
        return object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset != 0f) {
                    mViewPagerImages?.forEach { it?.visibility = View.VISIBLE }
                } else {
                    mViewPagerImages?.get(position)?.setContentDescription(getString(R.string.image_preview_content_description))
                    updateIndicator(position)
                }
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        }
    }

    private fun updateIndicator(position: Int) {
        mViewPagerImages?.forEachIndexed { i, view ->
            mDotIndicators?.get(i)?.setBackgroundResource(
                if (position == i) R.drawable.ic_image_preview_page_indicator_focused
                else R.drawable.ic_image_preview_page_indicator_unfocused
            )
            view?.visibility = if (position == i) View.VISIBLE else View.INVISIBLE
        }
        mViewArrowPrevious?.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        mViewArrowNext?.visibility = if (position == (mViewPagerImages?.size ?: 0) - 1) View.INVISIBLE else View.VISIBLE
    }

    private class ImagePreviewPagerAdapter(private val mPageViewList: Array<View?>) : PagerAdapter() {
        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            mPageViewList[position]?.let { container.removeView(it) }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            mPageViewList[position]?.let { container.addView(it) }
            return mPageViewList[position]!!
        }

        override fun getCount(): Int = mPageViewList.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean = `object` == view
    }
}
