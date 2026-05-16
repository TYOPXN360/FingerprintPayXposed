package com.surcumference.fingerprint.util.log;

import android.util.Log;

import com.surcumference.fingerprint.xposed.XposedInit;

/**
 * Logger using ONLY LibXposed API 101 XposedInterface.log().
 * All output goes to the Xposed framework log via XposedInit.sInstance.log().
 * Falls back to android.util.Log when Xposed is not available (module app UI).
 */
public class L {

    private static final String TAG = "FingerprintPay";

    // --- Public API: mirrors the old L.java varargs patterns ---
    public static void v(Object... args) { log(Log.VERBOSE, args); }
    public static void d(Object... args) { log(Log.DEBUG, args); }
    public static void i(Object... args) { log(Log.INFO, args); }
    public static void w(Object... args) { log(Log.WARN, args); }
    public static void e(Object... args) { log(Log.ERROR, args); }

    // --- Core log method ---
    private static void log(int priority, Object... args) {
        if (args == null || args.length == 0) return;
        String msg = buildMessage(args);
        Throwable tr = extractThrowable(args);
        if (msg == null && tr == null) return;
        doLog(priority, TAG, msg, tr);
    }

    private static String buildMessage(Object... args) {
        if (args == null || args.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (Object o : args) {
            if (o instanceof Throwable) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(o);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static Throwable extractThrowable(Object... args) {
        if (args == null) return null;
        for (Object o : args) {
            if (o instanceof Throwable) return (Throwable) o;
        }
        return null;
    }

    private static void doLog(int priority, String tag, String msg, Throwable tr) {
        try {
            XposedInit instance = XposedInit.sInstance;
            if (instance != null) {
                if (msg != null) instance.log(priority, tag, msg);
                if (tr != null) {
                    String stack = Log.getStackTraceString(tr);
                    if (stack != null && !stack.isEmpty()) {
                        // Split long stack traces into multiple log calls
                        int maxLen = 4000;
                        for (int i = 0; i < stack.length(); i += maxLen) {
                            int end = Math.min(stack.length(), i + maxLen);
                            instance.log(priority, tag, stack.substring(i, end));
                        }
                    }
                }
            } else {
                // Fallback: module app process where Xposed is not hooked
                String fullMsg = msg != null ? msg : "";
                if (tr != null) {
                    fullMsg += "\n" + Log.getStackTraceString(tr);
                }
                if (priority >= Log.WARN) {
                    Log.e(tag, fullMsg);
                } else {
                    Log.d(tag, fullMsg);
                }
            }
        } catch (Throwable ignored) {
            // Fatal: can't even log, do nothing
        }
    }
}
