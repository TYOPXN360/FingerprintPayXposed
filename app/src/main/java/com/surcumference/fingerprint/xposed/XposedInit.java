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
    public XposedInit() {
        L.d("XposedInit constructed: instance=" + System.identityHashCode(this));
    }
    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        sInstance = this;
        L.i("========================================");
        L.i("FingerprintPay module loaded!");
        L.i("  Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        L.i("  API version: " + getApiVersion());
        L.i("  Framework: " + getFrameworkName() + " " + getFrameworkVersion());
        L.i("  Process: " + param.getProcessName());
        L.i("  isSystemServer: " + param.isSystemServer());
        L.i("========================================");
    }
    @Override public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam lpparam) {
        String pkg = lpparam.getPackageName();
        boolean isFirst = lpparam.isFirstPackage();
        L.d("onPackageLoaded: pkg=" + pkg + " isFirstPackage=" + isFirst
            + " appInfo=" + lpparam.getApplicationInfo().processName);

        if (PACKAGE_NAME_WECHAT.equals(pkg)) {
            L.i("[目标] 微信包名匹配: " + pkg);
            initWechat(lpparam);
        } else if (PACKAGE_NAME_ALIPAY.equals(pkg)) {
            L.i("[目标] 支付宝包名匹配: " + pkg);
            initAlipay(lpparam);
        } else {
            L.v("非目标包名: " + pkg + " (process=" + lpparam.getApplicationInfo().processName + ")");
        }
        initGeneric(lpparam);
    }
    private void initWechat(XposedModuleInterface.PackageLoadedParam lpparam) {
        try {
            L.i("initWechat: 开始Hook微信...");
            L.d("initWechat: pkg=" + lpparam.getPackageName()
                + " versionCode=" + BuildConfig.VERSION_CODE);
            hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Application app = (Application) chain.getArgs().get(0);
                        L.i("[微信] Instrumentation.callApplicationOnCreate -> onCreate app=" + app);
                        try {
                            new WeChatPlugin().main(app, XposedInit.this, lpparam);
                            L.i("[微信] WeChatPlugin.main 执行完成");
                        } catch (Exception e) {
                            L.e(e, "[微信] WeChatPlugin.main 异常");
                        }
                        return chain.proceed();
                    }
                });
            L.i("initWechat: Hook注册完成 ✓");
        } catch (NoSuchMethodException e) {
            L.e(e, "initWechat: 找不到方法 Instrumentation.callApplicationOnCreate");
        }
    }
    private void initAlipay(XposedModuleInterface.PackageLoadedParam lpparam) {
        try {
            L.i("initAlipay: 开始Hook支付宝...");
            L.d("initAlipay: pkg=" + lpparam.getPackageName()
                + " versionCode=" + BuildConfig.VERSION_CODE);
            hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .intercept(new XposedInterface.Hooker() {
                    private boolean mCalled = false;
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        if (mCalled) {
                            L.v("[支付宝] 已执行过，跳过");
                            return chain.proceed();
                        }
                        mCalled = true;
                        Application app = (Application) chain.getArgs().get(0);
                        L.i("[支付宝] Instrumentation.callApplicationOnCreate -> onCreate app=" + app);
                        try {
                            new AlipayPlugin().main(app, XposedInit.this, lpparam);
                            L.i("[支付宝] AlipayPlugin.main 执行完成");
                        } catch (Exception e) {
                            L.e(e, "[支付宝] AlipayPlugin.main 异常");
                        }
                        return chain.proceed();
                    }
                });
            L.i("initAlipay: Hook注册完成 ✓");
        } catch (NoSuchMethodException e) {
            L.e(e, "initAlipay: 找不到方法 Instrumentation.callApplicationOnCreate");
        }
    }
    private void initGeneric(XposedModuleInterface.PackageLoadedParam lpparam) {
        try {
            String processName = lpparam.getApplicationInfo().processName;
            String pkg = lpparam.getPackageName();
            boolean shouldHook = "android".equals(processName) || PACKAGE_NAME_WECHAT.equals(pkg);
            L.d("initGeneric: processName=" + processName + " pkg=" + pkg + " shouldHook=" + shouldHook);
            if (shouldHook) {
                L.i("initGeneric: 开始Hook ActivityManager.checkComponentPermission");
                hook(ActivityManager.class.getDeclaredMethod("checkComponentPermission",
                        String.class, int.class, int.class, boolean.class))
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .intercept(new XposedInterface.Hooker() {
                        @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            String p = (String) chain.getArgs().get(0);
                            if (!TextUtils.isEmpty(p) && p.contains("MANAGE_USERS")) {
                                L.d("initGeneric: 拦截 MANAGE_USERS 权限检查 -> 授予");
                                return PackageManager.PERMISSION_GRANTED;
                            }
                            return chain.proceed();
                        }
                    });
                L.i("initGeneric: Hook注册完成");
            }
        } catch (NoSuchMethodException e) {
            L.e(e, "initGeneric: 找不到方法 ActivityManager.checkComponentPermission");
        }
    }
}
