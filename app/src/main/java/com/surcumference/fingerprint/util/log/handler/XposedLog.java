package com.surcumference.fingerprint.util.log.handler;

import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.inf.ILog;

import android.util.Log;

/**
 * Created by Jason on 2017/9/10.
 */

public class XposedLog implements ILog {

    @Override
    public void debug(String tag, String msg) {
        Log.d(tag + " " + msg);
    }

    @Override
    public void error(String tag, String msg) {
        Log.d(tag + " " + msg);
        Umeng.reportError(tag + " " + msg);
    }
}
