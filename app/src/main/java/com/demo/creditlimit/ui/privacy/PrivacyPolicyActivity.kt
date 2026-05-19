package com.demo.creditlimit.ui.privacy

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.network.manager.PermissionManager
import com.demo.creditlimit.ui.theme.CreditLimitTheme
import kotlinx.coroutines.launch

class PrivacyPolicyActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SHOW_AGREE = "show_agree"
        const val PRIVACY_URL = "https://admanfelly.github.io/privacypolicy/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showAgree = intent.getBooleanExtra(EXTRA_SHOW_AGREE, false)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContent {
            CreditLimitTheme {
                var showPermissionDialog by remember { mutableStateOf(false) }

                val saveAndFinish: () -> Unit = {
                    lifecycleScope.launch {
                        (applicationContext as CreditLimitApplication).container.savePrivacyAgreed()
                        finish()
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { finish() }

                BackHandler(enabled = showAgree) {
                    if (showPermissionDialog) finish()
                    else finishAffinity()
                }

                PrivacyPolicyScreen(
                    showAgree = showAgree,
                    onBack = { finish() },
                    onAgree = {
                        // Save agreed immediately on AGREE tap
                        lifecycleScope.launch {
                            (applicationContext as CreditLimitApplication).container.savePrivacyAgreed()
                        }
                        showPermissionDialog = true
                    }
                )

                if (showPermissionDialog) {
                    PermissionDialog(
                        onCancel = { finish() },
                        onConfirm = {
                            val missing = PermissionManager.getMissing(this@PrivacyPolicyActivity)
                            if (missing.isEmpty()) finish()
                            else permissionLauncher.launch(missing.toTypedArray())
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun PrivacyPolicyScreen(
    showAgree: Boolean,
    onBack: () -> Unit,
    onAgree: () -> Unit
) {
    val barColor = ComposeColor(0xFF1565C0)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(barColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!showAgree) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ComposeColor.White,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() }
                        .padding(8.dp)
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
            Text(
                text = "Privacy Policy",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ComposeColor.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(40.dp))
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    loadUrl(PrivacyPolicyActivity.PRIVACY_URL)
                }
            },
            modifier = Modifier.weight(1f)
        )

        if (showAgree) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
                    .clickable { onAgree() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AGREE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PermissionDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val blue = ComposeColor(0xFF1565C0)
    val permissionText = """
"CrediCash" requires the following permissions:

• Camera
Used to take photos for identity verification (KYC) and document upload.

• Phone State
Used to read device information and phone number for account security and fraud prevention.

• Location
Used to verify your location for loan application compliance and risk assessment.

• SMS
Used to auto-read OTP verification codes and analyze SMS history for credit assessment.

• Call Log
Used to analyze call history as part of the credit evaluation process.

• Notifications
Used to send you important updates about your loan application status and repayment reminders.

We only collect the data necessary to provide our services. Your information is kept secure and will not be shared with third parties without your consent.
    """.trimIndent()

    Dialog(
        onDismissRequest = { onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeColor.White)
                .padding(24.dp)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = permissionText,
                    fontSize = 14.sp,
                    color = ComposeColor(0xFF212121),
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(ComposeColor(0xFFE3F2FD))
                        .clickable { onCancel() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = blue
                    )
                }
                // Confirm
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(blue)
                        .clickable { onConfirm() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Confirm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ComposeColor.White
                    )
                }
            }
        }
    }
}
