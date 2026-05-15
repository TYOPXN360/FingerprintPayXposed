package com.surcumference.fingerprint.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.surcumference.fingerprint.BuildConfig
import com.surcumference.fingerprint.Lang
import com.surcumference.fingerprint.R
import com.surcumference.fingerprint.network.update.UpdateFactory
import com.surcumference.fingerprint.ui.theme.FingerprintPayTheme
import com.surcumference.fingerprint.util.Task
import com.surcumference.fingerprint.util.Umeng
import com.surcumference.fingerprint.util.bugfixer.TagManagerBugFixer
import com.surcumference.fingerprint.util.log.L
import com.surcumference.fingerprint.view.DonateView

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Umeng.init(this)
        setContent {
            FingerprintPayTheme {
                HomeScreen(
                    onNavigateToSettings = { SettingsActivity.open(this@HomeActivity) },
                    onItemClick = { item -> handleItemClick(item) },
                )
            }
        }
        Task.onMain(1000L) { UpdateFactory.doUpdateCheck(this@HomeActivity) }
        TagManagerBugFixer.fix()
    }

    private fun handleItemClick(item: HomeItem) {
        when (item.id) {
            "wechat" -> WebActivity.openUrl(this, com.surcumference.fingerprint.Constant.HELP_URL_WECHAT)
            "alipay" -> WebActivity.openUrl(this, com.surcumference.fingerprint.Constant.HELP_URL_ALIPAY)
            "faq" -> WebActivity.openUrl(this, com.surcumference.fingerprint.Constant.HELP_URL_FAQ)
            "qq_group" -> joinQQGroup()
            "donate" -> DonateView(this).showInDialog()
            "check_update" -> UpdateFactory.doUpdateCheck(this, false, true)
            "license" -> WebActivity.openUrl(this, com.surcumference.fingerprint.Constant.HELP_URL_LICENSE)
            "website" -> {
                com.surcumference.fingerprint.util.UrlUtils.openUrl(this, com.surcumference.fingerprint.Constant.PROJECT_URL)
                com.hjq.toast.Toaster.showLong(Lang.getString(R.id.toast_give_me_star))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Umeng.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        Umeng.onPause(this)
    }

    private fun joinQQGroup() {
        val intent = Intent().apply {
            data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + "A2WjHt6jDpAraj7z4LfTsSbS9SkZVEXi")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            L.e(e)
        }
    }
}

data class HomeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
)

private val homeItems = listOf(
    HomeItem("wechat",
        Lang.getString(R.id.settings_title_help_wechat),
        Lang.getString(R.id.settings_sub_title_help_wechat),
        Icons.Filled.School, Icons.Outlined.School),
    HomeItem("alipay",
        Lang.getString(R.id.settings_title_help_alipay),
        Lang.getString(R.id.settings_sub_title_help_alipay),
        Icons.Filled.Info, Icons.Outlined.Info),
    HomeItem("faq",
        Lang.getString(R.id.settings_title_help_faq),
        Lang.getString(R.id.settings_sub_title_help_faq),
        Icons.Filled.BugReport, Icons.Outlined.BugReport),
    HomeItem("qq_group",
        Lang.getString(R.id.settings_title_qq_group),
        Lang.getString(R.id.settings_sub_title_qq_group),
        Icons.Filled.Forum, Icons.Outlined.Forum),
    HomeItem("check_update",
        Lang.getString(R.id.settings_title_checkupdate),
        Lang.getString(R.id.settings_sub_title_checkupdate),
        Icons.Filled.SystemUpdate, Icons.Outlined.SystemUpdate),
    HomeItem("license",
        Lang.getString(R.id.settings_title_license),
        Lang.getString(R.id.settings_sub_title_license),
        Icons.Filled.Description, Icons.Outlined.Description),
    HomeItem("website",
        Lang.getString(R.id.settings_title_webside),
        Lang.getString(R.id.settings_sub_title_webside),
        Icons.Filled.Home, Icons.Outlined.Home),
    HomeItem("donate",
        Lang.getString(R.id.settings_title_donate),
        Lang.getString(R.id.settings_sub_title_donate),
        Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.getString(R.id.app_settings_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(homeItems) { item ->
                HomeListItem(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
            item {
                // Version info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = "${Lang.getString(R.id.settings_title_version)} ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeListItem(
    item: HomeItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = item.iconOutlined,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            },
        )
    }
}