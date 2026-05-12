package com.surcumference.fingerprint.xposed.loader;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.github.libxposed.api.XposedModule;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedPluginLoader {

    private static Map<Class, Object> sPluginCache = new HashMap<>();

    public static void load(Class pluginClz, Application application, XposedModuleInterface.PackageLoadedParam lpparam) throws Exception {
        Object pluginObj;
        if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
            synchronized (pluginClz) {
                if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
                    pluginObj = loadFromLocal(pluginClz);
                    sPluginCache.put(pluginClz, pluginObj);
                }
            }
        }
        callPluginMain(pluginObj, application, lpparam);
    }

    private static Object loadFromLocal(Class pluginClz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return pluginClz.newInstance();
    }

    private static void callPluginMain(Object pluginObj, Context context, XposedModuleInterface.PackageLoadedParam lpparam) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Method method = pluginObj.getClass().getDeclaredMethod("main", Application.class, XposedModuleInterface.PackageLoadedParam.class);
        method.invoke(pluginObj, context, lpparam);
    }

}
