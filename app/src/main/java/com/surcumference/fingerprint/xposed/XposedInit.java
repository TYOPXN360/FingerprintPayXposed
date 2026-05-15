package com.surcumference.fingerprint.xposed;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_ALIPAY;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_WECHAT;
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

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
@Keep public class XposedInit extends XposedModule {
    public static XposedInit sInstance;
    public XposedInit() {}
    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        L.i("Module loaded, API version: " + getApiVersion());
    }
    @Override public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam lpparam) {
        if (PACKAGE_NAME_WECHAT.equals(lpparam.getPackageName())) initWechat(lpparam);
        else if (PACKAGE_NAME_ALIPAY.equals(lpparam.getPackageName())) initAlipay(lpparam);
        initGeneric(lpparam);
    }
    private void initWechat(XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("loaded: [" + lpparam.getPackageName() + "]" + " version:" + BuildConfig.VERSION_NAME);
        hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
            .setPriority(XposedInterface.PRIORITY_DEFAULT)
            .intercept(new XposedInterface.Hooker() {
                @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    L.v("Application onCreate");
                    Application app = (Application) chain.getArgs().get(0);
                    new WeChatPlugin().main(app, XposedInit.this, lpparam);
                    return chain.proceed();
                }
            });
    }
    private void initAlipay(XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("loaded: [" + lpparam.getPackageName() + "]" + " version:" + BuildConfig.VERSION_NAME);
        hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
            .setPriority(XposedInterface.PRIORITY_DEFAULT)
            .intercept(new XposedInterface.Hooker() {
                private boolean mCalled = false;
                @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    L.v("Application onCreate");
                    if (!mCalled) {
                        mCalled = true;
                        Application app = (Application) chain.getArgs().get(0);
                        new AlipayPlugin().main(app, XposedInit.this, lpparam);
                    }
                    return chain.proceed();
                }
            });
    }
    private void initGeneric(XposedModuleInterface.PackageLoadedParam lpparam) {
        if ("android".equals(lpparam.getProcessName()) || PACKAGE_NAME_WECHAT.equals(lpparam.getPackageName())) {
            hook(ActivityManager.class.getDeclaredMethod("checkComponentPermission", String.class, int.class, int.class, boolean.class))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        String p = (String) chain.getArgs().get(0);
                        if (!TextUtils.isEmpty(p) && p.contains("MANAGE_USERS")) return PackageManager.PERMISSION_GRANTED;
                        return chain.proceed();
                    }
                });
        }
    }
}
