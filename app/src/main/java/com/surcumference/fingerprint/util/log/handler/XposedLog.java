package com.surcumference.fingerprint.util.log.handler;

import android.util.Log;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.inf.ILog;
import com.surcumference.fingerprint.xposed.XposedInit;

public class XposedLog implements ILog {
    @Override public void v(String tag, String msg) { log(Log.VERBOSE, tag, msg); }
    @Override public void d(String tag, String msg) { log(Log.DEBUG, tag, msg); }
    @Override public void i(String tag, String msg) { log(Log.INFO, tag, msg); }
    @Override public void w(String tag, String msg) { log(Log.WARN, tag, msg); }
    @Override public void e(String tag, String msg) { log(Log.ERROR, tag, msg); Umeng.reportError(tag + " " + msg); }
    private void log(int priority, String tag, String msg) {
        try {
            if (XposedInit.sInstance != null) XposedInit.sInstance.log(priority, tag, msg);
            else androidFallback(priority, tag, msg);
        } catch (Throwable t) {
            androidFallback(priority, tag, msg);
        }
    }
    private void androidFallback(int priority, String tag, String msg) {
        if (priority >= Log.WARN) Log.w(tag, msg); else Log.d(tag, msg);
    }
}
