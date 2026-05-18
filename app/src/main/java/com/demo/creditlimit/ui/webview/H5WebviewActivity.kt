package com.demo.creditlimit.ui.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ProcessLifecycleOwner
import com.demo.creditlimit.BuildConfig
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.MainActivity
import com.demo.creditlimit.network.manager.PermissionManager
import com.demo.creditlimit.network.model.request2.AndroidJsMsg
import com.demo.creditlimit.ui.kyc.KycBlue
import com.demo.creditlimit.ui.kyc.KycTopBar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

private const val H5_TEST_URL = "http://api.rp735.xyz/fnzln27/finzyloan.html#/"
private const val H5_FRAGMENT_TAG = "h5_webview_fragment"

class H5WebviewActivity : FragmentActivity(), LifecycleEventObserver {

    internal val appContainer get() = (application as CreditLimitApplication).container
    private val gson = Gson()

    private var pendingCameraImgType = ""
    private var pendingGalleryImgType = ""
    private var cameraPhotoFile: File? = null
    private var cameraPermTarget = CameraPermTarget.PHOTO
    private var processLifecycleStarted = false

    internal val _state = MutableStateFlow(H5State())

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var contactsLauncher: ActivityResultLauncher<Void?>
    private lateinit var cameraPermLauncher: ActivityResultLauncher<String>
    private lateinit var devicePermLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var livenessLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val file = cameraPhotoFile ?: run {
                    callJS(AndroidJsMsg().apply { key = JsMsgKey.CAMERA; value = "-1" })
                    return@registerForActivityResult
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    handleImageResult(null, file, pendingCameraImgType, JsMsgKey.CAMERA)
                }
            } else {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.CAMERA; value = "-1" })
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: run {
                    callJS(AndroidJsMsg().apply { key = JsMsgKey.GALLERY; value = "-1" })
                    return@registerForActivityResult
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    handleImageResult(uri, null, pendingGalleryImgType, JsMsgKey.GALLERY)
                }
            } else {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.GALLERY; value = "-1" })
            }
        }

        contactsLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if (uri == null) {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.CONTACTS; value = "-1" })
                return@registerForActivityResult
            }
            runCatching {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                        val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)) ?: ""
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        val phone = phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) pc.getString(0) ?: "" else ""
                        } ?: ""
                        callJS(AndroidJsMsg().apply {
                            key = JsMsgKey.CONTACTS; cName = name; cPhone = phone; value = "1"
                        })
                    } else {
                        callJS(AndroidJsMsg().apply { key = JsMsgKey.CONTACTS; value = "-1" })
                    }
                } ?: callJS(AndroidJsMsg().apply { key = JsMsgKey.CONTACTS; value = "-1" })
            }.onFailure {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.CONTACTS; value = "-1" })
            }
        }

        cameraPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                when (cameraPermTarget) {
                    CameraPermTarget.PHOTO -> launchCamera()
                    CameraPermTarget.LIVENESS -> livenessLauncher.launch(
                        Intent(this, AccLivenessActivity::class.java)
                    )
                }
            } else {
                val permanent = PermissionManager.isPermanentlyDenied(this, Manifest.permission.CAMERA)
                if (permanent) showPermissionDialog()
                when (cameraPermTarget) {
                    CameraPermTarget.PHOTO -> callJS(AndroidJsMsg().apply {
                        key = JsMsgKey.CAMERA; value = if (permanent) "-2" else "-1"
                    })
                    CameraPermTarget.LIVENESS -> callJS(AndroidJsMsg().apply {
                        key = JsMsgKey.LIVENESS; value = "-1"
                    })
                }
            }
        }

        devicePermLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                lifecycleScope.launch {
                    appContainer.runtimeManager.uploadAsync()
                    callJS(AndroidJsMsg().apply { key = JsMsgKey.DEVICE_INFO; value = "1" })
                }
            } else {
                val hasPermanent = results.entries.any { (perm, ok) ->
                    !ok && PermissionManager.isPermanentlyDenied(this, perm)
                }
                if (hasPermanent) showPermissionDialog()
                callJS(AndroidJsMsg().apply { key = JsMsgKey.DEVICE_INFO; value = if (hasPermanent) "-2" else "-1" })
            }
        }

        livenessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.LIVENESS; value = "PASS" })
            } else {
                callJS(AndroidJsMsg().apply { key = JsMsgKey.LIVENESS; value = "-1" })
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        onBackPressedDispatcher.addCallback(this) {
            callJS(AndroidJsMsg().apply { key = JsMsgKey.BACK })
            val frag = supportFragmentManager.findFragmentByTag(H5_FRAGMENT_TAG) as? H5WebviewFragment
            if (frag?.canGoBack() == true) frag.goBack() else finish()
        }

        setContent {
            val state by _state.collectAsState()
            H5WebviewScreen(state = state, fragmentManager = supportFragmentManager)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (processLifecycleStarted) {
                    callJS(AndroidJsMsg().apply { key = JsMsgKey.APP_LIFECYCLE; value = "1" })
                } else {
                    processLifecycleStarted = true
                }
            }
            Lifecycle.Event.ON_STOP -> callJS(AndroidJsMsg().apply { key = JsMsgKey.APP_LIFECYCLE; value = "2" })
            else -> {}
        }
    }

    override fun onRestart() {
        super.onRestart()
        buildKey13Msg().let { callJS(it) }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        super.onDestroy()
    }

    internal fun callJS(msg: AndroidJsMsg) {
        try {
            val json = JSONObject.quote(gson.toJson(msg))
            lifecycleScope.launch(Dispatchers.Main) {
                (supportFragmentManager.findFragmentByTag(H5_FRAGMENT_TAG) as? H5WebviewFragment)
                    ?.evaluateJS("javascript:callJS($json)")
            }
        } catch (_: Exception) {}
    }

    internal fun buildKey7Msg() = AndroidJsMsg().apply {
        key = JsMsgKey.APP_INFO
        appId = BuildConfig.APPLICATION_ID
        deviceId = appContainer.gaidManager.getGaid() ?: ""
        adjustId = ""
        adjustData = ""
        referrer = ""
        vName = BuildConfig.VERSION_NAME
        vCode = "${BuildConfig.VERSION_CODE}"
        value = "1"
    }

    internal fun buildKey10Msg() = AndroidJsMsg().apply {
        key = JsMsgKey.TOKEN_INFO
        token = appContainer.tokenManager.getCachedToken() ?: ""
        phone = appContainer.tokenManager.getCachedPhone() ?: ""
        value = "1"
    }

    internal fun buildKey13Msg(): AndroidJsMsg {
        val perms = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CAMERA
        )
        val allGranted = perms.all { PermissionManager.isGranted(this, it) }
        val value = when {
            allGranted -> "1"
            perms.any { !PermissionManager.isGranted(this, it) && PermissionManager.isPermanentlyDenied(this, it) } -> "-2"
            else -> "-1"
        }
        return AndroidJsMsg().apply { key = JsMsgKey.PERMISSION; this.value = value }
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera_photos").also { it.mkdirs() }
        val file = File(dir, "h5_${System.currentTimeMillis()}.jpg")
        cameraPhotoFile = file
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    private suspend fun handleImageResult(uri: Uri?, file: File?, imgType: String, jsKey: Int) {
        val bytes = if (file != null) compressImageFromFile(file)
                    else if (uri != null) compressImageFromUri(this, uri)
                    else null
        if (bytes == null) {
            callJS(AndroidJsMsg().apply { key = jsKey; value = "-1" })
            return
        }
        val imageUrl = appContainer.userRepository.uploadImage(bytes)
        if (imageUrl == null) {
            callJS(AndroidJsMsg().apply { key = jsKey; value = "-1" })
            return
        }
        val base64 = "data:image/*;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        callJS(AndroidJsMsg().apply {
            key = jsKey
            value = "1"
            imgUrl = imageUrl
            imgBase64 = base64
            this.imgType = imgType
        })
    }

    private fun showPermissionDialog() {
        _state.update { it.copy(showPermDialog = true) }
    }

    inner class AndroidJsInterface {
        @JavascriptInterface
        fun callAndroid(data: String) {
            android.util.Log.d("H5Webview", "callAndroid key=${runCatching { gson.fromJson(data, AndroidJsMsg::class.java).key }.getOrDefault(-1)} data=$data")
            val msg = runCatching { gson.fromJson(data, AndroidJsMsg::class.java) }.getOrNull() ?: return
            when (msg.key) {
                JsMsgKey.CAMERA -> {
                    pendingCameraImgType = msg.imgType ?: ""
                    cameraPermTarget = CameraPermTarget.PHOTO
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (PermissionManager.isGranted(this@H5WebviewActivity, Manifest.permission.CAMERA)) {
                            launchCamera()
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
                JsMsgKey.GALLERY -> {
                    pendingGalleryImgType = msg.imgType ?: ""
                    lifecycleScope.launch(Dispatchers.Main) {
                        galleryLauncher.launch(Intent(ACTION_PICK).apply { type = "image/*" })
                    }
                }
                JsMsgKey.CONTACTS -> lifecycleScope.launch(Dispatchers.Main) { contactsLauncher.launch(null) }
                JsMsgKey.OPEN_LINK -> {
                    callJS(AndroidJsMsg().apply { key = JsMsgKey.OPEN_LINK; value = "1" })
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (msg.out) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(msg.link ?: "")))
                        } else {
                            CommonWebActivity.start(this@H5WebviewActivity, msg.linkTitle ?: "", msg.link ?: "")
                        }
                    }
                }
                JsMsgKey.GOOGLE_REVIEW -> callJS(AndroidJsMsg().apply { key = JsMsgKey.GOOGLE_REVIEW; value = "1" })
                JsMsgKey.DEVICE_INFO -> {
                    val needed = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION)
                    val missing = needed.filter { !PermissionManager.isGranted(this@H5WebviewActivity, it) }
                    if (missing.isEmpty()) {
                        lifecycleScope.launch {
                            appContainer.runtimeManager.uploadAsync()
                            callJS(AndroidJsMsg().apply { key = JsMsgKey.DEVICE_INFO; value = "1" })
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) { devicePermLauncher.launch(missing.toTypedArray()) }
                    }
                }
                JsMsgKey.APP_INFO   -> callJS(buildKey7Msg())
                JsMsgKey.TOKEN_INFO -> callJS(buildKey10Msg())
                JsMsgKey.LOGOUT -> lifecycleScope.launch {
                    appContainer.tokenManager.clearTokens()
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@H5WebviewActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
                JsMsgKey.LIVENESS -> {
                    cameraPermTarget = CameraPermTarget.LIVENESS
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (PermissionManager.isGranted(this@H5WebviewActivity, Manifest.permission.CAMERA)) {
                            livenessLauncher.launch(Intent(this@H5WebviewActivity, AccLivenessActivity::class.java))
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
                JsMsgKey.PERMISSION -> callJS(buildKey13Msg())
            }
        }
    }
}

