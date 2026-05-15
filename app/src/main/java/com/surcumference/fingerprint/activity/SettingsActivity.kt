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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    onBack: () -> Unit,
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
        }
    }
}