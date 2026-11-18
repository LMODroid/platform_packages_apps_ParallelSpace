package com.libremobileos.parallelspace;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.ArrayMap;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.libremobileos.app.ParallelSpaceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpaceAppViewModel extends AndroidViewModel {
    private final SpaceAppListLiveData mLiveData;

    public SpaceAppViewModel(Application application, int userId) {
        super(application);
        mLiveData = new SpaceAppListLiveData(application, userId);
    }

    public LiveData<List<SpaceAppInfo>> getAppList() {
        return mLiveData;
    }
}

class SpaceAppListLiveData extends LiveData<List<SpaceAppInfo>> {

    private final PackageManager mPackageManager;
    private final ParallelSpaceManager mParallelSpaceManager;
    private final Context mContext;
    private int mCurrentDataVersion;

    public SpaceAppListLiveData(Context context, int userId) {
        mPackageManager = context.getPackageManager();
        mParallelSpaceManager = ParallelSpaceManager.getInstance();
        mContext = context;
        loadSupportedAppData(userId);
    }

    void loadSupportedAppData(int userId) {
        final int dataVersion = ++mCurrentDataVersion;

        Thread thread = new Thread(() -> {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER);
            // Also match hidden packages, they are filtered out of queries otherwise.
            final int flags = PackageManager.MATCH_UNINSTALLED_PACKAGES;
            List<ResolveInfo> ownerInfos = mPackageManager.queryIntentActivities(mainIntent, flags);
            List<ResolveInfo> spaceInfos =
                    mPackageManager.queryIntentActivitiesAsUser(mainIntent, flags, userId);

            // Packages currently installed in the space, including hidden ones.
            Map<String, ResolveInfo> spaceApps = new ArrayMap<>();
            for (ResolveInfo info : spaceInfos) {
                if (isInstalled(info)) {
                    spaceApps.putIfAbsent(info.activityInfo.packageName, info);
                }
            }

            Map<String, SpaceAppInfo> apps = new LinkedHashMap<>();
            for (ResolveInfo info : ownerInfos) {
                // Apps hidden or uninstalled in the owner are not offered for cloning.
                if (isInstalled(info) && !isHidden(info)) {
                    addApp(apps, info, spaceApps.get(info.activityInfo.packageName), userId);
                }
            }
            // Keep existing clones listed even if the original is hidden or gone,
            // so they can still be managed.
            for (ResolveInfo info : spaceApps.values()) {
                addApp(apps, info, info, userId);
            }

            if (dataVersion == mCurrentDataVersion) {
                postValue(new ArrayList<>(apps.values()));
            }
        });
        thread.start();
    }

    private void addApp(Map<String, SpaceAppInfo> apps, ResolveInfo info,
            ResolveInfo spaceInfo, int userId) {
        String packageName = info.activityInfo.packageName;
        if (apps.containsKey(packageName) ||
                info.activityInfo.applicationInfo.isSystemApp() ||
                packageName.equals(mContext.getPackageName()))
            return;
        boolean isHidden = spaceInfo != null && isHidden(spaceInfo);
        boolean isDuplicated = spaceInfo != null && !isHidden;
        apps.put(packageName, new SpaceAppInfo(info, mPackageManager, mParallelSpaceManager,
                userId, isDuplicated, isHidden));
    }

    private static boolean isInstalled(ResolveInfo info) {
        return (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
    }

    private static boolean isHidden(ResolveInfo info) {
        return (info.activityInfo.applicationInfo.privateFlags
                & ApplicationInfo.PRIVATE_FLAG_HIDDEN) != 0;
    }
}