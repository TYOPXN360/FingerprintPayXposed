package com.surcumference.fingerprint.plugin.xposed;
import android.app.Activity;
import android.app.Application;
import android.os.UserHandle;
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
import com.surcumference.fingerprint.util.Tools;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.xposed.XposedInit;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

public class WeChatPlugin {
    @Keep
    public void main(final Application application, final XposedInit module, final XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.WeChat);
            Toaster.init(application);
            Umeng.init(application);
            UpdateFactory.lazyUpdateWhenActivityAlive();
            IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_WECHAT);
            if (!Tools.isCurrentUserOwner(application)) {
                module.hook(UserHandle.class.getDeclaredMethod("getUserId", int.class))
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .intercept(new XposedInterface.Hooker() {
                        @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            if (plugin.getMockCurrentUser()) return 0;
                            return chain.proceed();
                        }
                    });
            }
            module.hook(Activity.class.getDeclaredMethod("onResume"))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        plugin.onActivityResumed((Activity) chain.getThisObject());
                        return chain.proceed();
                    }
                });
            module.hook(Activity.class.getDeclaredMethod("onPause"))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        plugin.onActivityPaused((Activity) chain.getThisObject());
                        return chain.proceed();
                    }
                });
        } catch (Throwable l) { L.e(l); }
    }
}
