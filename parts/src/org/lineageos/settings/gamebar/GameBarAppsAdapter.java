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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.lineageos.settings.R;
import java.util.List;

public class GameBarAppsAdapter extends RecyclerView.Adapter<GameBarAppsAdapter.ViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(ApplicationInfo appInfo);
    }

    private PackageManager packageManager;
    private List<ApplicationInfo> apps;
    private OnAppClickListener listener;

    public GameBarAppsAdapter(PackageManager packageManager, List<ApplicationInfo> apps, OnAppClickListener listener) {
        this.packageManager = packageManager;
        this.apps = apps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GameBarAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_bar_app_selector_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameBarAppsAdapter.ViewHolder holder, int position) {
        final ApplicationInfo appInfo = apps.get(position);
        holder.appName.setText(appInfo.loadLabel(packageManager));
        holder.appPackage.setText(appInfo.packageName);
        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppClick(appInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView appPackage;
        ImageView appIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.app_package);
            appIcon = itemView.findViewById(R.id.app_icon);
        }
    }
}
