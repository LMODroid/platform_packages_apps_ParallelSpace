package com.libremobileos.parallelspace;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.libremobileos.app.ParallelSpaceManager;

public class SpaceAppInfo {

    private final String mLabel;
    private final String mPackageName;
    private final Drawable mIcon;
    private final ParallelSpaceManager mParallelSpaceManager;
    private final int mUserId;
    private boolean mAppDuplicated;
    private boolean mAppHidden;

    SpaceAppInfo(ResolveInfo info, PackageManager pm, ParallelSpaceManager psm, int userId,
            boolean isDuplicated, boolean isHidden) {
        mLabel = info.loadLabel(pm).toString();
        mIcon = info.loadIcon(pm);
        mPackageName = info.activityInfo.packageName;
        mParallelSpaceManager = psm;
        mUserId = userId;
        mAppDuplicated = isDuplicated;
        mAppHidden = isHidden;
    }

    String getLabel() {
        return mLabel;
    }

    String getPackageName() {
        return mPackageName;
    }

    boolean isAppDuplicated() {
        return mAppDuplicated;
    }

    /** Whether the clone exists but was hidden, e.g. from its app info page. */
    boolean isAppHidden() {
        return mAppHidden;
    }

    void setDuplicateApp(boolean duplicate) {
        if (duplicate)
            mParallelSpaceManager.duplicatePackage(mPackageName, mUserId);
        else
            mParallelSpaceManager.removePackage(mPackageName, mUserId);

        mAppDuplicated = duplicate;
        // Duplicating unhides an existing clone, removing deletes it.
        mAppHidden = false;
    }

    Drawable getIcon() {
        return mIcon;
    }

    @Override
    public String toString() {
        return mLabel;
    }
}
