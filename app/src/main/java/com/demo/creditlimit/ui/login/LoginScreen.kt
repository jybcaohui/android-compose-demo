package com.demo.creditlimit.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.R
import com.demo.creditlimit.navigation.Screen
import kotlinx.coroutines.delay

private val LoginBlue = Color(0xFF1B7FE8)
private val LoginGray = Color(0xFFB0BEC5)
private val LoginHint = Color(0xFFB0BEC5)
private val LoginText = Color(0xFF212121)

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val loginViewModel: LoginViewModel = viewModel(
        factory = application.container.viewModelFactory {
            LoginViewModel(
                authRepository = application.container.authRepository,
                appName = application.container.appName
            )
        }
    )

    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    val deviceId = remember { application.container.gaidManager.getGaid() ?: "unknown" }

    val isLoading = uiState is LoginUiState.Loading
    val isOtpSent = uiState is LoginUiState.OtpSent
    val isPhoneValid = phone.length == 10 && phone.firstOrNull()?.let { it in "6789" } == true
    val isButtonEnabled = !isLoading && if (isOtpSent) otp.length == 4 else isPhoneValid

    var countdownSeconds by remember { mutableIntStateOf(0) }
    var countdownKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(isOtpSent) {
        if (isOtpSent) countdownKey++
    }

    LaunchedEffect(countdownKey) {
        if (countdownKey > 0) {
            countdownSeconds = 100
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                navController.navigate(Screen.KycBasicInfo.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is LoginUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                loginViewModel.resetError()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(top = 40.dp,start = 4.dp,
                        end = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Log in",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LoginText
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(56.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Welcome Back",
                    color = LoginBlue,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.height(40.dp))

                PhoneInputField(
                    value = phone,
                    onValueChange = { if (it.length <= 10) phone = it },
                    enabled = !isOtpSent && !isLoading
                )

                if (isOtpSent) {
                    Spacer(Modifier.height(24.dp))
                    OtpInputField(
                        value = otp,
                        onValueChange = { if (it.length <= 4) otp = it },
                        countdownSeconds = countdownSeconds,
                        onResend = {
                            loginViewModel.sendVcode(phone)
                            countdownKey++
                        },
                        enabled = !isLoading
                    )
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (isButtonEnabled) LoginBlue else LoginGray)
                        .clickable(enabled = isButtonEnabled) {
                            if (isOtpSent) {
                                loginViewModel.login(phone, otp, deviceId)
                            } else {
                                loginViewModel.sendVcode(phone)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Login",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                PrivacyPolicyText()

                Spacer(Modifier.height(32.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                color = if (enabled) LoginText else LoginText.copy(alpha = 0.5f),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(LoginHint)
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = TextStyle(fontSize = 16.sp, color = LoginText),
                cursorBrush = SolidColor(LoginBlue),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter Your Phone Number",
                            color = LoginHint,
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                    innerTextField()
                }
            )
        }
        HorizontalDivider(color = LoginHint, thickness = 1.dp)
    }
}

@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    countdownSeconds: Int,
    onResend: () -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(fontSize = 16.sp, color = LoginText),
                cursorBrush = SolidColor(LoginBlue),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter SMS verification code",
                            color = LoginHint,
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(Modifier.width(8.dp))
            if (countdownSeconds > 0) {
                Text(
                    text = "${countdownSeconds}S",
                    color = LoginBlue,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
            } else {
                Text(
                    text = "Resend",
                    color = LoginBlue,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.clickable(onClick = onResend)
                )
            }
        }
        HorizontalDivider(color = LoginHint, thickness = 1.dp)
    }
}

@Composable
private fun PrivacyPolicyText() {
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF9E9E9E), fontSize = 12.sp)) {
            append("By continuing you agree to our ")
        }
        pushStringAnnotation(tag = "URL", annotation = "https://c438b.com/PrivacyPolicy/")
        withStyle(SpanStyle(color = LoginBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
            append("Privacy Policy")
        }
        pop()
    }
    ClickableText(
        text = annotatedString,
        style = TextStyle(textAlign = TextAlign.Center),
        onClick = { /* WebView launch handled by caller if needed */ }
    )
}
