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
import com.surcumference.fingerprint.xposed.loader.XposedPluginLoader;
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
            .priority(XposedInterface.PRIORITY_DEFAULT)
            .hooker(new XposedInterface.Hooker() {
                @Override public void beforeHookedMethod(XposedInterface.Chain chain) {
                    L.v("Application onCreate");
                    Application app = (Application) chain.getArgs()[0];
                    XposedPluginLoader.load(WeChatPlugin.class, app, lpparam);
                }
            });
    }
    private void initAlipay(XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("loaded: [" + lpparam.getPackageName() + "]" + " version:" + BuildConfig.VERSION_NAME);
        hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
            .priority(XposedInterface.PRIORITY_DEFAULT)
            .hooker(new XposedInterface.Hooker() {
                private boolean mCalled = false;
                @Override public void beforeHookedMethod(XposedInterface.Chain chain) {
                    L.v("Application onCreate");
                    if (!mCalled) {
                        mCalled = true;
                        Application app = (Application) chain.getArgs()[0];
                        XposedPluginLoader.load(AlipayPlugin.class, app, lpparam);
                    }
                }
            });
    }
    private void initGeneric(XposedModuleInterface.PackageLoadedParam lpparam) {
        if ("android".equals(lpparam.getProcessName()) || PACKAGE_NAME_WECHAT.equals(lpparam.getPackageName())) {
            hook(ActivityManager.class.getDeclaredMethod("checkComponentPermission", String.class, int.class, int.class, boolean.class))
                .priority(XposedInterface.PRIORITY_DEFAULT)
                .hooker(new XposedInterface.Hooker() {
                    @Override public void beforeHookedMethod(XposedInterface.Chain chain) {
                        String p = (String) chain.getArgs()[0];
                        if (!TextUtils.isEmpty(p) && p.contains("MANAGE_USERS")) chain.setResult(PackageManager.PERMISSION_GRANTED);
                    }
                });
        }
    }
}
