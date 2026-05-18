package com.demo.creditlimit.ui.kyc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.navigation.KycRouter
import com.demo.creditlimit.navigation.Screen
import com.demo.creditlimit.ui.webview.AccLivenessActivity
import kotlinx.coroutines.launch

@Composable
fun FaceScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication
    val scope = rememberCoroutineScope()
    var launched by remember { mutableStateOf(false) }

    val livenessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val profile = application.container.userRepository.getProfileBitmask()
                val next = KycRouter.resolveNextScreen(profile) ?: Screen.CreditResult
                navController.navigate(next.route) {
                    popUpTo(Screen.KycFace.route) { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            livenessLauncher.launch(Intent(context, AccLivenessActivity::class.java))
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                livenessLauncher.launch(Intent(context, AccLivenessActivity::class.java))
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
