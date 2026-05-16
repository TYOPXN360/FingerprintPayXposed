package com.surcumference.fingerprint.plugin.impl.alipay;

import static com.surcumference.fingerprint.Constant.ICON_ALIPAY_SETTING_ENTRY_BASE64;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_ALIPAY;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.DigitPasswordKeyPadInfo;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.plugin.inf.OnFingerprintVerificationOKListener;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ActivityViewObserverHolder;
import com.surcumference.fingerprint.util.AlipayVersionControl;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BiometricPromptHandler;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.DialogUtils;
import com.surcumference.fingerprint.view.SettingsView;
import java.util.ArrayList;
import java.util.List;

public class AlipayBasePlugin implements IAppPlugin {


    private AlertDialog mFingerPrintAlertDialog;
    private int mPwdActivityReShowDelayTimeMsec;

    private BiometricPromptHandler mFingerprintIdentify;
    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mAlipayVersionCode;
    private boolean mFingerprintIdentifyTemporaryBlocking = false;

    @Override
    public int getVersionCode(Context context) {
        if (mAlipayVersionCode != 0) {
            return mAlipayVersionCode;
        }
        mAlipayVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_ALIPAY);
        return mAlipayVersionCode;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        L.d("activity", activity);
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            int alipayVersionCode = getVersionCode(activity);
            if (alipayVersionCode >= 773 /** 10.3.80.9100 */ && activityClzName.contains(".FBAppWindowActivity")) {
                ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
                activityViewObserver.setViewIdentifyText("支付密码", "支付密碼", "Payment Password");
                ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.AlipaySettingPageEntered,
                        activityViewObserver, 100, (observer, view) -> doSettingsMenuInject_10_1_38(activity),
                        30000);
            } else if (activityClzName.contains(".MySettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject_10_1_38(activity));
            } else if (activityClzName.contains(".UserSettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject(activity));
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        //Xposed not hooked yet!
        L.d("onActivityPaused", activity);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public boolean getMockCurrentUser() {
        return false;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    public void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            L.d("[支付宝] onActivityResumed: " + activityClzName
                + " hashCode=" + System.identityHashCode(activity)
                + " isFinishing=" + activity.isFinishing());
            mCurrentActivity = activity;
            if (activityClzName.contains(".PayPwdDialogActivity")
                    || activityClzName.contains(".PayPwdFullActivity")
                    || activityClzName.contains(".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.i("[支付宝] *** 检测到密码弹窗 Activity: " + activityClzName + " ***");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                if (mFingerprintIdentifyTemporaryBlocking) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
                int versionCode = getVersionCode(activity);
                View rootView = activity.getWindow().getDecorView();
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (mCurrentActivity == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) || mCurrentActivity != activity) {
                        return;
                    }
                    if (versionCode >= 661 /** 10.3.10.8310 */) {
                        View foundInputEt = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password");
                        View foundKeyboard = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "keyboard_container");
                        boolean isRechargePay = ViewUtils.isShown(foundInputEt) && ViewUtils.isShown(foundKeyboard);
                        L.v("[支付宝] View检测: input_et_password=" + foundInputEt + " shown=" + ViewUtils.isShown(foundInputEt)
                            + " keyboard_container=" + foundKeyboard + " shown=" + ViewUtils.isShown(foundKeyboard));

                        View foundLongPwd = ViewUtils.findViewByText(rootView, "请输入长密码", "請輸入長密碼", "Payment Password");
                        View found6digit = ViewUtils.findViewByText(rootView, "密码共6位，已输入0位");
                        boolean isNormalPay = ViewUtils.isShown(foundLongPwd) || ViewUtils.isShown(found6digit);
                        L.v("[支付宝] View检测: 长密码=" + foundLongPwd + " 6位=" + found6digit);

                        View foundKeyArea = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "ll_key_area");
                        boolean isKeyAreaPay = ViewUtils.isShown(foundKeyArea);
                        L.v("[支付宝] View检测: ll_key_area=" + foundKeyArea + " shown=" + ViewUtils.isShown(foundKeyArea));

                        // 极速付款模式: 尝试检测"请输入支付密码"文本
                        View foundPayPwdText = ViewUtils.findViewByText(rootView, "请输入支付密码", "請輸入支付密碼", "Payment password");
                        boolean hasPayPwdText = ViewUtils.isShown(foundPayPwdText);
                        L.d("[支付宝] 极速模式检测: 请输入支付密码文本=" + foundPayPwdText + " shown=" + hasPayPwdText);

                        boolean anyCondition = isRechargePay || isNormalPay || isKeyAreaPay || hasPayPwdText;
                        L.d("[支付宝] View检测结果汇总: isRechargePay=" + isRechargePay
                            + " isNormalPay=" + isNormalPay
                            + " isKeyAreaPay=" + isKeyAreaPay
                            + " hasPayPwdText=" + hasPayPwdText
                            + " -> " + (anyCondition ? "满足条件，调用showFingerPrintDialog" : "不满足，跳过"));

                        if (anyCondition) {
                            if (mIsViewTreeObserverFirst) {
                                if (showFingerPrintDialog(activity)) {
                                    mIsViewTreeObserverFirst = false;
                                }
                            }
                            return;
                        }
                        return;
                    }
                    if (ViewUtils.findViewByName(activity, (versionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.safepaybase", "mini_linSimplePwdComponent") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.safepaysdk", "mini_linSimplePwdComponent") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password") == null ) {
                        return;
                    }
                    if (mIsViewTreeObserverFirst) {
                        mIsViewTreeObserverFirst = false;
                        showFingerPrintDialog(activity);
                    }
                });

            } else if (activityClzName.contains("PayPwdHalfActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                if (mFingerprintIdentifyTemporaryBlocking) {
                    return;
                }
                Task.onMain(1500, () -> {
                    int versionCode = getVersionCode(activity);
                    DigitPasswordKeyPadInfo digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
                    View key1View = ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1);
                    if (key1View != null) {
                        showFingerPrintDialog(activity);
                        return;
                    }

                    //try again
                    Task.onMain(2000, () -> showFingerPrintDialog(activity));
                });
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    public void initFingerPrintLock(final Context context ,AlertDialog dialog, String passwordEncrypted,
                                    OnFingerprintVerificationOKListener onSuccessUnlockCallback) {
        if (!(context instanceof Activity)) {
            L.e("[支付宝] initFingerPrintLock: Context不是Activity");
            return;
        }
        // Hide the custom dialog since BiometricPrompt shows system UI
        ViewUtils.setAlpha(dialog, 0);
        ViewUtils.setDimAmount(dialog, 0);
        mFingerprintIdentify = new BiometricPromptHandler((Activity) context);
        mFingerprintIdentify.decryptPasscode(passwordEncrypted, new BiometricPromptHandler.IdentifyListener() {

                    @Override
                    public void onDecryptionSuccess(BiometricPromptHandler h, @NonNull String decryptedContent) {
                        onSuccessUnlockCallback.onFingerprintVerificationOK(decryptedContent);
                    }

                    @Override
                    public void onFailed(BiometricPromptHandler h, int errorCode, @Nullable String errString) {
                        if (dialog != null) {
                            ViewUtils.setAlpha(dialog, 1);
                            ViewUtils.setDimAmount(dialog, 0.6f);
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    }
                });
    }

    public boolean showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        final Config config = Config.from(context);
        L.i("[支付宝] showFingerPrintDialog 被调用, Activity=" + activity.getClass().getName()
            + " 配置已开启=" + config.isOn()
            + " 临时屏蔽=" + mFingerprintIdentifyTemporaryBlocking);
        try {
            int versionCode = getVersionCode(activity);
            L.d("[支付宝] 版本号=" + versionCode);
            if (versionCode >= 224) {
                if (activity.getClass().getName().contains(".MspContainerActivity")
                        || activity.getClass().getName().contains(".PayPwdFullActivity")) {
                    View payTextView = ViewUtils.findViewByText(activity.getWindow().getDecorView(),
                            "支付宝支付密码", "支付寶支付密碼", "Alipay Payment Password",
                            "请输入支付密码", "請輸入支付密碼", "Payment password",
                            "请输入长密码", "請輸入長密碼", "密码共6位，已输入0位");
                    L.d("[支付宝] payTextView 查找结果=" + payTextView);
                    if (payTextView == null) {
                        L.d("[支付宝] payTextView 为空, 可能不是MspContainerActivity, 继续");
                    }
                } else {
                    L.d("[支付宝] 不是MspContainerActivity/PayPwdFullActivity, 跳过文本检测");
                }
            }

            hidePreviousPayDialog();
            String passwordEncrypted = config.getPasswordEncrypted();
            if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
                L.w("[支付宝] 密码未设置, 弹出提示");
                Toaster.showLong(Lang.getString(R.id.toast_password_not_set_alipay));
                return true;
            }
            L.d("[支付宝] 密码已设置, 密码长度=" + passwordEncrypted.length());

            mPwdActivityReShowDelayTimeMsec = 0;
            clickDigitPasswordWidget(activity);
            reEnteredPayDialogSolution(activity);
            AlipayPayView alipayPayView = new AlipayPayView(context)
                .withOnShowListener((target) -> {
                    AlertDialog dialog = target.getDialog();
                    initFingerPrintLock(context, dialog, passwordEncrypted, (password) -> {
                        L.d("[支付宝] 认证成功回调, 密码长度=" + password.length());
                        BlackListUtils.applyIfNeeded(context);
                        Runnable onCompleteRunnable = () -> {
                            mPwdActivityReShowDelayTimeMsec = 1000;
                            DialogUtils.dismiss(mFingerPrintAlertDialog);
                        };

                        // 优先使用inputDigitPassword点击数字键盘
                        L.d("[支付宝] 尝试inputDigitPassword...");
                        boolean digitOk = false;
                        try {
                            inputDigitPassword(activity, password);
                            digitOk = true;
                            L.d("[支付宝] inputDigitPassword完成");
                        } catch (NullPointerException e) {
                            L.d("[支付宝] inputDigitPassword NPE: " + e.getMessage());
                        } catch (Exception e) {
                            L.e(e, "[支付宝] inputDigitPassword异常");
                        }
                        if (!digitOk) {
                            L.d("[支付宝] inputDigitPassword失败, 尝试tryInputGenericPassword兜底...");
                            boolean tryInputOk = tryInputGenericPassword(activity, password);
                            L.d("[支付宝] tryInputGenericPassword结果=" + tryInputOk);
                        }
                        onCompleteRunnable.run();
                    });
                    // 无需反注册， 作用域仅限于Dialog Window
                    if (config.isVolumeDownMonitorEnabled()) {
                        ViewUtils.registerVolumeKeyDownEventListener(dialog.getWindow(), event -> {
                            if (mFingerprintIdentifyTemporaryBlocking) {
                                return false;
                            }
                            DialogUtils.dismiss(dialog);
                            Toaster.showLong(Lang.getString(R.id.toast_fingerprint_temporary_disabled));
                            mFingerprintIdentifyTemporaryBlocking = true;
                            Task.onBackground(60000, () -> mFingerprintIdentifyTemporaryBlocking = false);
                            return false;
                        });
                    }
            }).withOnCancelButtonClickListener(target -> {
                DialogUtils.dismiss(target.getDialog());
            }).withOnDismissListener(v -> {
                BiometricPromptHandler fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancel();
                }
            });
            AlertDialog fingerPrintAlertDialog = alipayPayView.showInDialog();
            ViewUtils.setAlpha(fingerPrintAlertDialog, 0);
            ViewUtils.setDimAmount(fingerPrintAlertDialog, 0);
            mFingerPrintAlertDialog = fingerPrintAlertDialog;
        } catch (OutOfMemoryError e) {
        }
        return true;
    }

    private void setupPaymentItemOnClickListener(ViewGroup rootView) {
        List<View> paymentMethodsViewList = new ArrayList<>();
        ViewUtils.getChildViewsByRegex(rootView, ".*选中,.+", paymentMethodsViewList);
        long paymentMethodsViewListSize = paymentMethodsViewList.size();
        for (int i = 0; i < paymentMethodsViewListSize; i++) {
            View paymentMethodView = paymentMethodsViewList.get(i);
            L.d("paymentMethodView", ViewUtils.getViewInfo(paymentMethodView));
            // 只取第一次, 防止多次调用造成出错
            View.OnClickListener originPaymentMethodListener = (View.OnClickListener) paymentMethodView.getTag(R.id.alipay_payment_method_item_click_listener);
            if (originPaymentMethodListener == null) {
                paymentMethodView.setTag(R.id.alipay_payment_method_item_click_listener, ViewUtils.getOnClickListener(paymentMethodView));
            }
            paymentMethodView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (mFingerprintIdentifyTemporaryBlocking) {
                            return;
                        }
                        AlertDialog dialog = mFingerPrintAlertDialog;
                        if (dialog == null) {
                            return;
                        }
                        if (!dialog.isShowing()) {
                            dialog.show();
                        }
                    } finally {
                        View.OnClickListener originPaymentMethodListener = (View.OnClickListener) v.getTag(R.id.alipay_payment_method_item_click_listener);
                        if (originPaymentMethodListener != null && originPaymentMethodListener != this) {
                            originPaymentMethodListener.onClick(v);
                        }
                    }
                }
            });
        }
    }

    private void reEnteredPayDialogSolution(Activity activity) {
        int versionCode = getVersionCode(activity);
        L.d("[支付宝] reEnteredPayDialogSolution: versionCode=" + versionCode);
        if (versionCode < 1261 /** 10.5.96.8000 */) {
            L.d("[支付宝] 版本低于1261, 跳过 reEnteredPayDialogSolution");
            return;
        }
        ViewGroup rootView = (ViewGroup)activity.getWindow().getDecorView();
        setupPaymentItemOnClickListener(rootView);
        // 在10s内寻找密码框
        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
        activityViewObserver.setActivityViewFinder(outViewList -> {
            EditText view = findPasswordEditText(activity);
            if (view != null) {
                outViewList.add(view);
            }
            View shortPwdView = ViewUtils.findViewByText(rootView, "密码共6位，已输入0位");
            if (ViewUtils.isShown(shortPwdView)) {
                outViewList.add(shortPwdView);
            }
        });
        ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.AlipayPasswordView,
                activityViewObserver, 300, (observer, view) -> {
                    ActivityViewObserverHolder.stop(observer);
                    L.d("找到密码框", view);
                    Object debounceToken = new Object();
                    Handler debounceHandler = view.getHandler();
                    view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                        debounceHandler.removeCallbacksAndMessages(debounceToken);
                        debounceHandler.postAtTime(() -> {
                            setupPaymentItemOnClickListener(rootView);
                        }, debounceToken, SystemClock.uptimeMillis() + 666);
                    });
                },
                10000);
    }

    /**
     * 修复某个设备不自动弹出键盘
     * @param activity
     */
    private void clickDigitPasswordWidget(Activity activity) {
        int versionCode = getVersionCode(activity);
        View view = ViewUtils.findViewByName(activity, (versionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout");
        L.d("digit password widget", view);
        if (view == null) {
            return;
        }
        ViewUtils.performActionClick(view);
    }

    private void doSettingsMenuInject_10_1_38(final Activity activity) {

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).pressedColor(0xFFD9D9D9).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtils.dip2px(activity, 12), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, DpUtils.dip2px(activity, 18), 0);
        itemSummerText.setTextColor(0xFF999999);
        int versionCode = getVersionCode(activity);

        //try use Alipay style
        try {
            View settingsView = versionCode >= 773 /** 10.3.80.9100 */
                    ? ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付密码", "支付密碼", "Payment Password")
                    : ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "item_left_text");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        if (versionCode >= 773 /** 10.3.80.9100 */) {
        } else if (versionCode >= 661 /** 10.3.10.8310 */) {
            ImageView itemIconImageView = new ImageView(activity);
            itemIconImageView.setImageBitmap(ImageUtils.base64ToBitmap(ICON_ALIPAY_SETTING_ENTRY_BASE64));
            LinearLayout.LayoutParams itemIconImageViewLayoutParams = new LinearLayout.LayoutParams(DpUtils.dip2px(activity, 24), DpUtils.dip2px(activity, 24));
            itemIconImageViewLayoutParams.leftMargin = DpUtils.dip2px(activity, 12);
            itemHlinearLayout.addView(itemIconImageView, itemIconImageViewLayoutParams);
        }
        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout rootLinearLayout = new LinearLayout(activity);
        rootLinearLayout.setOrientation(LinearLayout.VERTICAL);
        rootLinearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);

        if (versionCode >= 661 /** 10.3.10.8310 */) {
            lineTopView.setVisibility(View.INVISIBLE);
            itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE)
                    .pressedColor(0xFFEBEBEB).round(32).create());
            lineBottomView.setVisibility(View.INVISIBLE);
            lineParams.bottomMargin = DpUtils.dip2px(activity, 8);
            rootLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        } else {
            rootLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 45)));
            lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        }

        rootLinearLayout.addView(lineBottomView, lineParams);

        if (versionCode >= 773 /** 10.3.80.9100 */) {
            View itemView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付密码", "支付密碼", "Payment Password");
            if (itemView != null) {
                ViewGroup itemViewGroup = (ViewGroup) itemView.getParent().getParent().getParent().getParent();
                L.d("生物支付item: " + ViewUtils.getViewInfo(itemViewGroup));
                itemViewGroup.setPadding(0, DpUtils.dip2px(activity, 62), 0, 0);
                itemViewGroup.setClipToPadding(false);
                FrameLayout.LayoutParams rootLinearLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rootLinearLayoutParams.topMargin = -DpUtils.dip2px(activity, 50);
                rootLinearLayoutParams.leftMargin = DpUtils.dip2px(activity, 12);
                rootLinearLayoutParams.rightMargin = DpUtils.dip2px(activity, 12);
                itemViewGroup.addView(rootLinearLayout, rootLinearLayoutParams);
            }
        } else {
            int listViewId = activity.getResources().getIdentifier("setting_list", "id", "com.alipay.android.phone.openplatform");
            ListView listView = activity.findViewById(listViewId);
            listView.addHeaderView(rootLinearLayout);
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        int logout_id = activity.getResources().getIdentifier("logout", "id", "com.alipay.android.phone.openplatform");

        View logoutView = activity.findViewById(logout_id);
        LinearLayout linearLayout = (LinearLayout) logoutView.getParent();
        linearLayout.setPadding(0, 0, 0, 0);
        List<ViewGroup.LayoutParams> childViewParamsList = new ArrayList<>();
        List<View> childViewList = new ArrayList<>();
        int childViewCount = linearLayout.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View view = linearLayout.getChildAt(i);
            childViewList.add(view);
            childViewParamsList.add(view.getLayoutParams());
        }

        linearLayout.removeAllViews();

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF888888);

        //try use Alipay style
        try {
            View settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.ui", "title_bar_title");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        linearLayout.addView(lineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputDigitPassword(Activity activity, String password) {
        inputDigitPassword(activity, password, 0);
    }

    private void inputDigitPassword(Activity activity, String password, int retryCount) {
        if (retryCount > 20) {
            L.d("[支付宝] inputDigitPassword 等待数字键盘超时(6s), 放弃");
            return;
        }
        int versionCode = getVersionCode(activity);
        DigitPasswordKeyPadInfo digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
        View ks[] = new View[] {
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("1"), "1"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("2"), "2"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("3"), "3"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("4"), "4"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("5"), "5"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("6"), "6"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("7"), "7"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("8"), "8"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("9"), "9"),
                findDigitKeyView(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.keys.get("0"), "0"),
        };
        // 检查是否有至少一个数字键找到了
        boolean anyKeyFound = false;
        for (View k : ks) {
            if (k != null) { anyKeyFound = true; break; }
        }
        if (!anyKeyFound) {
            L.d("[支付宝] inputDigitPassword 键盘未就绪(retry=" + retryCount + "), 300ms后重试...");
            int finalRetry = retryCount + 1;
            activity.getWindow().getDecorView().postDelayed(() -> {
                inputDigitPassword(activity, password, finalRetry);
            }, 300);
            return;
        }

        char[] chars = password.toCharArray();
        for (int idx = 0; idx < chars.length; idx++) {
            char c = chars[idx];
            View v = null;
            int digit = c - '0';
            if (digit >= 1 && digit <= 9) {
                v = ks[digit - 1]; // '1'→ks[0], '2'→ks[1], ..., '9'→ks[8]
            } else if (digit == 0) {
                v = ks[9]; // '0'→ks[9]
            }
            if (v == null) {
                L.d("[支付宝] inputDigit按键" + c + "未找到View");
                continue;
            }
            L.d("[支付宝] inputDigit点击按键" + c + " view=" + v.getClass().getName());
            // 记录按键信息到日志
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            ViewParent parent = v.getParent();
            String parentInfo = parent != null ? parent.getClass().getName() : "null";
            L.d("[支付宝] 按键" + c + " 屏幕坐标=(" + pos[0] + "," + pos[1] + ") 大小=" + v.getWidth() + "x" + v.getHeight()
                + " 父容器=" + parentInfo + " onClickListener=" + (ViewUtils.getOnClickListener(v) != null ? "Y" : "N"));
            // 直接分发真实坐标触摸事件
            float touchX = pos[0] + v.getWidth() / 2f;
            float touchY = pos[1] + v.getHeight() / 2f;
            long downTime = SystemClock.uptimeMillis();
            MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, touchX, touchY, 0);
            v.dispatchTouchEvent(downEvent);
            downEvent.recycle();
            MotionEvent upEvent = MotionEvent.obtain(downTime + 30, downTime + 30, MotionEvent.ACTION_UP, touchX, touchY, 0);
            v.dispatchTouchEvent(upEvent);
            upEvent.recycle();

        }
    }

    private View findDigitKeyView(Activity activity, String pkg, String[] keyIds, String digitText) {
        // 先按ID查找（尝试每个可能的ID）
        View v = null;
        if (keyIds != null) {
            for (String keyId : keyIds) {
                v = ViewUtils.findViewByName(activity, pkg, keyId);
                if (v != null) break;
            }
        }
        if (v != null) {
            // 读取按钮实际显示的文本（AUSecureTextView可能被随机打乱）
            String actualText = null;
            try {
                if (v instanceof android.widget.TextView) {
                    actualText = ((android.widget.TextView) v).getText().toString().trim();
                }
            } catch (Exception ignored) {}
            if (actualText == null || actualText.isEmpty()) {
                try {
                    java.lang.reflect.Field f = v.getClass().getDeclaredField("mText");
                    f.setAccessible(true);
                    Object t = f.get(v);
                    if (t != null) actualText = t.toString().trim();
                } catch (Exception ignored) {}
            }
            L.d("[支付宝] 数字键[" + digitText + "]通过ID找到: " + v.getClass().getName()
                + " 实际文本=[" + (actualText != null ? actualText : "null") + "] shown=" + v.isShown());
            return v;
        }
        // ID查找失败，按文本查找
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "", outList);
        for (View view : outList) {
            if (view instanceof android.widget.Button || view instanceof android.widget.TextView) {
                CharSequence text = null;
                if (view instanceof android.widget.TextView) {
                    text = ((android.widget.TextView) view).getText();
                }
                if (text != null && digitText.equals(text.toString().trim())) {
                    L.d("[支付宝] 数字键[" + digitText + "]通过文本找到: " + view.getClass().getName() + " shown=" + view.isShown());
                    return view;
                }
            }
        }
        L.d("[支付宝] 数字键[" + digitText + "]未找到");
        return null;
    }

    private boolean tryInputGenericPassword(Activity activity, String password) {

        EditText pwdEditText = findPasswordEditText(activity);
        L.d("[支付宝] tryInput findPwdEditText=" + pwdEditText + " bounds=" + (pwdEditText != null ? pwdEditText.getWidth() + "x" + pwdEditText.getHeight() : "null"));
        if (pwdEditText == null) {
            L.d("[支付宝] tryInput fail: pwdEditText is null");
            return false;
        }
        View confirmPwdBtn = findConfirmPasswordBtn(activity);
        L.d("[支付宝] tryInput findConfirmBtn=" + confirmPwdBtn);
        if (confirmPwdBtn == null) {
            L.d("[支付宝] tryInput fail: confirmBtn is null");
            return false;
        }
        L.d("[支付宝] tryInput 密码长度=" + password.length());

        // 直接setText+click（不再等待渲染，inputDigitPassword才是主要输入方式）
        pwdEditText.setFocusable(true);
        pwdEditText.setFocusableInTouchMode(true);
        pwdEditText.requestFocus();
        pwdEditText.performClick();
        pwdEditText.setText(password);
        pwdEditText.postDelayed(() -> {
            confirmPwdBtn.performClick();
        }, 100);
        return true;
    }

    private EditText findPasswordEditText(Activity activity) {
        // 尝试标准密码输入框（允许不可见，后续再验证）
        View pwdEditText = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password");
        L.v("[支付宝] findPasswordEditText: input_et_password -> " + pwdEditText + " shown=" + (pwdEditText != null && pwdEditText.isShown()));
        if (pwdEditText instanceof EditText) {
            if (pwdEditText.isShown()) {
                L.d("[支付宝] 找到可见密码输入框: input_et_password");
                return (EditText) pwdEditText;
            }
            L.d("[支付宝] input_et_password 存在但不可见, 等待布局完成, bounds=" + pwdEditText.getWidth() + "x" + pwdEditText.getHeight());
            // 即使不可见也返回, 给上层一次机会
            return (EditText) pwdEditText;
        }
        // 极速付款模式: 尝试新的6位密码输入视图ID
        pwdEditText = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "ap_six_number_pwd_input");
        L.v("[支付宝] findPasswordEditText: ap_six_number_pwd_input(verifyidentity) -> " + pwdEditText);
        if (pwdEditText instanceof EditText && pwdEditText.isShown()) {
            L.d("[支付宝] 找到密码输入框: ap_six_number_pwd_input(verifyidentity)");
            return (EditText) pwdEditText;
        }
        // 尝试antui包名下的极速付款模式密码输入框
        pwdEditText = ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "ap_six_number_pwd_input");
        L.v("[支付宝] findPasswordEditText: ap_six_number_pwd_input(antui) -> " + pwdEditText);
        if (pwdEditText instanceof EditText && pwdEditText.isShown()) {
            L.d("[支付宝] 找到密码输入框: ap_six_number_pwd_input(antui)");
            return (EditText) pwdEditText;
        }
        L.d("[支付宝] 未找到输入框(input_et_password存在但不可见), 回退到遍历搜索");
        // long password
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "", outList);
        for (View view : outList) {
            if (view instanceof EditText) {
                if (view.getId() != -1) {
                    continue;
                }
                if (!view.isShown()) {
                    continue;
                }
                return (EditText) view;
            }
        }
        return null;
    }

    private View findConfirmPasswordBtn(Activity activity) {
        View okView =  ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "button_ok");
        L.d("okView", okView);
        if (okView != null) {
            // 即使不可见也返回 - 新版支付宝密码框按钮可能还未布局完成
            return okView;
        }
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViewsByRegex(rootView, "确定|確定|OK|确认|付款|確認|Pay", outList);
        int versionCode = getVersionCode(activity);
        for (View view : outList) {
            if (view.getId() != -1) {
                continue;
            }
            if (!view.isShown()) {
                continue;
            }
            // 跳过键盘上的OK
            if (view.getParent().toString().contains(":id/key_enter")) {
                continue;
            }
            if (versionCode < 1261 /** 10.5.96.8000 */) {
                return (View) view.getParent();
            }
            return view;
        }
        return null;
    }

    private void hidePreviousPayDialog() {
        L.d("hidePreviousPayDialog", mFingerPrintAlertDialog);
        DialogUtils.dismiss(mFingerPrintAlertDialog);
        mFingerPrintAlertDialog = null;
    }
}
