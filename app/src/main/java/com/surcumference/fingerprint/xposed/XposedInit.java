package com.surcumference.fingerprint.xposed;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_ALIPAY;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_WECHAT;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.Keep;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.plugin.xposed.AlipayPlugin;
import com.surcumference.fingerprint.plugin.xposed.WeChatPlugin;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.xposed.loader.XposedPluginLoader;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@Keep
public class XposedInit implements IXposedHookZygoteInit, IXposedHookLoadPackage {


    public void initZygote(StartupParam startupParam) throws Throwable {
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (PACKAGE_NAME_WECHAT.equals(lpparam.packageName)) {
            initWechat(lpparam);
        } else if (PACKAGE_NAME_ALIPAY.equals(lpparam.packageName)) {
            initAlipay(lpparam);
        }

        initGeneric(lpparam);
    }

    private void initWechat(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                Application application = (Application) param.args[0];
                XposedPluginLoader.load(WeChatPlugin.class, application, lpparam);
            }
        });
    }

    private void initAlipay(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            private boolean mCalled = false;
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (mCalled == false) {
                    mCalled = true;
                    Application application = (Application) param.args[0];
                    XposedPluginLoader.load(AlipayPlugin.class, application, lpparam);
                }
            }
        });
    }

    private void initGeneric(final LoadPackageParam lpparam) {
        //for multi user
        if ("android".equals(lpparam.processName)
                || PACKAGE_NAME_WECHAT.equals(lpparam.packageName)
) {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "checkComponentPermission", String.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String permission = (String) param.args[0];
                    if (TextUtils.isEmpty(permission)) {
                        return;
                    }
                    if (!permission.contains("MANAGE_USERS")) {
                        return;
                    }
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                }
            });
        }
    }
}