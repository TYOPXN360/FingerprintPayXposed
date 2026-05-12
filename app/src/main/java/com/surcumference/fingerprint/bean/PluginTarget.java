package com.surcumference.fingerprint.bean;

import androidx.annotation.IdRes;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;

public enum PluginTarget {
    WeChat(R.id.settings_title_wechat),
    Alipay(R.id.settings_title_alipay);

    @IdRes
    private int mAppNameRes;

    PluginTarget(@IdRes int appNameRes) {
        mAppNameRes = appNameRes;
    }

    public String getAppName() {
        return Lang.getString(mAppNameRes);
    }
}