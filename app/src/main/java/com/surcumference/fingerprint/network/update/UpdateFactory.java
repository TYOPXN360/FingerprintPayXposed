package com.surcumference.fingerprint.network.update;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.bean.UpdateInfo;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.update.github.GithubUpdateChecker;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.DownloadView;
import com.surcumference.fingerprint.view.MessageView;
import com.surcumference.fingerprint.view.UpdateInfoView;


import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Jason on 2017/9/10.
 */

public class UpdateFactory {

    public static void doUpdateCheck(final Context context) {
        doUpdateCheck(context, true, false);
    }

    public static void doUpdateCheck(final Context context, final boolean quite, final boolean dontSkip) {
        if (!quite) {
            Toaster.showLong(Lang.getString(R.id.toast_checking_update));
        }
        try {
            String packageName = context.getPackageName();
            String fileName = packageName + ".apk";
            File targetFile = FileUtils.getSharableFile(context, fileName);
            FileUtils.delete(targetFile);
            new GithubUpdateChecker(BuildConfig.VERSION_NAME, Constant.UPDATE_URLS,
                    new UpdateResultListener() {
                @Override
                public void onNoUpdate() {
                    if (!quite) {
                        Toaster.showLong(Lang.getString(R.id.toast_no_update));
                    }
                }

                @Override
                public void onNetErr(Exception e) {
                    if (!quite) {
                        Toaster.showLong(Lang.getString(R.id.toast_check_update_fail_net_err));
                    }
                }

                @Override
                public void onHasUpdate(UpdateInfo updateInfo) {
                    if (!dontSkip) {
                        if (isSkipVersion(context, updateInfo.version)) {
                            L.d("已跳過版本: " + updateInfo.version);
                            return;
                        }
                    }
                    Toaster.cancel();
                    UpdateInfoView updateInfoView = new UpdateInfoView(context);
                    updateInfoView.setTitle(Lang.getString(R.id.found_new_version) + updateInfo.version);
                    updateInfoView.setContent(updateInfo.content);
                    updateInfoView.withOnNeutralButtonClickListener((dialogInterface, i) -> {
                        Config.from(context).setSkipVersion(updateInfo.version);
                        dialogInterface.dismiss();
                    });
                    updateInfoView.withOnPositiveButtonClickListener((dialogInterface, i) -> {
                        handleXposedUpdate(context, updateInfo, dialogInterface);
                    });
                    Task.onMain(200, updateInfoView::showInDialog);
                }
            }).doUpdateCheck();
        } catch (Exception | Error e) {
            //for OPPO R11 Plus 6.0 NoSuchFieldError: No instance field mResultListener
            L.e(e);
        }
    }

    private static void handleXposedUpdate(Context context, UpdateInfo updateInfo, DialogInterface updateInfoViewDialogInterface) {
        String fileName = context.getPackageName() + ".apk";
        File targetFile = FileUtils.getSharableFile(context, fileName);
        FileUtils.delete(targetFile);
        String mirrorUrl = String.format(Locale.getDefault(), Constant.UPDATE_URL_MIRROR_FILE, updateInfo.version, updateInfo.name);
        new DownloadView(context)
                .download(new String[]{mirrorUrl, updateInfo.url}, targetFile, updateInfo.size, () -> {
                    updateInfoViewDialogInterface.dismiss();
                    UpdateFactory.installApk(context, targetFile);
                    new MessageView(context).text(Lang.getString(R.id.update_success_note)).showInDialog();
                }).showInDialog();
    }

    public static void lazyUpdateWhenActivityAlive() {
        int lazyCheckTimeMsec = BuildConfig.DEBUG ? 200 : 6000;
        Task.onMain(lazyCheckTimeMsec, new Runnable() {
            @Override
            public void run() {
                Activity activity = ApplicationUtils.getCurrentActivity();
                if (activity == null
                    || activity.getClass().getName().contains("com.tencent.mm.app.WeChatSplashActivity")) {
                    Task.onMain(lazyCheckTimeMsec, this);
                    return;
                }
                UpdateFactory.doUpdateCheck(activity);
            }
        });
    }

    private static boolean isSkipVersion(Context context, String targetVersion) {
        Config config = Config.from(context);
        String skipVersion = config.getSkipVersion();
        if (TextUtils.isEmpty(skipVersion)) {
            return false;
        }
        if (String.valueOf(targetVersion).equals(skipVersion)) {
            return true;
        }
        return false;
    }

    public static void installApk(Context context, File file) {
        Uri uri = FileUtils.getUri(context, file);
        file.setReadable(true, false);
        file.getParentFile().setReadable(true, false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        Task.onMain(() -> context.startActivity(intent));
    }
}
