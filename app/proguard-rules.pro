# ProGuard / R8 rules for FingerprintPayXposed

# ===== LibXposed API 101 - MUST KEEP =====
-keep class io.github.libxposed.api.** { *; }
-keep class com.surcumference.fingerprint.xposed.XposedInit { *; }

# ===== Module entry points (hooked by Xposed framework) =====
-keep class com.surcumference.fingerprint.plugin.xposed.** { *; }
-keep class com.surcumference.fingerprint.plugin.impl.** { *; }

# ===== Keep all classes used via reflection (ViewUtils.findViewByName etc.) =====
-keep class com.surcumference.fingerprint.util.** { *; }
-keep class com.surcumference.fingerprint.view.** { *; }
-keep class com.surcumference.fingerprint.activity.** { *; }

# ===== Keep Config (SharedPreferences serialization) =====
-keep class com.surcumference.fingerprint.Config { *; }
-keep class com.surcumference.fingerprint.Constant { *; }

# ===== Keep R8 from stripping custom View classes =====
-keep class com.surcumference.fingerprint.view.** { *; }

# ===== Third-party libraries =====
-keep class com.hjq.toast.** { *; }
-keep class com.wei.android.lib.fingerprintidentify.** { *; }

# ===== Keep Kotlin metadata =====
-keep class kotlin.Metadata { *; }

# ===== Keep Compose runtime classes =====
-keep class androidx.compose.** { *; }

# ===== Keep Gson serialization =====
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ===== Keep BiometricPrompt platform API =====
-keep class android.hardware.biometrics.** { *; }

# ===== General Android =====
-keepattributes Exceptions, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation class * {
    @android.webkit.JavascriptInterface <methods>;
}
