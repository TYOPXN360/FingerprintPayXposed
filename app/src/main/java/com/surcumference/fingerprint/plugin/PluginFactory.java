package com.surcumference.fingerprint.plugin;

import android.content.Context;

import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.plugin.impl.alipay.AlipayBasePlugin;
import com.surcumference.fingerprint.plugin.impl.wechat.WeChatBasePlugin;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.util.ApplicationUtils;

public class PluginFactory {
    public static IAppPlugin loadPlugin(Context context, String packageName) {
        int versionCode = ApplicationUtils.getPackageVersionCode(context, packageName);
        switch (packageName) {
            case Constant.PACKAGE_NAME_ALIPAY:
                return new AlipayBasePlugin();
            case Constant.PACKAGE_NAME_WECHAT:
                return new WeChatBasePlugin();
        }
        throw new RuntimeException("Unsupported package: " + packageName);
    }
}
