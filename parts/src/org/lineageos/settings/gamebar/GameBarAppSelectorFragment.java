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

package org.lineageos.settings.gamebar;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.lineageos.settings.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameBarAppSelectorFragment extends Fragment {

    public static final String PREF_AUTO_APPS = "game_bar_auto_apps";

    private RecyclerView recyclerView;
    private GameBarAppsAdapter adapter;
    private PackageManager packageManager;
    private List<ApplicationInfo> allApps;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_bar_app_selector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.app_list);
        packageManager = getContext().getPackageManager();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadApps();
    }

    private void loadApps() {
        allApps = new ArrayList<>();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Set<String> autoApps = getSavedAutoApps();
        for (ApplicationInfo appInfo : installedApps) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 &&
                !appInfo.packageName.equals(getContext().getPackageName()) &&
                !autoApps.contains(appInfo.packageName)) {
                allApps.add(appInfo);
            }
        }
        adapter = new GameBarAppsAdapter(packageManager, allApps, new GameBarAppsAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(ApplicationInfo appInfo) {
                addAppToAutoList(appInfo.packageName);
                Toast.makeText(getContext(), appInfo.loadLabel(packageManager) + " added.", Toast.LENGTH_SHORT).show();
                allApps.remove(appInfo);
                adapter.notifyDataSetChanged();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private Set<String> getSavedAutoApps() {
        return PreferenceManager.getDefaultSharedPreferences(getContext())
                .getStringSet(PREF_AUTO_APPS, new HashSet<>());
    }

    private void addAppToAutoList(String packageName) {
        Set<String> autoApps = new HashSet<>(getSavedAutoApps());
        autoApps.add(packageName);
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putStringSet(PREF_AUTO_APPS, autoApps).apply();
    }
}
