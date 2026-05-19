package com.demo.creditlimit.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.R
import com.demo.creditlimit.navigation.Screen
import com.demo.creditlimit.network.manager.PermissionManager
import com.demo.creditlimit.ui.privacy.PrivacyPolicyActivity
import com.demo.creditlimit.ui.webview.H5WebviewActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HomeBgStart = Color(0xFF4DD0E1)
private val HomeBgEnd   = Color(0xFF29B6F6)
private val TabSelected = Color(0xFF1565C0)
private val TabUnselected = Color(0xFF9E9E9E)

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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingKycRoute by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var backPressedOnce by remember { mutableStateOf(false) }
    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finishAffinity()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

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
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HomeTab(
                    isLoading = isLoading,
                    isLoggedIn = isLoggedIn,
                    onGetStarted = { homeViewModel.handleGetStarted() }
                )
                1 -> PersonalTab(
                    navController = navController,
                    onGetStarted = { homeViewModel.handleGetStarted() }
                )
            }
        }

        // Bottom navigation bar
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

// ── Home Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun HomeTab(
    isLoading: Boolean,
    isLoggedIn: Boolean?,
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HomeBgStart, HomeBgEnd)))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_home_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 20.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(16.dp))

            // Center image with Apply Now button overlaid at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_home_center),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HomeBgEnd)
                        .clickable(enabled = isLoggedIn != null && !isLoading) { onGetStarted() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = "Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Products row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(vertical = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Choose our products",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TabSelected,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            R.drawable.ic_home_01 to "Apply fast",
                            R.drawable.ic_home_02 to "Coupon",
                            R.drawable.ic_home_03 to "Long term",
                            R.drawable.ic_home_04 to "Low interest"
                        ).forEach { (icon, label) ->
                            ProductItem(icon = icon, label = label)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "does not provide direct loan services. Instead, based on your personal information and credit profile, we will recommend suitable loan products for you.",
                fontSize = 11.sp,
                lineHeight = 14.sp, // 行高
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductItem(icon: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFF424242))
    }
}

// ── Personal Tab ──────────────────────────────────────────────────────────────

@Composable
private fun PersonalTab(navController: NavController, onGetStarted: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication
    val isLoggedIn by application.container.isLoggedIn.collectAsStateWithLifecycle()
    val phone = application.container.tokenManager.getCachedPhone() ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header: avatar + phone
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .then(
                    if (isLoggedIn != true) Modifier.clickable { onGetStarted() }
                    else Modifier
                )
                .padding(horizontal = 20.dp, vertical = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_header),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (isLoggedIn == true && phone.isNotBlank()) phone else "Not logged in",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLoggedIn == true && phone.isNotBlank()) Color(0xFF212121) else Color(0xFF6E6E6E)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Menu items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            PersonalMenuItem(
                icon = R.drawable.ic_privacy,
                label = "Privacy Policy",
                onClick = {
                    context.startActivity(
                        Intent(context, PrivacyPolicyActivity::class.java).apply {
                            putExtra(PrivacyPolicyActivity.EXTRA_SHOW_AGREE, false)
                        }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
            PersonalMenuItem(
                icon = R.drawable.ic_service,
                label = "Contact Us",
                onClick = {}
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
            PersonalMenuItem(
                icon = R.drawable.ic_setting,
                label = "Settings",
                onClick = {}
            )
        }

        Spacer(Modifier.height(12.dp))

        // Disclaimer banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Please contact the official customer service through the APP. We do not have any external official website. Other \"Official\" website or impersonation of customer service for refunds and other operations are fraud.",
                fontSize = 13.sp,
                color = Color(0xFFE65100),
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Debug page list
        DebugPageList(navController = navController)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PersonalMenuItem(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────────────────────

@Composable
private fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Home",     R.drawable.ic_home_selected,   R.drawable.ic_home_unselected),
        Triple("Personal", R.drawable.ic_my_selected,     R.drawable.ic_my_unselected)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(if (isSelected) selectedIcon else unselectedIcon),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isSelected) TabSelected else TabUnselected
                    )
                }
            }
        }
    }
}

// ── Debug Page List ───────────────────────────────────────────────────────────

@Composable
private fun DebugPageList(navController: NavController) {
    val context = LocalContext.current
    val pages = listOf(
        "Login"              to Screen.Login.route,
        "Emergency Contact"  to Screen.KycEmergencyContact.route,
        "Basic Info"         to Screen.KycBasicInfo.route,
        "Supplementary Info" to Screen.KycSupplementaryInfo.route,
        "OCR"                to Screen.KycOcr.route,
        "Face Recognition"   to Screen.KycFace.route,
        "Bind Card"          to Screen.KycBindCard.route,
        "Credit Result"      to Screen.CreditResult.route,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Text(
            text = "DEBUG — Page List",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        pages.forEach { (label, route) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(route) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFEEEEEE))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { context.startActivity(Intent(context, H5WebviewActivity::class.java)) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "H5 Webview", fontSize = 15.sp, color = Color(0xFF212121))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
