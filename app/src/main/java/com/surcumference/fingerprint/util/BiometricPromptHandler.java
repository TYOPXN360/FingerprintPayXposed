package com.surcumference.fingerprint.util;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.surcumference.fingerprint.Config;
import com.surcumference.fingerprint.util.log.L;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * BiometricPrompt-based handler replacing XBiometricIdentify + BizBiometricIdentify.
 * Uses AndroidX BiometricPrompt with CryptoObject for secure biometric authentication.
 * Requires Android 12+ (minSdk 31).
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
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final FragmentActivity activity;
    private final Config config;
    private String cipherContent;
    private boolean isEncryptMode;
    private boolean cancelled;

    public BiometricPromptHandler(@NonNull FragmentActivity activity) {
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
    }

    private void startBiometric(@NonNull IdentifyListener listener) {
        cancelled = false;

        // Check biometric availability
        BiometricManager biometricManager = BiometricManager.from(activity);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            L.w("[Biometric] 设备不支持或未注册生物识别: canAuthenticate=" + canAuthenticate);
            listener.onFailed(this, canAuthenticate, "Biometric not available (code=" + canAuthenticate + ")");
            return;
        }

        try {
            Cipher cipher = createCipher(isEncryptMode);
            if (cipher == null) {
                listener.onFailed(this, -1, "Failed to create cipher");
                return;
            }

            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(isEncryptMode ? "设置指纹支付" : "指纹支付")
                    .setSubtitle(isEncryptMode ? "验证指纹以加密支付密码" : "验证指纹以完成支付")
                    .setNegativeButtonText("取消")
                    .setConfirmationRequired(false)
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                    ContextCompat.getMainExecutor(activity),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            if (cancelled) return;
                            L.i("[Biometric] 认证成功");
                            try {
                                Cipher authCipher = result.getCryptoObject().getCipher();
                                if (authCipher == null) {
                                    listener.onFailed(BiometricPromptHandler.this, -1, "CryptoObject cipher is null");
                                    return;
                                }
                                listener.onInited(BiometricPromptHandler.this);
                                if (isEncryptMode) {
                                    byte[] encrypted = authCipher.doFinal(cipherContent.getBytes(StandardCharsets.UTF_8));
                                    byte[] iv = authCipher.getIV();
                                    listener.onEncryptionSuccess(BiometricPromptHandler.this,
                                            AESUtils.byte2hex(encrypted), iv);
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
                    });

            listener.onInited(this);
            biometricPrompt.authenticate(promptInfo, cryptoObject);

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
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(-1) // biometric required each time
                    .build());
            keyGenerator.generateKey();
            L.d("[Biometric] 新密钥已生成");
        }

        SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);
        if (key == null) {
            L.e("[Biometric] 密钥获取失败");
            return null;
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        if (encryptMode) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            // Decryption: IV stored in config during encryption
            String ivHex = config.getPasswordIV();
            if (ivHex != null && !ivHex.isEmpty()) {
                byte[] iv = AESUtils.hex2byte(ivHex);
                if (iv.length == 12) { // GCM standard IV size
                    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
                } else {
                    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                }
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
        }
        return cipher;
    }

}
