/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.settings.R
import org.lineageos.settings.thermal.ThermalUtils.ThermalState
import org.lineageos.settings.utils.dlog
import com.android.settingslib.widget.MainSwitchPreference

/**
 * Thermal profile settings fragment. Shows list of all launchable apps with a drop-down spinner to
 * select their thermal profile.
 */
class ThermalSettingsFragment : PreferenceFragment() {

    private lateinit var appsAdapter: AppsAdapter
    private lateinit var launcherApps: LauncherApps
    private lateinit var thermalUtils: ThermalUtils
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var mainSwitch: MainSwitchPreference
    private lateinit var loadingView: View
    private var isLoaded = false
    private val handlerThread = HandlerThread(TAG).apply { start() }
    private val bgHandler = Handler(handlerThread.looper)

    private val launcherAppsCallback =
        object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                appsAdapter.run {
                    val pos = entries.indexOfFirst { it.packageName == packageName }
                    if (pos != -1) {
                        dlog(TAG, "onPackageRemoved: $packageName")
                        entries.removeAt(pos)
                        notifyItemRemovedOnUiThread(pos)
                    }
                }
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                val info = launcherApps.getActivityList(packageName, user).firstOrNull() ?: return
                val entry = info.toAppEntry()
                appsAdapter.run {
                    if (entries.any { it.packageName == packageName }) return
                    val index = entries.binarySearchBy(entry.label) { it.label }
                    val pos = if (index < 0) -(index + 1) else index
                    entries.add(pos, entry)
                    dlog(TAG, "onPackageAdded: $packageName")
                    notifyItemInsertedOnUiThread(pos)
                }
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {}

            override fun onPackagesAvailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean
            ) {}

            override fun onPackagesUnavailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean
            ) {}
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.thermal_settings)

        thermalUtils = ThermalUtils.getInstance(activity)
        mainSwitch =
            findPreference<MainSwitchPreference>(THERMAL_ENABLE_KEY)!!.apply {
                isChecked = thermalUtils.enabled
                addOnSwitchChangeListener { _, isChecked ->
                    thermalUtils.enabled = isChecked
                    updateRvVisibility()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = activity.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        appsAdapter = AppsAdapter(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.thermal_layout, container, false)
        val prefsContainer = view.findViewById<ViewGroup>(android.R.id.list_container)
        val prefsView = super.onCreateView(inflater, prefsContainer, savedInstanceState)
        prefsContainer.addView(prefsView)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appsRecyclerView =
            view.findViewById<RecyclerView>(R.id.thermal_rv_view)!!.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = appsAdapter
            }
        loadingView = view.findViewById(R.id.thermal_loading)!!
        updateRvVisibility()
        loadApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        dlog(TAG, "onDestroy")
        handlerThread.quitSafely()
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    private fun updateRvVisibility() {
        activity?.runOnUiThread {
            appsRecyclerView.isVisible = thermalUtils.enabled && isLoaded
            loadingView.isVisible = thermalUtils.enabled && !isLoaded
        }
    }

    private fun loadApps() {
        bgHandler.post {
            val appEntries =
                launcherApps
                    .getActivityList(null, Process.myUserHandle())
                    .distinctBy { it.componentName.packageName }
                    .map { it.toAppEntry() }
                    .sortedBy { it.label.lowercase() }
            
            dlog(TAG, "loaded ${appEntries.size} apps")
            appsAdapter.run {
                entries.clear()
                entries.addAll(appEntries)
                notifyDataSetChangedOnUiThread()
            }
            isLoaded = true
            updateRvVisibility()
            launcherApps.registerCallback(launcherAppsCallback, bgHandler)
        }
    }

    private fun LauncherActivityInfo.toAppEntry() =
        AppEntry(
            packageName = componentName.packageName,
            label = label.toString(),
            icon = getIcon(0),
            state = thermalUtils.getStateForPackage(componentName.packageName)
        )

    private data class AppEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        var state: ThermalState,
    )

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.app_name)!!
        val mode: Spinner = view.findViewById(R.id.app_mode)!!
        val icon: ImageView = view.findViewById(R.id.app_icon)!!

        init {
            view.tag = this
        }
    }

    private inner class ModeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val items = ThermalState.values().map { it.label }

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (convertView as? TextView
                    ?: inflater.inflate(
                        android.R.layout.simple_spinner_dropdown_item,
                        parent,
                        false
                    ) as TextView)
                .apply {
                    setText(items[position])
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                }
    }

    private inner class AppsAdapter(private val activity: Activity) :
        RecyclerView.Adapter<ViewHolder>(), AdapterView.OnItemSelectedListener {

        var entries = mutableListOf<AppEntry>()
        private val modeAdapter = ModeAdapter(activity)

        override fun getItemCount() = entries.size
        override fun getItemId(position: Int) = entries[position].packageName.hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.thermal_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.run {
                mode.apply {
                    adapter = modeAdapter
                    onItemSelectedListener = null
                    setSelection(entry.state.id, false)
                    onItemSelectedListener = this@AppsAdapter
                    tag = entry
                }
                title.apply {
                    text = entry.label
                    setOnClickListener { mode.performClick() }
                }
                icon.setImageDrawable(entry.icon)
            }
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val entry = parent?.tag as? AppEntry ?: return
            
            if (entry.state.id != position) {
                dlog(TAG, "Selected position $position for ${entry.packageName}")
                thermalUtils.writePackage(entry.packageName, position)
                
                entry.state = ThermalState.values()[position]
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        fun notifyDataSetChangedOnUiThread() {
            activity.runOnUiThread { notifyDataSetChanged() }
        }

        fun notifyItemInsertedOnUiThread(position: Int) {
            activity.runOnUiThread { notifyItemInserted(position) }
        }

        fun notifyItemRemovedOnUiThread(position: Int) {
            activity.runOnUiThread { notifyItemRemoved(position) }
        }
    }

    companion object {
        private const val TAG = "ThermalSettingsFragment"
        private const val THERMAL_ENABLE_KEY = "thermal_enable"
    }
}
