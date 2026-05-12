package com.demo.creditlimit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.demo.creditlimit.navigation.AppNavGraph
import com.demo.creditlimit.ui.theme.CreditLimitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreditLimitTheme {
                AppNavGraph()
            }
        }
    }
}
