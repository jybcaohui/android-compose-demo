package com.demo.creditlimit.ui.kyc

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.R
import com.demo.creditlimit.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val viewModel: BasicInfoViewModel = viewModel(
        factory = application.container.viewModelFactory {
            BasicInfoViewModel(
                configRepository = application.container.configRepository,
                userRepository = application.container.userRepository
            )
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BasicUiState.SubmitSuccess ->
                navController.navigate(Screen.KycBindCard.route) {
                    popUpTo(Screen.KycBasicInfo.route) { inclusive = true }
                }
            is BasicUiState.Ready -> state.errorMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KycBg)) {
        when (val state = uiState) {
            is BasicUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is BasicUiState.Ready -> {
                BasicReadyContent(state = state, navController = navController, viewModel = viewModel)
                state.activeSheet?.let { sheet ->
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeSheet() },
                        sheetState = sheetState
                    ) {
                        BasicSheetContent(sheet = sheet, state = state, viewModel = viewModel)
                    }
                }
            }
            else -> Unit
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BasicReadyContent(
    state: BasicUiState.Ready,
    navController: NavController,
    viewModel: BasicInfoViewModel
) {
    val form = state.formState
    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = "Basic Information", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.ic_pro_basic),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Personal Information card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KycCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Personal Information",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KycText),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                KycInputRow(
                    label = "PAN Number (10)",
                    value = form.panNumber,
                    placeholder = "Please Enter (eg: SSSSS0000A)",
                    onValueChange = { viewModel.updatePanNumber(it) }
                )
                KycPickerRow(label = "Marital Status", value = form.maritalStatus?.item, onClick = { viewModel.openSheet(BasicSheet.MARITAL_STATUS) })
                KycPickerRow(label = "Education", value = form.education?.item, onClick = { viewModel.openSheet(BasicSheet.EDUCATION) })
                KycPickerRow(label = "Religion", value = form.religion?.item, onClick = { viewModel.openSheet(BasicSheet.RELIGION) })
                KycPickerRow(label = "Expected Loan Amount", value = form.expectedLoanAmount?.item, onClick = { viewModel.openSheet(BasicSheet.EXPECTED_LOAN_AMOUNT) })
                KycPickerRow(label = "Loan Purpose", value = form.loanPurpose?.item, isLast = true, onClick = { viewModel.openSheet(BasicSheet.LOAN_PURPOSE) })

                // Optional section
                HorizontalDivider(color = KycDivider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleOptional() }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Optional items",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KycText)
                    )
                    Icon(
                        imageVector = if (state.optionalExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KycLabel,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (state.optionalExpanded) {
                    HorizontalDivider(color = KycDivider)
                    KycInputRow(label = "Email(optional)", value = form.email, onValueChange = { viewModel.updateEmail(it) })
                    KycInputRow(label = "WhatsApp Number(optional)", value = form.whatsApp, onValueChange = { viewModel.updateWhatsApp(it) })
                    KycInputRow(label = "Facebook Account(optional)", value = form.facebook, onValueChange = { viewModel.updateFacebook(it) })
                    KycInputRow(label = "LinkedIn Account(optional)", value = form.linkedIn, isLast = true, onValueChange = { viewModel.updateLinkedIn(it) })
                }
            }

            Spacer(Modifier.height(24.dp))
            KycNextButton(enabled = form.isComplete, isLoading = state.isSubmitting, onClick = { viewModel.submit() })
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BasicSheetContent(sheet: BasicSheet, state: BasicUiState.Ready, viewModel: BasicInfoViewModel) {
    val config = state.kycConfig
    when (sheet) {
        BasicSheet.MARITAL_STATUS -> KycSingleSelectSheet(
            items = config?.maritalStatus ?: emptyList(),
            selectedEnum = state.formState.maritalStatus?.eneum,
            onSelect = { viewModel.selectMaritalStatus(it); viewModel.closeSheet() },
            onClose = { viewModel.closeSheet() }
        )
        BasicSheet.EDUCATION -> KycSingleSelectSheet(
            items = config?.education ?: emptyList(),
            selectedEnum = state.formState.education?.eneum,
            onSelect = { viewModel.selectEducation(it); viewModel.closeSheet() },
            onClose = { viewModel.closeSheet() }
        )
        BasicSheet.RELIGION -> KycSingleSelectSheet(
            items = config?.religion ?: emptyList(),
            selectedEnum = state.formState.religion?.eneum,
            onSelect = { viewModel.selectReligion(it); viewModel.closeSheet() },
            onClose = { viewModel.closeSheet() }
        )
        BasicSheet.EXPECTED_LOAN_AMOUNT -> KycSingleSelectSheet(
            items = config?.expectedLoanAmount ?: emptyList(),
            selectedEnum = state.formState.expectedLoanAmount?.eneum,
            onSelect = { viewModel.selectExpectedLoanAmount(it); viewModel.closeSheet() },
            onClose = { viewModel.closeSheet() }
        )
        BasicSheet.LOAN_PURPOSE -> KycSingleSelectSheet(
            items = config?.loanPurpose ?: emptyList(),
            selectedEnum = state.formState.loanPurpose?.eneum,
            onSelect = { viewModel.selectLoanPurpose(it); viewModel.closeSheet() },
            onClose = { viewModel.closeSheet() }
        )
    }
}
