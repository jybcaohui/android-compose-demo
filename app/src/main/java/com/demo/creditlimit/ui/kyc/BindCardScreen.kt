package com.demo.creditlimit.ui.kyc

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.R
import com.demo.creditlimit.network.model.request2.IfscInfoResp
import com.demo.creditlimit.navigation.KycRouter
import com.demo.creditlimit.navigation.Screen

@Composable
fun BindCardScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val viewModel: BindCardViewModel = viewModel(
        factory = application.container.viewModelFactory {
            BindCardViewModel(
                configRepository = application.container.configRepository,
                userRepository = application.container.userRepository
            )
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BindCardUiState.SubmitSuccess -> {
                val profile = application.container.userRepository.getProfileBitmask()
                val next = KycRouter.resolveNextScreen(profile) ?: Screen.CreditResult
                navController.navigate(next.route) {
                    popUpTo(Screen.KycBindCard.route) { inclusive = true }
                }
            }
            is BindCardUiState.Ready -> state.cardState.errorMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KycBg)) {
        when (val state = uiState) {
            is BindCardUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is BindCardUiState.Ready ->
                BindCardReadyContent(state = state, navController = navController, viewModel = viewModel)
            else -> Unit
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BindCardReadyContent(
    state: BindCardUiState.Ready,
    navController: NavController,
    viewModel: BindCardViewModel
) {
    val context = LocalContext.current
    val card = state.cardState
    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = "Add Bank Account", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.ic_pro_card),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KycCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bank Card Information",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KycText),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                KycReadonlyRow(label = "Account Holder Name", value = card.holderName.ifBlank { null })

                KycInputRow(
                    label = "Bank Account",
                    value = card.bankCardFormatted,
                    onValueChange = { viewModel.updateBankCard(it) }
                )

                KycInputRow(
                    label = "Re-enter Bank Account",
                    value = card.reenterFormatted,
                    onValueChange = { viewModel.updateReenterBankCard(it) }
                )

                KycInputRow(
                    label = "IFSC Code",
                    value = card.ifscCode,
                    placeholder = "Please Enter (eg: SBIN0000001)",
                    isLast = true,
                    onValueChange = { viewModel.updateIfscCode(it) }
                )

                when {
                    card.isLoadingIfsc -> {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(24.dp),
                            strokeWidth = 2.dp,
                            color = KycBlue
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    card.ifscInfo != null -> {
                        Spacer(Modifier.height(8.dp))
                        IfscInfoCard(card.ifscInfo)
                        Spacer(Modifier.height(8.dp))
                    }
                    card.ifscError != null -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = card.ifscError,
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFFE53935))
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Look up my IFSC Code >",
                style = TextStyle(fontSize = 13.sp, color = KycBlue, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://rbi.org.in/Scripts/IFSCMICRDetails.aspx")
                        )
                        context.startActivity(intent)
                    }
            )

            Spacer(Modifier.height(24.dp))
            KycNextButton(
                enabled = card.isComplete,
                isLoading = card.isSubmitting,
                onClick = { viewModel.submit() }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun IfscInfoCard(info: IfscInfoResp) {
    val lines = listOf(
        "BANK" to info.bank,
        "STATE" to info.state,
        "DISTRICT" to info.district,
        "CITY" to info.city,
        "BRANCH" to info.branch
    ).filter { !it.second.isNullOrBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F4FD))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        lines.forEach { (label, value) ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = KycText)) {
                        append(label)
                    }
                    withStyle(SpanStyle(color = KycLabel)) {
                        append(": $value")
                    }
                },
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
