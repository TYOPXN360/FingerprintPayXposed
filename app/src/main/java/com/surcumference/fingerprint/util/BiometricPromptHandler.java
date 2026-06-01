package com.surcumference.fingerprint.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.log.L;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Platform BiometricPrompt handler (Android 12+).
 * Uses android.hardware.biometrics.BiometricPrompt directly (no AndroidX dependency).
 */
public class BiometricPromptHandler {

    public interface IdentifyListener {
        default void onInited(BiometricPromptHandler handler) {}
        default void onEncryptionSuccess(BiometricPromptHandler handler, @NonNull String encryptedContent, @Nullable byte[] encryptedIV) {}
        default void onDecryptionSuccess(BiometricPromptHandler handler, @NonNull String decryptedContent) {}
        default void onFailed(BiometricPromptHandler handler, int errorCode, @Nullable String errString) {}
        default void onSuccess() {}
    }

    private static final String KEY_NAME = "fingerprintpay_biometric_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private final Activity activity;
    private final Config config;
    private String cipherContent;
    private boolean isEncryptMode;
    private boolean cancelled;
    private CancellationSignal cancellationSignal;

    public BiometricPromptHandler(@NonNull Activity activity) {
        this.activity = activity;
        this.config = Config.from(activity);
    }

    public BiometricPromptHandler encryptPasscode(@NonNull String plainPassword, @NonNull IdentifyListener listener) {
        this.cipherContent = plainPassword;
        this.isEncryptMode = true;
        startBiometric(listener);
        return this;
    }

    public BiometricPromptHandler decryptPasscode(@NonNull String encryptedPassword, @NonNull IdentifyListener listener) {
        this.cipherContent = encryptedPassword;
        this.isEncryptMode = false;
        startBiometric(listener);
        return this;
    }

    public void cancel() {
        this.cancelled = true;
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    private void startBiometric(@NonNull IdentifyListener listener) {
        cancelled = false;

        BiometricManager biometricManager = activity.getSystemService(BiometricManager.class);
        if (biometricManager == null) {
            listener.onFailed(this, -1, "BiometricManager not available");
            return;
        }

        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            L.w("[Biometric] 设备不支持或未注册生物识别: code=" + canAuthenticate);
            listener.onFailed(this, canAuthenticate, "Biometric not available (code=" + canAuthenticate + ")");
            return;
        }

        try {
            Cipher cipher = createCipher(isEncryptMode);
            if (cipher == null) {
                if (!isEncryptMode) {
                    L.w("[Biometric] 解密模式下cipher创建失败, 清除已失效的密码");
                    config.setPasswordEncrypted("");
                    config.setPasswordIV("");
                    config.commit();
                    listener.onFailed(this, -1, "KEY_INVALIDATED");
                } else {
                    listener.onFailed(this, -1, "Failed to create cipher");
                }
                return;
            }

            Executor executor = ContextCompat.getMainExecutor(activity);

            BiometricPrompt.AuthenticationCallback authCallback = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    if (cancelled) return;
                    L.i("[Biometric] 认证成功");
                    try {
                        listener.onInited(BiometricPromptHandler.this);
                        BiometricPrompt.CryptoObject cryptoObject = result.getCryptoObject();
                        Cipher authCipher = cryptoObject != null ? cryptoObject.getCipher() : null;
                        if (authCipher == null) {
                            authCipher = cipher;
                        }
                        if (isEncryptMode) {
                            byte[] encrypted = authCipher.doFinal(cipherContent.getBytes(StandardCharsets.UTF_8));
                            byte[] iv = authCipher.getIV();
                            listener.onEncryptionSuccess(BiometricPromptHandler.this, AESUtils.byte2hex(encrypted), iv);
                        } else {
                            byte[] encryptedBytes = AESUtils.hex2byte(cipherContent);
                            byte[] decrypted = authCipher.doFinal(encryptedBytes);
                            String decryptedStr = new String(decrypted, StandardCharsets.UTF_8);
                            listener.onDecryptionSuccess(BiometricPromptHandler.this, decryptedStr);
                        }
                        listener.onSuccess();
                    } catch (Exception e) {
                        L.e(e, "[Biometric] 加解密失败");
                        listener.onFailed(BiometricPromptHandler.this, -1, "Crypto error: " + e.getMessage());
                    }
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    if (cancelled) return;
                    L.w("[Biometric] 认证错误: code=" + errorCode + " msg=" + errString);
                    listener.onFailed(BiometricPromptHandler.this, errorCode, errString.toString());
                }

                @Override
                public void onAuthenticationFailed() {
                    if (cancelled) return;
                    L.d("[Biometric] 认证失败（指纹不匹配）");
                }
            };

            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(activity)
                    .setTitle(isEncryptMode ? "设置指纹支付" : "指纹支付")
                    .setSubtitle(isEncryptMode ? "验证指纹以加密支付密码" : "验证指纹以完成支付")
                    .setNegativeButton("取消", executor, (dialog, which) -> {
                        L.d("[Biometric] 用户取消");
                        listener.onFailed(BiometricPromptHandler.this, -1, "User cancelled");
                    })
                    .build();

            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            cancellationSignal = new CancellationSignal();
            listener.onInited(this);
            biometricPrompt.authenticate(cryptoObject, cancellationSignal, executor, authCallback);

        } catch (Exception e) {
            L.e(e, "[Biometric] startBiometric 异常");
            listener.onFailed(this, -1, "Exception: " + e.getMessage());
        }
    }

    @Nullable
    private Cipher createCipher(boolean encryptMode) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_NAME)) {
            generateNewKey(keyStore);
        }

        SecretKey key;
        try {
            key = (SecretKey) keyStore.getKey(KEY_NAME, null);
        } catch (UnrecoverableKeyException e) {
            L.w("[Biometric] 密钥不可恢复, 删除并重新生成");
            keyStore.deleteEntry(KEY_NAME);
            generateNewKey(keyStore);
            key = (SecretKey) keyStore.getKey(KEY_NAME, null);
        }
        if (key == null) {
            L.e("[Biometric] 密钥获取失败");
            return null;
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        try {
            if (encryptMode) {
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } else {
                String ivHex = config.getPasswordIV();
                if (ivHex != null && !ivHex.isEmpty()) {
                    byte[] iv = AESUtils.hex2byte(ivHex);
                    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
                } else {
                    cipher.init(Cipher.DECRYPT_MODE, key);
                }
            }
        } catch (android.security.keystore.KeyPermanentlyInvalidatedException e) {
            L.w("[Biometric] 密钥已永久失效(指纹注册信息变更), 删除并重新生成");
            keyStore.deleteEntry(KEY_NAME);
            if (!encryptMode) {
                L.w("[Biometric] 解密模式下密钥失效, 旧密码无法解密");
                return null;
            }
            generateNewKey(keyStore);
            key = (SecretKey) keyStore.getKey(KEY_NAME, null);
            if (key == null) {
                L.e("[Biometric] 重新生成密钥后仍获取失败");
                return null;
            }
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }
        return cipher;
    }

    private void generateNewKey(KeyStore keyStore) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(-1)
                .build());
        keyGenerator.generateKey();
        L.d("[Biometric] 新密钥已生成");
    }
}
