package com.demo.creditlimit.ui.kyc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

private val SuppBlue = Color(0xFF1B7FE8)
private val SuppBg = Color(0xFFF5F5F5)
private val SuppCard = Color(0xFFFFFFFF)
private val SuppLabel = Color(0xFF9E9E9E)
private val SuppText = Color(0xFF212121)
private val SuppDivider = Color(0xFFEEEEEE)
private val SuppDisabled = Color(0xFFB0BEC5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementaryInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as CreditLimitApplication

    val viewModel: SupplementaryInfoViewModel = viewModel(
        factory = application.container.viewModelFactory {
            SupplementaryInfoViewModel(
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
            is SuppUiState.SubmitSuccess -> {
                navController.navigate(Screen.KycEmergencyContact.route) {
                    popUpTo(Screen.KycSupplementaryInfo.route) { inclusive = true }
                }
            }
            is SuppUiState.Ready -> {
                state.errorMsg?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearError()
                }
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SuppBg)) {
        when (val state = uiState) {
            is SuppUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SuppUiState.Ready -> {
                SuppReadyContent(
                    state = state,
                    navController = navController,
                    viewModel = viewModel
                )
                // Bottom sheets
                state.activeSheet?.let { sheet ->
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeSheet() },
                        sheetState = sheetState
                    ) {
                        SuppSheetContent(sheet = sheet, state = state, viewModel = viewModel)
                    }
                }
            }
            else -> Unit
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SuppReadyContent(
    state: SuppUiState.Ready,
    navController: NavController,
    viewModel: SupplementaryInfoViewModel
) {
    val form = state.formState
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuppCard)
                .padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)
                .height(44.dp)
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
                text = "Other Information",
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuppText
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Step header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_pro_sup),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            // Required info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuppCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Required Info",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuppText
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                PickerRow(
                    label = "Home State and District",
                    value = buildAddressDisplay(
                        form.homeProvince?.name,
                        form.homeCity?.name
                    ),
                    onClick = { viewModel.openSheet(SuppSheet.HOME_PROVINCE) }
                )
                PickerRow(
                    label = "Type of Residence",
                    value = form.addressProperty?.item,
                    onClick = { viewModel.openSheet(SuppSheet.ADDRESS_PROPERTY) }
                )
                PickerRow(
                    label = "Employment Status",
                    value = form.workType?.item,
                    onClick = { viewModel.openSheet(SuppSheet.WORK_TYPE) }
                )

                if (!form.isWorkConditionalHidden) {
                    PickerRow(
                        label = "Working Years",
                        value = form.workingTime?.item,
                        onClick = { viewModel.openSheet(SuppSheet.WORKING_TIME) }
                    )
                    PickerRow(
                        label = "Type of Jobs",
                        value = form.industry?.item,
                        onClick = { viewModel.openSheet(SuppSheet.INDUSTRY) }
                    )
                    PickerRow(
                        label = "Designation",
                        value = form.designation?.item,
                        onClick = { viewModel.openSheet(SuppSheet.DESIGNATION) }
                    )
                    PickerRow(
                        label = "Company State and District",
                        value = buildAddressDisplay(
                            form.workProvince?.name,
                            form.workCity?.name
                        ),
                        onClick = { viewModel.openSheet(SuppSheet.WORK_PROVINCE) }
                    )
                }

                PickerRow(
                    label = "Monthly Income(Rupees)",
                    value = form.monthlyIncome?.item,
                    onClick = { viewModel.openSheet(SuppSheet.MONTHLY_INCOME) }
                )
                PickerRow(
                    label = "Monthly Payday",
                    value = if (form.salaryDays.isEmpty()) null
                    else form.salaryDays.joinToString(", "),
                    isLast = true,
                    onClick = { viewModel.openSheet(SuppSheet.SALARY_DAY) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Next Step button
            val isEnabled = form.isComplete && !state.isSubmitting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isEnabled) SuppBlue else SuppDisabled)
                    .clickable(enabled = isEnabled) { viewModel.submit() },
                contentAlignment = Alignment.Center
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Next Step",
                        color = Color.White,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Bottom sheet content ────────────────────────────────────────────────────

