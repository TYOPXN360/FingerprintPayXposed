package com.surcumference.fingerprint.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.surcumference.fingerprint.Lang
import com.surcumference.fingerprint.R
import com.surcumference.fingerprint.ui.theme.FingerprintPayTheme
import com.surcumference.fingerprint.util.BiometricPromptHandler
import com.surcumference.fingerprint.util.Config
import com.surcumference.fingerprint.util.log.L

class SettingsActivity : ComponentActivity() {

    companion object {
        fun open(context: Context) {
            try {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    private var showIcon by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showIcon = Config.from(this).isShowAppIcon()
        setContent {
            FingerprintPayTheme {
                SettingsScreen(
                    showIcon = showIcon,
                    onShowIconChange = { newValue ->
                        showIcon = newValue
                        updateLauncherIcon(newValue)
                    },
                    onBack = { finish() },
                    testDecrypt = { testBiometricDecrypt() },
                    testDecryptNoKeyboard = { testBiometricDecryptNoKeyboard() },
                )
            }
        }
    }

    private fun updateLauncherIcon(show: Boolean) {
        Config.from(this).setShowAppIcon(show)
        val state = if (show) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val aliasName = ComponentName(this, "com.surcumference.fingerprint.Main")
        packageManager.setComponentEnabledSetting(aliasName, state, PackageManager.DONT_KILL_APP)
    }

    private fun testBiometricDecrypt() {
        val config = Config.from(this)
        val encrypted = config.getPasswordEncrypted()
        val iv = config.getPasswordIV()
        if (encrypted.isNullOrEmpty() || iv.isNullOrEmpty()) {
            com.hjq.toast.Toaster.show("未设置支付密码，请先在目标应用中录入密码")
            return
        }
        L.d("[测试] 开始Biometric解密密文长度=" + encrypted.length + " iv长度=" + iv.length)
        BiometricPromptHandler(this).decryptPasscode(encrypted, object : BiometricPromptHandler.IdentifyListener {
            override fun onDecryptionSuccess(handler: BiometricPromptHandler, decryptedContent: String) {
                val msg = "解密成功! 密码=" + decryptedContent
                L.d("[测试] " + msg)
                runOnUiThread {
                    com.hjq.toast.Toaster.show(msg)
                }
            }
            override fun onFailed(handler: BiometricPromptHandler, errorCode: Int, errString: String?) {
                val msg = "解密失败: code=" + errorCode + " " + (errString ?: "")
                L.e("[测试] " + msg)
                runOnUiThread {
                    com.hjq.toast.Toaster.show(msg)
                }
            }
        })
    }

    private fun testBiometricDecryptNoKeyboard() {
        val config = Config.from(this)
        val encrypted = config.getPasswordEncrypted()
        val iv = config.getPasswordIV()
        if (encrypted.isNullOrEmpty() || iv.isNullOrEmpty()) {
            com.hjq.toast.Toaster.show("未设置支付密码，请先在目标应用中录入密码")
            return
        }
        L.d("[测试/极速] 开始Biometric解密密文长度=" + encrypted.length + " iv长度=" + iv.length)
        BiometricPromptHandler(this).decryptPasscode(encrypted, object : BiometricPromptHandler.IdentifyListener {
            override fun onDecryptionSuccess(handler: BiometricPromptHandler, decryptedContent: String) {
                val msg = "极速模式解密成功! 密码=" + decryptedContent
                L.d("[测试/极速] " + msg)
                // 尝试查找当前界面的密码输入框
                val currentActivity = this@SettingsActivity
                runOnUiThread {
                    com.hjq.toast.Toaster.show(msg)
                }
            }
            override fun onFailed(handler: BiometricPromptHandler, errorCode: Int, errString: String?) {
                val msg = "极速模式解密失败: code=" + errorCode + " " + (errString ?: "")
                L.e("[测试/极速] " + msg)
                runOnUiThread {
                    com.hjq.toast.Toaster.show(msg)
                }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    testDecrypt: () -> Unit = {},
    testDecryptNoKeyboard: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocalContext.current.getString(R.string.generic_settings),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Card(
                onClick = { onShowIconChange(!showIcon) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LocalContext.current.getString(R.string.settings_title_show_icon),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Switch(
                        checked = showIcon,
                        onCheckedChange = onShowIconChange,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 测试功能卡片
            Card(
                onClick = { testDecrypt() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "测试: 指纹解密(普通模式)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "验证指纹后以Toast显示解密密码(含键盘检测)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = { testDecryptNoKeyboard() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "测试: 指纹解密(极速付款模式)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "验证指纹后以Toast显示解密密码(无键盘模拟)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = {
                    testDecrypt()
                    testDecryptNoKeyboard()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "注意：密码仅本地Toast显示，不会上传",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}