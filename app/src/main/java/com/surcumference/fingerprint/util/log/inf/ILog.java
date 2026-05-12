package com.surcumference.fingerprint.util.log.inf;

public interface ILog {
    void v(String tag, String msg);
    void d(String tag, String msg);
    void i(String tag, String msg);
    void w(String tag, String msg);
    void e(String tag, String msg);
}
