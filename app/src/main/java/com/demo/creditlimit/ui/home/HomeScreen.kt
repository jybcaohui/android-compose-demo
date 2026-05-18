package com.demo.creditlimit.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.navigation.Screen
import com.demo.creditlimit.network.manager.PermissionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val homeViewModel: HomeViewModel = viewModel(
        factory = application.container.viewModelFactory {
            HomeViewModel(
                userRepository = application.container.userRepository,
                isLoggedIn = application.container.isLoggedIn
            )
        }
    )

    val isLoggedIn by application.container.isLoggedIn.collectAsStateWithLifecycle()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is HomeUiState.Loading
    val scope = rememberCoroutineScope()

    var pendingKycRoute by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            scope.launch { application.container.runtimeManager.uploadAsync() }
            pendingKycRoute?.let { navController.navigate(it) }
        } else {
            val hasPermanent = results.entries.any { (perm, granted) ->
                !granted && !androidx.core.app.ActivityCompat
                    .shouldShowRequestPermissionRationale(context as androidx.activity.ComponentActivity, perm)
            }
            if (hasPermanent) showSettingsDialog = true
        }
        pendingKycRoute = null
        homeViewModel.resetNavigation()
    }

    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.NavigateTo) {
            val screen = (uiState as HomeUiState.NavigateTo).screen
            if (screen == Screen.Login) {
                navController.navigate(screen.route)
                homeViewModel.resetNavigation()
            } else {
                val missing = PermissionManager.getMissing(context)
                if (missing.isEmpty()) {
                    scope.launch { application.container.runtimeManager.uploadAsync() }
                    navController.navigate(screen.route)
                    homeViewModel.resetNavigation()
                } else {
                    pendingKycRoute = screen.route
                    permissionsLauncher.launch(missing.toTypedArray())
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Please go to Settings to enable the required permissions to continue.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credit Limit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Find out your eligible credit limit in minutes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { homeViewModel.handleGetStarted() },
                enabled = isLoggedIn != null && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Get Started")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            DebugPageList(navController)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DebugPageList(navController: NavController) {
    val pages = listOf(
        "Login" to Screen.Login.route,
        "Emergency Contact" to Screen.KycEmergencyContact.route,
        "Basic Info" to Screen.KycBasicInfo.route,
        "Supplementary Info" to Screen.KycSupplementaryInfo.route,
        "OCR" to Screen.KycOcr.route,
        "Face Recognition" to Screen.KycFace.route,
        "Bind Card" to Screen.KycBindCard.route,
        "Credit Result" to Screen.CreditResult.route,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DEBUG — Page List",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        pages.forEachIndexed { index, (label, route) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(route) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 15.sp, color = Color(0xFF212121))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (index < pages.lastIndex) HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}
