package com.demo.creditlimit.ui.kyc

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.demo.creditlimit.navigation.KycRouter
import com.demo.creditlimit.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val viewModel: EmergencyContactViewModel = viewModel(
        factory = application.container.viewModelFactory {
            EmergencyContactViewModel(
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
            is EmerUiState.SubmitSuccess -> {
                val profile = application.container.userRepository.getProfileBitmask()
                val next = KycRouter.resolveNextScreen(profile) ?: Screen.CreditResult
                navController.navigate(next.route) {
                    popUpTo(Screen.KycEmergencyContact.route) { inclusive = true }
                }
            }
            is EmerUiState.Ready -> state.errorMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KycBg)) {
        when (val state = uiState) {
            is EmerUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is EmerUiState.Ready -> {
                EmerReadyContent(state = state, navController = navController, viewModel = viewModel)
                state.activeSheet?.let { sheet ->
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeSheet() },
                        sheetState = sheetState
                    ) {
                        EmerSheetContent(sheet = sheet, state = state, viewModel = viewModel)
                    }
                }
            }
            else -> Unit
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EmerReadyContent(
    state: EmerUiState.Ready,
    navController: NavController,
    viewModel: EmergencyContactViewModel
) {
    val context = LocalContext.current
    val form = state.formState

    // Contact 1 picker
    val contact1Launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        viewModel.updateContact1Name(cursor.getString(0) ?: "")
                        viewModel.updateContact1Phone(cursor.getString(1) ?: "")
                    }
                }
            }
        }
    }

    // Contact 2 picker
    val contact2Launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        viewModel.updateContact2Name(cursor.getString(0) ?: "")
                        viewModel.updateContact2Phone(cursor.getString(1) ?: "")
                    }
                }
            }
        }
    }

    val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = "Contact Information", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.ic_pro_contact),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            ContactCard(
                title = "Contact Info 1",
                contact = form.contact1,
                onNameChange = { viewModel.updateContact1Name(it) },
                onPhoneChange = { viewModel.updateContact1Phone(it) },
                onRelationClick = { viewModel.openSheet(EmerSheet.RELATION_1) },
                onContactPickClick = { contact1Launcher.launch(pickContactIntent) }
            )

            Spacer(Modifier.height(12.dp))

            ContactCard(
                title = "Contact Info 2",
                contact = form.contact2,
                onNameChange = { viewModel.updateContact2Name(it) },
                onPhoneChange = { viewModel.updateContact2Phone(it) },
                onRelationClick = { viewModel.openSheet(EmerSheet.RELATION_2) },
                onContactPickClick = { contact2Launcher.launch(pickContactIntent) }
            )

            Spacer(Modifier.height(24.dp))
            KycNextButton(enabled = form.isComplete, isLoading = state.isSubmitting, onClick = { viewModel.submit() })
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ContactCard(
    title: String,
    contact: ContactState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRelationClick: () -> Unit,
    onContactPickClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KycCard)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KycText),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        KycPickerRow(
            label = "Relationship",
            value = contact.relation?.item,
            onClick = onRelationClick
        )
        KycInputRow(
            label = "Name",
            value = contact.name,
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.ic_contact),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onContactPickClick)
                )
            },
            onValueChange = onNameChange
        )
        KycInputRow(
            label = "Mobile Number",
            value = contact.phone,
            isLast = true,
            onValueChange = onPhoneChange
        )
    }
}

@Composable
private fun EmerSheetContent(sheet: EmerSheet, state: EmerUiState.Ready, viewModel: EmergencyContactViewModel) {
    val relations = state.kycConfig?.relation ?: emptyList()
    when (sheet) {
        EmerSheet.RELATION_1 -> KycSingleSelectSheet(
            items = relations,
            selectedEnum = state.formState.contact1.relation?.eneum,
            onSelect = { viewModel.selectRelation1(it) },
            onClose = { viewModel.closeSheet() }
        )
        EmerSheet.RELATION_2 -> KycSingleSelectSheet(
            items = relations,
            selectedEnum = state.formState.contact2.relation?.eneum,
            onSelect = { viewModel.selectRelation2(it) },
            onClose = { viewModel.closeSheet() }
        )
    }
}
