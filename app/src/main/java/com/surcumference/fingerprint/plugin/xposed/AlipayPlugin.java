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
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.xposed.XposedInit;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

public class AlipayPlugin {
    @Keep
    public void main(final Application application, final XposedInit module,
                     final XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("[支付宝插件] main() 被调用");
        L.d("[支付宝插件] application=" + application + " module=" + module);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.Alipay);
            L.d("[支付宝插件] PluginApp.setup 完成");

            Toaster.init(application);
            L.d("[支付宝插件] Toaster.init 完成");

            UpdateFactory.lazyUpdateWhenActivityAlive();
            L.d("[支付宝插件] UpdateFactory 完成");

            IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_ALIPAY);
            L.i("[支付宝插件] 插件实现类: " + (plugin != null ? plugin.getClass().getName() : "null"));

            if (plugin == null) {
                L.e("[支付宝插件] PluginFactory.loadPlugin 返回 null! 无法工作");
                return;
            }

            // Hook Activity.onResume - 检测支付弹窗入口
            L.d("[支付宝插件] 注册 Activity.onResume Hook");
            module.hook(Activity.class.getDeclaredMethod("onResume"))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Activity activity = (Activity) chain.getThisObject();
                        String clzName = activity.getClass().getName();
                        L.v("[支付宝] onResume: " + clzName);
                        try {
                            plugin.onActivityResumed(activity);
                        } catch (Exception e) {
                            L.e(e, "[支付宝] onActivityResumed 异常 in " + clzName);
                        }
                        return chain.proceed();
                    }
                });

            // Hook Activity.onCreate - 检测 Activity 创建
            L.d("[支付宝插件] 注册 Activity.onCreate Hook");
            module.hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Activity activity = (Activity) chain.getThisObject();
                        Bundle bundle = (Bundle) chain.getArgs().get(0);
                        String clzName = activity.getClass().getName();
                        L.v("[支付宝] onCreate: " + clzName);
                        try {
                            plugin.onActivityCreated(activity, bundle);
                        } catch (Exception e) {
                            L.e(e, "[支付宝] onActivityCreated 异常 in " + clzName);
                        }
                        return chain.proceed();
                    }
                });

            L.i("[支付宝插件] 所有 Hook 注册完成 ✓");
        } catch (Throwable l) {
            L.e(l, "[支付宝插件] main() 异常");
        }
    }
}
