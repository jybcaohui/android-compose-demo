package com.demo.creditlimit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.creditlimit.navigation.AppNavGraph
import com.demo.creditlimit.ui.privacy.PrivacyPolicyActivity
import com.demo.creditlimit.ui.theme.CreditLimitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreditLimitTheme {
                val app = applicationContext as CreditLimitApplication
                val privacyAgreed by app.container.privacyAgreedFlow.collectAsStateWithLifecycle()

                val privacyLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { /* AppContainer.privacyAgreedFlow updates when PrivacyPolicyActivity saves */ }

                LaunchedEffect(privacyAgreed) {
                    if (privacyAgreed == false) {
                        privacyLauncher.launch(
                            Intent(this@MainActivity, PrivacyPolicyActivity::class.java).apply {
                                putExtra(PrivacyPolicyActivity.EXTRA_SHOW_AGREE, true)
                            }
                        )
                    }
                }

                if (privacyAgreed == true) {
                    AppNavGraph()
                }
            }
        }
    }
}
