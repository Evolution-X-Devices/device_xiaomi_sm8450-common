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

public class GameBarAppRemoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private GameBarAutoAppsAdapter adapter;
    private PackageManager packageManager;
    private List<ApplicationInfo> autoAppsList;

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
        loadAutoApps();
    }

    private void loadAutoApps() {
        Set<String> autoAppsSet = getSavedAutoApps();
        autoAppsList = new ArrayList<>();
        for (String pkg : autoAppsSet) {
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(pkg, 0);
                autoAppsList.add(info);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        adapter = new GameBarAutoAppsAdapter(packageManager, autoAppsList, new GameBarAutoAppsAdapter.OnAppRemoveListener() {
            @Override
            public void onAppRemove(ApplicationInfo appInfo) {
                removeAppFromAutoList(appInfo.packageName);
                Toast.makeText(getContext(), appInfo.loadLabel(packageManager) + " removed.", Toast.LENGTH_SHORT).show();
                autoAppsList.remove(appInfo);
                adapter.notifyDataSetChanged();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private Set<String> getSavedAutoApps() {
        return PreferenceManager.getDefaultSharedPreferences(getContext())
                .getStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, new HashSet<>());
    }

    private void removeAppFromAutoList(String packageName) {
        Set<String> autoApps = new HashSet<>(getSavedAutoApps());
        autoApps.remove(packageName);
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, autoApps).apply();
    }
}