private enum class CameraPermTarget { PHOTO, LIVENESS }

data class H5State(
    val progress: Int = 0,
    val showPermDialog: Boolean = false
)

class H5WebviewFragment : Fragment() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return WebView(requireContext()).also { wv ->
            webView = wv
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                allowContentAccess = true
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false
            }
            wv.addJavascriptInterface(
                (requireActivity() as H5WebviewActivity).AndroidJsInterface(),
                "Android"
            )
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    view.loadUrl(request.url.toString())
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    (requireActivity() as H5WebviewActivity)._state.update { it.copy(progress = 0) }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    val activity = requireActivity() as H5WebviewActivity
                    activity._state.update { it.copy(progress = 100) }
                    activity.callJS(activity.buildKey7Msg())
                    activity.callJS(activity.buildKey10Msg())
                    activity.callJS(activity.buildKey13Msg())
                }
            }
            wv.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    (requireActivity() as H5WebviewActivity)._state.update { it.copy(progress = newProgress) }
                }
            }
            wv.loadUrl(H5_TEST_URL)
        }
    }

    fun canGoBack() = webView?.canGoBack() == true
    fun goBack() = webView?.goBack()
    fun evaluateJS(script: String) = webView?.evaluateJavascript(script, null)

    override fun onDestroyView() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }
}

