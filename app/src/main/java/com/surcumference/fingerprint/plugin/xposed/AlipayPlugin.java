package com.surcumference.fingerprint.plugin.xposed;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.Keep;
import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.update.UpdateFactory;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.plugin.PluginFactory;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.xposed.XposedInit;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

public class AlipayPlugin {
    @Keep
    public void main(final Application application, final XposedInit module, final XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.Alipay);
            Toaster.init(application);
            Umeng.init(application);
            UpdateFactory.lazyUpdateWhenActivityAlive();
            IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_ALIPAY);
            module.hook(Activity.class.getDeclaredMethod("onResume"))
                .priority(XposedInterface.PRIORITY_DEFAULT)
                .hooker(new XposedInterface.Hooker() {
                    @Override public void beforeHookedMethod(XposedInterface.Chain chain) {
                        plugin.onActivityResumed((Activity) chain.getThisObject());
                    }
                });
            module.hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class))
                .priority(XposedInterface.PRIORITY_DEFAULT)
                .hooker(new XposedInterface.Hooker() {
                    @Override public void beforeHookedMethod(XposedInterface.Chain chain) {
                        plugin.onActivityCreated((Activity) chain.getThisObject(), (Bundle) chain.getArgs()[0]);
                    }
                });
        } catch (Throwable l) { L.e(l); }
    }
}
