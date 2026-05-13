package com.demo.creditlimit.ui.kyc

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.demo.creditlimit.CreditLimitApplication
import com.demo.creditlimit.R
import com.demo.creditlimit.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val viewModel: OcrViewModel = viewModel(
        factory = application.container.viewModelFactory {
            OcrViewModel(userRepository = application.container.userRepository)
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Screen-level state for captured image display
    var capturedDisplayUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capturedDisplayUri) {
        capturedBitmap = capturedDisplayUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { bmp ->
                            val maxDim = 720
                            val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                                val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                                Bitmap.createScaledBitmap(
                                    bmp,
                                    (bmp.width * ratio).toInt(),
                                    (bmp.height * ratio).toInt(),
                                    true
                                )
                            } else bmp
                            scaled.asImageBitmap()
                        }
                    }
                }.getOrNull()
            }
        }
    }

    // Camera state: both URI (for TakePicture) and File (for direct read after capture)
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // Camera launcher — reads image directly from File to avoid ContentResolver URI issues
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = pendingCameraUri ?: return@rememberLauncherForActivityResult
            val file = pendingCameraFile ?: return@rememberLauncherForActivityResult
            capturedDisplayUri = uri
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { compressImageFromFile(file) }
                if (bytes != null) viewModel.uploadAndOcr(bytes)
                else viewModel.reportError("Failed to read camera image")
            }
        }
    }

    // Creates a camera file+URI pair and stores them, then launches camera
    fun doLaunchCamera() {
        val dir = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
        val file = File(dir, "ocr_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraFile = file
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Camera permission launcher — requests CAMERA then calls doLaunchCamera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doLaunchCamera()
        else scope.launch { snackbarHostState.showSnackbar("Camera permission denied") }
    }

    // Checks permission then launches camera
    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            doLaunchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            capturedDisplayUri = it
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { compressImage(context, it) }
                bytes?.let { b -> viewModel.uploadAndOcr(b) }
            }
        }
    }

    // Effects
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is OcrUiState.SubmitSuccess ->
                navController.navigate(Screen.KycBindCard.route) {
                    popUpTo(Screen.KycOcr.route) { inclusive = true }
                }
            is OcrUiState.Ready -> {
                s.errorMsg?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearError()
                }
                if (s.ageWarningVisible) {
                    snackbarHostState.showSnackbar("Age must be between 20 and 60 years")
                    viewModel.clearAgeWarning()
                }
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KycBg)) {
        when (val state = uiState) {
            is OcrUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is OcrUiState.Ready -> {
                OcrReadyContent(
                    state = state,
                    capturedBitmap = capturedBitmap,
                    navController = navController,
                    viewModel = viewModel,
                    onCameraClick = { launchCamera() },
                    onGalleryClick = { galleryLauncher.launch("image/*") }
                )

                // Source picker sheet
                if (state.showSourceSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.hideSourceSheet() },
                        sheetState = sheetState
                    ) {
                        ImageSourceSheet(
                            onCamera = {
                                viewModel.hideSourceSheet()
                                launchCamera()
                            },
                            onGallery = {
                                viewModel.hideSourceSheet()
                                galleryLauncher.launch("image/*")
                            },
                            onClose = { viewModel.hideSourceSheet() }
                        )
                    }
                }

                // Confirm dialog
                if (state.showConfirmDialog && state.confirmState != null) {
                    ConfirmDialog(
                        state = state.confirmState,
                        isSubmitting = state.isSubmitting,
                        showDatePicker = state.showDatePicker,
                        showGenderSheet = state.showGenderSheet,
                        onDismiss = { viewModel.closeConfirmDialog() },
                        onUpdateAadNumber = { viewModel.updateAadNumber(it) },
                        onUpdateAadName = { viewModel.updateAadName(it) },
                        onOpenDatePicker = { viewModel.openDatePicker() },
                        onCloseDatePicker = { viewModel.closeDatePicker() },
                        onConfirmDate = { viewModel.updateBirthday(it); viewModel.closeDatePicker() },
                        onOpenGenderSheet = { viewModel.openGenderSheet() },
                        onCloseGenderSheet = { viewModel.closeGenderSheet() },
                        onSelectGender = { viewModel.updateGender(it) },
                        onAgree = { viewModel.agreeAndSubmit() }
                    )
                }
            }
            else -> Unit
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Main content ───────────────────────────────────────────────────────────