@Composable
private fun H5WebviewScreen(
    state: H5State,
    fragmentManager: androidx.fragment.app.FragmentManager
) {
    Column(modifier = Modifier.fillMaxSize()) {

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
                                if (fragmentManager.findFragmentByTag(H5_FRAGMENT_TAG) == null) {
                                    fragmentManager.beginTransaction()
                                        .replace(containerId, H5WebviewFragment(), H5_FRAGMENT_TAG)
                                        .commitNow()
                                }
                            }
                            override fun onViewDetachedFromWindow(v: View) {}
                        })
                    }
                }
            )

            if (state.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = KycBlue
                )
            }
        }
    }

    if (state.showPermDialog) {
        val activityContext = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permission Required") },
            text = { Text("To provide you with better service, please go to the app settings page and enable all permissions.") },
            confirmButton = {
                TextButton(onClick = {
                    activityContext.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activityContext.packageName, null)
                        }
                    )
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    (activityContext as? H5WebviewActivity)?._state?.update { it.copy(showPermDialog = false) }
                }) { Text("Cancel") }
            }
        )
    }
}

private fun compressImageFromFile(file: File): ByteArray? = runCatching {
    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    compressBitmap(original)
}.getOrNull()

private fun compressImageFromUri(context: android.content.Context, uri: Uri): ByteArray? = runCatching {
    val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    compressBitmap(original)
}.getOrNull()

private fun compressBitmap(original: Bitmap): ByteArray {
    val maxDim = 720
    val scaled = if (original.width > maxDim || original.height > maxDim) {
        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
        Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
    } else original
    val out = ByteArrayOutputStream()
    var quality = 90
    do {
        out.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        quality -= 10
    } while (out.size() > 250 * 1024 && quality > 10)
    return out.toByteArray()
}
