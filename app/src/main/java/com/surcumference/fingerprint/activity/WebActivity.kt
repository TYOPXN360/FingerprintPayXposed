package com.surcumference.fingerprint.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.surcumference.fingerprint.ui.theme.FingerprintPayTheme
import com.surcumference.fingerprint.util.Task
import com.surcumference.fingerprint.util.Umeng
import com.surcumference.fingerprint.util.UrlUtils
import com.surcumference.fingerprint.util.log.L

class WebActivity : ComponentActivity() {

    companion object {
        fun openUrl(context: Context, url: String) {
            try {
                val intent = Intent(context, WebActivity::class.java).apply {
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: ""
        setContent {
            FingerprintPayTheme {
                WebScreen(
                    url = url,
                    onBack = { finish() },
                    onOpenExternal = { externalUrl ->
                        if (!TextUtils.isEmpty(externalUrl)) {
                            UrlUtils.openUrl(this@WebActivity, externalUrl)
                        }
                    },
                    onWebViewError = { errorUrl ->
                        if (!TextUtils.isEmpty(errorUrl)) {
                            UrlUtils.openUrl(applicationContext, errorUrl)
                        }
                        Task.onMain(100) { finish() }
                    },
                )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebScreen(
    url: String,
    onBack: () -> Unit,
    onOpenExternal: (String) -> Unit,
    onWebViewError: (String) -> Unit,
) {
    var progress by remember { mutableIntStateOf(0) }
    var pageTitle by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle.ifEmpty { "Loading..." },
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
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
                .padding(padding),
        ) {
            // Progress indicator
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                if (TextUtils.isEmpty(url)) return false
                                val lurl = url.lowercase()
                                if (lurl.startsWith("http://") || lurl.startsWith("https://")) {
                                    if (lurl.endsWith(".apk") || lurl.endsWith(".zip") ||
                                        lurl.endsWith(".tar.gz") || lurl.contains("pan.baidu.com/s/")) {
                                        onOpenExternal(url)
                                        return true
                                    }
                                    view.loadUrl(url)
                                    return true
                                }
                                onOpenExternal(url)
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                url?.let { pageTitle = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageTitle = view?.title ?: ""
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                pageTitle = title ?: ""
                            }
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}