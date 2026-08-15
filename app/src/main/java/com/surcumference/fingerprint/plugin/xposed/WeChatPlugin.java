package com.surcumference.fingerprint.plugin.xposed;
import android.app.Activity;
import android.app.Application;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
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
import com.surcumference.fingerprint.plugin.impl.wechat.WeChatBasePlugin;
import com.surcumference.fingerprint.util.Tools;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.xposed.XposedInit;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

public class WeChatPlugin {
    @Keep
    public void main(final Application application, final XposedInit module,
                     final XposedModuleInterface.PackageLoadedParam lpparam) {
        L.i("[微信插件] main() 被调用");
        L.d("[微信插件] application=" + application + " module=" + module);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.WeChat);
            L.d("[微信插件] PluginApp.setup 完成");

            Toaster.init(application);
            L.d("[微信插件] Toaster.init 完成");

            UpdateFactory.lazyUpdateWhenActivityAlive();
            L.d("[微信插件] UpdateFactory 完成");

            IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_WECHAT);
            L.i("[微信插件] 插件实现类: " + (plugin != null ? plugin.getClass().getName() : "null"));

            if (plugin == null) {
                L.e("[微信插件] PluginFactory.loadPlugin 返回 null! 无法工作");
                return;
            }

            // 多用户支持
            if (!Tools.isCurrentUserOwner(application)) {
                L.d("[微信插件] 非主用户空间, 注册 getUserId Hook");
                module.hook(UserHandle.class.getDeclaredMethod("getUserId", int.class))
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .intercept(new XposedInterface.Hooker() {
                        @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            if (plugin.getMockCurrentUser()) {
                                L.d("[微信] getUserId 返回 0 (Mock)");
                                return 0;
                            }
                            return chain.proceed();
                        }
                    });
            }

            // Activity.onResume
            L.d("[微信插件] 注册 Activity.onResume Hook");
            module.hook(Activity.class.getDeclaredMethod("onResume"))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Activity activity = (Activity) chain.getThisObject();
                        String clzName = activity.getClass().getName();
                        L.v("[微信] onResume: " + clzName);
                        try {
                            plugin.onActivityResumed(activity);
                        } catch (Exception e) {
                            L.e(e, "[微信] onActivityResumed 异常 in " + clzName);
                        }
                        return chain.proceed();
                    }
                });

            // Activity.onPause
            L.d("[微信插件] 注册 Activity.onPause Hook");
            module.hook(Activity.class.getDeclaredMethod("onPause"))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Activity activity = (Activity) chain.getThisObject();
                        String clzName = activity.getClass().getName();
                        L.v("[微信] onPause: " + clzName);
                        try {
                            plugin.onActivityPaused(activity);
                        } catch (Exception e) {
                            L.e(e, "[微信] onActivityPaused 异常 in " + clzName);
                        }
                        return chain.proceed();
                    }
                });

            L.i("[微信插件] 所有 Hook 注册完成 ✓");

            // PullDownListView.onItemLongClick - 长按设置入口
            try {
                Class<?> pullDownClass = application.getClassLoader().loadClass(
                        "com.tencent.mm.ui.widget.listview.PullDownListView");
                module.hook(pullDownClass.getDeclaredMethod("onItemLongClick",
                                AdapterView.class, View.class, int.class, long.class))
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .intercept(new XposedInterface.Hooker() {
                        @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            try {
                                View view = (View) chain.getArgs().get(1);
                                if (plugin instanceof WeChatBasePlugin && WeChatBasePlugin.isSettingsView(view)) {
                                    ((WeChatBasePlugin) plugin).showSettingsDialog(view.getContext());
                                    return true;
                                }
                            } catch (Exception e) {
                                L.e(e, "[微信] PullDownListView.onItemLongClick 异常");
                            }
                            return chain.proceed();
                        }
                    });
                L.i("[微信插件] PullDownListView.onItemLongClick Hook 注册完成 ✓");
            } catch (Exception e) {
                L.d("[微信插件] PullDownListView Hook 跳过: " + e.getMessage());
            }

            // MyKeyboardWindow.setInputEditText - 支付键盘检测
            // 兼容新旧版微信键盘类:
            //  - 旧版: com.tenpay.android.wechat.MyKeyboardWindow
            //  - 8.0.7x+: com.tenpay.wphk.HkWxKeyboardWindow / com.tenpay.miniapp.MiniAppKeyboardWindow
            //    (这些新键盘类不继承 MyKeyboardWindow, 各自实现了 setInputEditText)
            String[] keyboardClassNames = new String[]{
                    "com.tenpay.android.wechat.MyKeyboardWindow",
                    "com.tenpay.wphk.HkWxKeyboardWindow",
                    "com.tenpay.miniapp.MiniAppKeyboardWindow",
            };
            for (String keyboardClassName : keyboardClassNames) {
                try {
                    Class<?> keyboardClass = application.getClassLoader().loadClass(keyboardClassName);
                    module.hook(keyboardClass.getDeclaredMethod("setInputEditText", EditText.class))
                        .setPriority(XposedInterface.PRIORITY_DEFAULT)
                        .intercept(new XposedInterface.Hooker() {
                            @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                                L.i("[微信] " + keyboardClassName + ".setInputEditText 触发! this=" + chain.getThisObject().getClass().getName());
                                try {
                                    if (plugin instanceof WeChatBasePlugin) {
                                        ViewGroup keyboardView = (ViewGroup) chain.getThisObject();
                                        L.i("[微信] 调用 handleKeyboardSetup, keyboardView=" + keyboardView.getClass().getName());
                                        ((WeChatBasePlugin) plugin).handleKeyboardSetup(keyboardView);
                                    } else {
                                        L.w("[微信] plugin 不是 WeChatBasePlugin 实例: " + plugin.getClass().getName());
                                    }
                                } catch (Exception e) {
                                    L.e(e, "[微信] " + keyboardClassName + ".setInputEditText 异常");
                                }
                                return chain.proceed();
                            }
                        });
                    L.i("[微信插件] " + keyboardClassName + ".setInputEditText Hook 注册完成 ✓");
                } catch (Throwable e) {
                    L.d("[微信插件] " + keyboardClassName + " Hook 跳过: " + e.getMessage());
                }
            }

        } catch (Throwable l) {
            L.e(l, "[微信插件] main() 异常");
        }
    }
}