@Composable
private fun OcrReadyContent(
    state: OcrUiState.Ready,
    capturedBitmap: ImageBitmap?,
    navController: NavController,
    viewModel: OcrViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        KycTopBar(title = "KYC Documents", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Section header: "ID card info" + step progress + icon
            OcrSectionHeader()

            Spacer(Modifier.height(12.dp))

            // Photo requirements card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KycCard)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Photo Requirements",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KycText)
                )
                Spacer(Modifier.height(8.dp))
                Image(
                    painter = painterResource(R.drawable.ic_ocr_req),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Upload card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KycCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Upload Aadhaar Card Front",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KycText)
                )
                Spacer(Modifier.height(10.dp))
                UploadArea(
                    capturedBitmap = capturedBitmap,
                    isProcessing = state.isProcessing,
                    onClick = { viewModel.showSourceSheet() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Example image card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KycCard)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Aadhaar Card Example",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KycText)
                )
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = painterResource(R.drawable.ic_ocr_eg),
                    contentDescription = null,
                    modifier = Modifier .fillMaxWidth(0.8f)
                )
            }

            Spacer(Modifier.height(24.dp))
            KycNextButton(
                enabled = state.canProceed,
                isLoading = false,
                onClick = { viewModel.openConfirmDialog() }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Section header ─────────────────────────────────────────────────────────

@Composable
private fun OcrSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KycCard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_pro_ocr),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ── Upload area ────────────────────────────────────────────────────────────

@Composable
private fun UploadArea(
    capturedBitmap: ImageBitmap?,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isProcessing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_ocr_bg),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(KycBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Image source picker sheet ──────────────────────────────────────────────

@Composable
private fun ImageSourceSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        KycSheetHeader(title = "Select image source", onClose = onClose)
        HorizontalDivider(color = KycDivider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCamera)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Camera", style = TextStyle(fontSize = 15.sp, color = KycText))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KycLabel
            )
        }
        HorizontalDivider(color = KycDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onGallery)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Gallery", style = TextStyle(fontSize = 15.sp, color = KycText))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KycLabel
            )
        }
    }
}

// ── Confirm dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDialog(
    state: OcrConfirmState,
    isSubmitting: Boolean,
    showDatePicker: Boolean,
    showGenderSheet: Boolean,
    onDismiss: () -> Unit,
    onUpdateAadNumber: (String) -> Unit,
    onUpdateAadName: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onCloseDatePicker: () -> Unit,
    onConfirmDate: (String) -> Unit,
    onOpenGenderSheet: () -> Unit,
    onCloseGenderSheet: () -> Unit,
    onSelectGender: (Long) -> Unit,
    onAgree: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            // Close button top-right
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aadhaar card",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KycText)
                    )
                    Text(
                        text = "Please confirm information",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KycText)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "×", style = TextStyle(fontSize = 16.sp, color = KycLabel))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Aadhaar Number
            ConfirmInputRow(
                label = "Aadhaar Number(12)",
                value = state.aadNumber,
                onValueChange = onUpdateAadNumber,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            Spacer(Modifier.height(8.dp))

            // Name
            ConfirmInputRow(
                label = "Name",
                value = state.aadName,
                onValueChange = onUpdateAadName
            )
            Spacer(Modifier.height(8.dp))

            // Date of Birth
            ConfirmPickerRow(
                label = "Date of Birth",
                value = state.birthday.ifBlank { "Please select" },
                isWarning = state.isAgeOutOfRange,
                onClick = onOpenDatePicker
            )
            Spacer(Modifier.height(8.dp))

            // Gender
            ConfirmPickerRow(
                label = "Gender",
                value = state.genderLabel,
                onClick = onOpenGenderSheet
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Please make sure you are take clear photos of the original documents.",
                style = TextStyle(fontSize = 12.sp, color = KycLabel, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // AGREE button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (state.isComplete) KycBlue else KycDisabled)
                    .clickable(enabled = state.isComplete && !isSubmitting, onClick = onAgree)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "AGREE",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        OcrDatePickerDialog(
            birthday = state.birthday,
            onConfirm = onConfirmDate,
            onDismiss = onCloseDatePicker
        )
    }

    // Gender bottom sheet
    if (showGenderSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseGenderSheet,
            sheetState = sheetState
        ) {
            GenderSheet(
                selectedGender = state.gender,
                onSelect = onSelectGender,
                onClose = onCloseGenderSheet
            )
        }
    }
}

