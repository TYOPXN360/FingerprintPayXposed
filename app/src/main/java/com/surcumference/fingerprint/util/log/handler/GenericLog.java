package com.surcumference.fingerprint.util.log.handler;

import android.util.Log;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.inf.ILog;

public class GenericLog implements ILog {
    @Override public void v(String tag, String msg) { Log.v(tag, msg); }
    @Override public void d(String tag, String msg) { Log.d(tag, msg); }
    @Override public void i(String tag, String msg) { Log.i(tag, msg); }
    @Override public void w(String tag, String msg) { Log.w(tag, msg); }
    @Override public void e(String tag, String msg) { Log.e(tag, msg); Umeng.reportError(tag + " " + msg); }
}
