package com.demo.creditlimit.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.network.model.request2.RecgFaceReq
import com.demo.creditlimit.ui.kyc.KycBlue
import com.demo.creditlimit.ui.kyc.KycTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val ACC_H5_URL = "https://sdk.india.accuauth.com/silent-liveness-h5/index.html#"
private const val LIVENESS_TAG = "liveness_fragment"

class AccLivenessActivity : FragmentActivity() {

    internal val _state = MutableStateFlow(AccState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            val fragment = supportFragmentManager.findFragmentByTag(LIVENESS_TAG) as? LivenessFragment
            if (fragment?.canGoBack() == true) fragment.goBack()
            else { setResult(RESULT_CANCELED); finish() }
        }

        setContent {
            val state by _state.collectAsState()
            AccLivenessScreen(
                state = state,
                fragmentManager = supportFragmentManager,
                onRetry = {
                    _state.update { it.copy(errorMsg = null) }
                    val fragment = supportFragmentManager.findFragmentByTag(LIVENESS_TAG) as? LivenessFragment
                    fragment?.loadUrl(ACC_H5_URL)
                },
                onBack = { setResult(RESULT_CANCELED); finish() }
            )
        }
    }

    inner class AccJsCallback {
        @JavascriptInterface
        fun onMessage(imageId: String?, base64Image: String?, length: Int) {
            val cleanImg = base64Image?.substringAfter("base64,") ?: return
            _state.update { it.copy(isProcessing = true, errorMsg = null) }

            CoroutineScope(Dispatchers.IO).launch {
                val req = RecgFaceReq().apply {
                    livenessId = imageId ?: "placeholder"
                    livenessImg = cleanImg
                    this.imageID = imageId ?: ""
                }
                val repo = (application as CreditLimitApplication).container.userRepository
                val resp = repo.submitFaceRecognition(req)

                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false) }
                    when (resp?.conclusion) {
                        "PASS" -> { setResult(RESULT_OK); finish() }
                        "LOW_LIVE_SCORE" -> _state.update {
                            it.copy(errorMsg = "Liveness detection failed, please try again.")
                        }
                        "LOW_SIMILARITY" -> _state.update {
                            it.copy(errorMsg = "Face comparison failed, please try again.")
                        }
                        else -> _state.update {
                            it.copy(errorMsg = "Recognition failed, please try again.")
                        }
                    }
                }
            }
        }
    }
}

class LivenessFragment : Fragment() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return WebView(requireContext()).also { wv ->
            webView = wv
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
            }
            wv.addJavascriptInterface(
                (requireActivity() as AccLivenessActivity).AccJsCallback(),
                "callbackObj"
            )
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean {
                    view.loadUrl(request.url.toString())
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    (requireActivity() as AccLivenessActivity)._state.update { it.copy(progress = 0) }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    val param = JSONObject().apply {
                        put("language", "en")
                        put("region", "india")
                    }.toString()
                    view.evaluateJavascript("javascript:set_param('$param')", null)
                }
            }
            wv.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    (requireActivity() as AccLivenessActivity)._state.update {
                        it.copy(progress = newProgress)
                    }
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    request.grant(request.resources)
                }

                override fun getDefaultVideoPoster(): Bitmap =
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            wv.loadUrl(ACC_H5_URL)
        }
    }

    fun canGoBack() = webView?.canGoBack() == true
    fun goBack() = webView?.goBack()
    fun loadUrl(url: String) = webView?.loadUrl(url)

    override fun onDestroyView() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }
}

data class AccState(
    val progress: Int = 0,
    val isProcessing: Boolean = false,
    val errorMsg: String? = null
)

@Composable
private fun AccLivenessScreen(
    state: AccState,
    fragmentManager: androidx.fragment.app.FragmentManager,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = "Face Recognition", onBack = onBack, showService = false)

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = View.generateViewId()
                        val containerId = this.id
                        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {
                                removeOnAttachStateChangeListener(this)
                                if (fragmentManager.findFragmentByTag(LIVENESS_TAG) == null) {
                                    fragmentManager.beginTransaction()
                                        .replace(containerId, LivenessFragment(), LIVENESS_TAG)
                                        .commitNow()
                                }
                            }
                            override fun onViewDetachedFromWindow(v: View) {}
                        })
                    }
                }
            )

            if (state.progress < 100) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = KycBlue
                )
            }

            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    state.errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Verification Failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = onRetry) { Text("Retry") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Exit") }
            }
        )
    }
}