// ── Confirm field composables ──────────────────────────────────────────────

@Composable
private fun ConfirmInputRow(
    label: String,
    value: String,
    isWarning: Boolean = false,
    onValueChange: (String) -> Unit,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, color = KycText),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1.3f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = if (isWarning) Color(0xFFE53935) else KycText,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(KycBlue),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Please enter",
                            style = TextStyle(fontSize = 13.sp, color = KycLabel, textAlign = TextAlign.End),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun ConfirmPickerRow(
    label: String,
    value: String,
    isWarning: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, color = KycText),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1.3f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F5F5))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = if (isWarning) Color(0xFFE53935) else KycText,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KycLabel,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Date picker dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrDatePickerDialog(
    birthday: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = birthday.toDateMillis() ?: run {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(1990, 0, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = 1934..currentYear
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onConfirm(it.toDateString()) }
                    ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ── Gender sheet ───────────────────────────────────────────────────────────

@Composable
private fun GenderSheet(
    selectedGender: Long,
    onSelect: (Long) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        KycSheetHeader(title = "Select Gender", onClose = onClose)
        HorizontalDivider(color = KycDivider)
        listOf(1L to "Male", 2L to "Female").forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedGender == value,
                    onClick = { onSelect(value) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = KycBlue,
                        unselectedColor = KycLabel
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(text = label, style = TextStyle(fontSize = 15.sp, color = KycText))
            }
            HorizontalDivider(color = KycDivider, thickness = 0.5.dp)
        }
    }
}

// ── Image compression utility ──────────────────────────────────────────────

private fun compressImage(context: android.content.Context, uri: Uri): ByteArray? = runCatching {
    val original = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
    } ?: return null
    val maxDim = 720
    val scaled = if (original.width > maxDim || original.height > maxDim) {
        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
        Bitmap.createScaledBitmap(
            original,
            (original.width * ratio).toInt(),
            (original.height * ratio).toInt(),
            true
        )
    } else original
    val out = ByteArrayOutputStream()
    var quality = 90
    do {
        out.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        quality -= 10
    } while (out.size() > 200 * 1024 && quality > 10)
    out.toByteArray()
}.getOrNull()

// Reads camera photo directly from File — avoids ContentResolver URI permission issues
private fun compressImageFromFile(file: File): ByteArray? = runCatching {
    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    val maxDim = 720
    val scaled = if (original.width > maxDim || original.height > maxDim) {
        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
        Bitmap.createScaledBitmap(
            original,
            (original.width * ratio).toInt(),
            (original.height * ratio).toInt(),
            true
        )
    } else original
    val out = ByteArrayOutputStream()
    var quality = 90
    do {
        out.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        quality -= 10
    } while (out.size() > 200 * 1024 && quality > 10)
    out.toByteArray()
}.getOrNull()

// ── Date string helpers ────────────────────────────────────────────────────

private fun String.toDateMillis(): Long? = runCatching {
    val parts = split("-")
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.timeInMillis
}.getOrNull()

private fun Long.toDateString(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = this
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