@Composable
private fun SuppSheetContent(
    sheet: SuppSheet,
    state: SuppUiState.Ready,
    viewModel: SupplementaryInfoViewModel
) {
    val config = state.kycConfig
    when (sheet) {
        SuppSheet.HOME_PROVINCE -> AddressSheet(
            title = "Select State",
            items = state.provinces,
            selectedCode = state.formState.homeProvince?.code,
            onSelect = { viewModel.selectHomeProvince(it) }
        )
        SuppSheet.HOME_CITY -> AddressSheet(
            title = "Select City",
            items = state.homeCities,
            selectedCode = state.formState.homeCity?.code,
            onSelect = { viewModel.selectHomeCity(it) }
        )
        SuppSheet.ADDRESS_PROPERTY -> SingleSelectSheet(
            title = "Type of Residence",
            items = config?.addressProperty ?: emptyList(),
            selectedEnum = state.formState.addressProperty?.eneum,
            onSelect = { viewModel.selectAddressProperty(it) }
        )
        SuppSheet.WORK_TYPE -> SingleSelectSheet(
            title = "Employment Status",
            items = config?.workType ?: emptyList(),
            selectedEnum = state.formState.workType?.eneum,
            onSelect = { viewModel.selectWorkType(it) }
        )
        SuppSheet.WORKING_TIME -> SingleSelectSheet(
            title = "Working Years",
            items = config?.workingTime ?: emptyList(),
            selectedEnum = state.formState.workingTime?.eneum,
            onSelect = { viewModel.selectWorkingTime(it) }
        )
        SuppSheet.INDUSTRY -> SingleSelectSheet(
            title = "Type of Jobs",
            items = config?.industry ?: emptyList(),
            selectedEnum = state.formState.industry?.eneum,
            onSelect = { viewModel.selectIndustry(it) }
        )
        SuppSheet.DESIGNATION -> SingleSelectSheet(
            title = "Designation",
            items = config?.designation ?: emptyList(),
            selectedEnum = state.formState.designation?.eneum,
            onSelect = { viewModel.selectDesignation(it) }
        )
        SuppSheet.WORK_PROVINCE -> AddressSheet(
            title = "Select State",
            items = state.provinces,
            selectedCode = state.formState.workProvince?.code,
            onSelect = { viewModel.selectWorkProvince(it) }
        )
        SuppSheet.WORK_CITY -> AddressSheet(
            title = "Select City",
            items = state.workCities,
            selectedCode = state.formState.workCity?.code,
            onSelect = { viewModel.selectWorkCity(it) }
        )
        SuppSheet.MONTHLY_INCOME -> SingleSelectSheet(
            title = "Monthly Income",
            items = config?.monthlyIncome ?: emptyList(),
            selectedEnum = state.formState.monthlyIncome?.eneum,
            onSelect = { viewModel.selectMonthlyIncome(it) }
        )
        SuppSheet.SALARY_DAY -> SalaryDaySheet(
            selected = state.formState.salaryDays,
            onToggle = { viewModel.toggleSalaryDay(it) }
        )
    }
}

// ── Sheet components ────────────────────────────────────────────────────────

@Composable
private fun AddressSheet(
    title: String,
    items: List<AddrResp>,
    selectedCode: Long?,
    onSelect: (AddrResp) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuppText),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = SuppDivider)
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.height(360.dp)
            ) {
                items(items) { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(addr) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = addr.name,
                            style = TextStyle(fontSize = 15.sp, color = SuppText)
                        )
                        if (addr.code == selectedCode) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = SuppBlue)
                            )
                        }
                    }
                    HorizontalDivider(color = SuppDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SingleSelectSheet(
    title: String,
    items: List<ConfigResp>,
    selectedEnum: Long?,
    onSelect: (ConfigResp) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuppText),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = SuppDivider)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.height(360.dp)
        ) {
            items(items) { cfg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(cfg) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cfg.item,
                        style = TextStyle(fontSize = 15.sp, color = SuppText)
                    )
                    if (cfg.eneum == selectedEnum) {
                        RadioButton(
                            selected = true,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = SuppBlue)
                        )
                    }
                }
                HorizontalDivider(color = SuppDivider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SalaryDaySheet(
    selected: List<Int>,
    onToggle: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Payday",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuppText)
            )
            Text(
                text = "${selected.size}/4",
                style = TextStyle(fontSize = 13.sp, color = SuppLabel)
            )
        }
        HorizontalDivider(color = SuppDivider)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.height(360.dp)
        ) {
            items((1..31).toList()) { day ->
                val isSelected = selected.contains(day)
                val canSelect = isSelected || selected.size < 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canSelect) { onToggle(day) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day $day",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = if (canSelect) SuppText else SuppLabel
                        )
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = if (canSelect) ({ onToggle(day) }) else null
                    )
                }
                HorizontalDivider(color = SuppDivider, thickness = 0.5.dp)
            }
        }
    }
}

// ── Reusable row ────────────────────────────────────────────────────────────

@Composable
private fun PickerRow(
    label: String,
    value: String?,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp, color = SuppLabel)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value ?: "Please Select",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (value != null) SuppText else SuppLabel
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SuppLabel,
                modifier = Modifier.size(14.dp)
            )
        }
    }
    if (!isLast) HorizontalDivider(color = SuppDivider)
}

// ── Step progress bar ───────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < currentStep) SuppBlue else Color(0xFFDDDDDD))
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun buildAddressDisplay(province: String?, city: String?): String? {
    if (province == null) return null
    return if (city != null) "$province · $city" else province
}

// Alias for AddrResp used in sheet (avoids import clash)
private typealias AddrResp = com.demo.creditlimit.network.model.request2.AddrResp
private typealias ConfigResp = com.demo.creditlimit.network.model.request2.ConfigResp
