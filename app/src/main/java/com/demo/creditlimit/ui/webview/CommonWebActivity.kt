package com.demo.creditlimit.ui.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.demo.creditlimit.ui.kyc.KycBlue
import com.demo.creditlimit.ui.kyc.KycTopBar

class CommonWebActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val url = intent.getStringExtra(EXTRA_URL) ?: ""

        var webViewRef: WebView? = null

        onBackPressedDispatcher.addCallback(this) {
            val wv = webViewRef
            if (wv != null && wv.canGoBack()) wv.goBack()
            else finish()
        }

        setContent {
            WebScreen(
                title = title,
                url = url,
                onBack = { finish() },
                onWebViewCreated = { webViewRef = it }
            )
        }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_URL = "extra_url"

        fun start(context: Context, title: String, url: String) {
            context.startActivity(
                Intent(context, CommonWebActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_URL, url)
                }
            )
        }
    }
}

@Composable
private fun WebScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    var progress by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = title, onBack = onBack, showService = false)
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).also { wv ->
                        wv.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        wv.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                view.loadUrl(request.url.toString())
                                return true
                            }
                        }
                        wv.webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        onWebViewCreated(wv)
                        wv.loadUrl(url)
                    }
                }
            )
            if (progress < 100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = KycBlue
                )
            }
        }
    }
}
