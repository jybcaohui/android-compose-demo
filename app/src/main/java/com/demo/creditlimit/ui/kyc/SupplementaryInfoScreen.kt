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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
        KycTopBar(title = "Other Information", onBack = { navController.popBackStack() })

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

                KycPickerRow(
                    label = "Home State and District",
                    value = buildAddressDisplay(
                        form.homeProvince?.name,
                        form.homeCity?.name
                    ),
                    onClick = { viewModel.openSheet(SuppSheet.HOME_ADDRESS) }
                )
                KycPickerRow(
                    label = "Type of Residence",
                    value = form.addressProperty?.item,
                    onClick = { viewModel.openSheet(SuppSheet.ADDRESS_PROPERTY) }
                )
                KycPickerRow(
                    label = "Employment Status",
                    value = form.workType?.item,
                    onClick = { viewModel.openSheet(SuppSheet.WORK_TYPE) }
                )

                if (!form.isWorkConditionalHidden) {
                    KycPickerRow(
                        label = "Working Years",
                        value = form.workingTime?.item,
                        onClick = { viewModel.openSheet(SuppSheet.WORKING_TIME) }
                    )
                    KycPickerRow(
                        label = "Type of Jobs",
                        value = form.industry?.item,
                        onClick = { viewModel.openSheet(SuppSheet.INDUSTRY) }
                    )
                    KycPickerRow(
                        label = "Designation",
                        value = form.designation?.item,
                        onClick = { viewModel.openSheet(SuppSheet.DESIGNATION) }
                    )
                    KycPickerRow(
                        label = "Company State and District",
                        value = buildAddressDisplay(
                            form.workProvince?.name,
                            form.workCity?.name
                        ),
                        onClick = { viewModel.openSheet(SuppSheet.WORK_ADDRESS) }
                    )
                }

                KycPickerRow(
                    label = "Monthly Income(Rupees)",
                    value = form.monthlyIncome?.item,
                    onClick = { viewModel.openSheet(SuppSheet.MONTHLY_INCOME) }
                )
                KycPickerRow(
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
        SuppSheet.HOME_ADDRESS -> AddressSheet(
            provinces = state.provinces,
            cities = state.homeCities,
            isLoadingCities = state.isLoadingHomeCities,
            selectedProvinceCode = state.formState.homeProvince?.code,
            selectedCityCode = state.formState.homeCity?.code,
            onProvinceSelect = { viewModel.selectHomeProvinceInSheet(it) },
            onCitySelect = { viewModel.selectHomeCity(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.ADDRESS_PROPERTY -> KycSingleSelectSheet(
            items = config?.addressProperty ?: emptyList(),
            selectedEnum = state.formState.addressProperty?.eneum,
            onSelect = { viewModel.selectAddressProperty(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.WORK_TYPE -> KycSingleSelectSheet(
            items = config?.workType ?: emptyList(),
            selectedEnum = state.formState.workType?.eneum,
            onSelect = { viewModel.selectWorkType(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.WORKING_TIME -> KycSingleSelectSheet(
            items = config?.workingTime ?: emptyList(),
            selectedEnum = state.formState.workingTime?.eneum,
            onSelect = { viewModel.selectWorkingTime(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.INDUSTRY -> KycSingleSelectSheet(
            items = config?.industry ?: emptyList(),
            selectedEnum = state.formState.industry?.eneum,
            onSelect = { viewModel.selectIndustry(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.DESIGNATION -> KycSingleSelectSheet(
            items = config?.designation ?: emptyList(),
            selectedEnum = state.formState.designation?.eneum,
            onSelect = { viewModel.selectDesignation(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.WORK_ADDRESS -> AddressSheet(
            provinces = state.provinces,
            cities = state.workCities,
            isLoadingCities = state.isLoadingWorkCities,
            selectedProvinceCode = state.formState.workProvince?.code,
            selectedCityCode = state.formState.workCity?.code,
            onProvinceSelect = { viewModel.selectWorkProvinceInSheet(it) },
            onCitySelect = { viewModel.selectWorkCity(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.MONTHLY_INCOME -> KycSingleSelectSheet(
            items = config?.monthlyIncome ?: emptyList(),
            selectedEnum = state.formState.monthlyIncome?.eneum,
            onSelect = { viewModel.selectMonthlyIncome(it) },
            onClose = { viewModel.closeSheet() }
        )
        SuppSheet.SALARY_DAY -> SalaryDaySheet(
            initialSelected = state.formState.salaryDays,
            onConfirm = { viewModel.confirmSalaryDays(it) },
            onClose = { viewModel.closeSheet() }
        )
    }
}

// 地址弹框：两个 Tab（Select City=省 / Select Region=市），Radio 在左侧
@Composable
private fun AddressSheet(
    provinces: List<AddrResp>,
    cities: List<AddrResp>,
    isLoadingCities: Boolean,
    selectedProvinceCode: Long?,
    selectedCityCode: Long?,
    onProvinceSelect: (AddrResp) -> Unit,
    onCitySelect: (AddrResp) -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        KycSheetHeader(title = "Please select", onClose = onClose)

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0F0F0))
        ) {
            listOf("Select City", "Select Region").forEachIndexed { idx, label ->
                val isActive = activeTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isActive) SuppCard else Color.Transparent)
                        .clickable { activeTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) SuppBlue else SuppLabel
                        )
                    )
                }
                if (isActive && idx == 0) {
                    // Blue underline effect handled by color change
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // List
        if (activeTab == 0) {
            // 省列表
            AddrList(
                items = provinces,
                selectedCode = selectedProvinceCode,
                isLoading = provinces.isEmpty(),
                onSelect = { prov ->
                    onProvinceSelect(prov)
                    activeTab = 1  // 选完省自动切到市 Tab
                }
            )
        } else {
            // 市列表
            AddrList(
                items = cities,
                selectedCode = selectedCityCode,
                isLoading = isLoadingCities,
                onSelect = { onCitySelect(it) }
            )
        }
    }
}

@Composable
private fun AddrList(
    items: List<AddrResp>,
    selectedCode: Long?,
    isLoading: Boolean,
    onSelect: (AddrResp) -> Unit
) {
    if (isLoading || items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(32.dp), color = SuppBlue)
            else Text("No data", color = SuppLabel, style = TextStyle(fontSize = 14.sp))
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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = addr.code == selectedCode,
                        onClick = { onSelect(addr) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SuppBlue,
                            unselectedColor = SuppLabel
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = addr.name, style = TextStyle(fontSize = 15.sp, color = SuppText))
                }
                HorizontalDivider(color = SuppDivider, thickness = 0.5.dp)
            }
        }
    }
}

// 发薪日弹框：7列日历格，选中显示蓝色圆圈，确认后关闭
@Composable
private fun SalaryDaySheet(
    initialSelected: List<Int>,
    onConfirm: (List<Int>) -> Unit,
    onClose: () -> Unit
) {
    var pending by remember { mutableStateOf(initialSelected) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        KycSheetHeader(title = "Please select", onClose = onClose)
        HorizontalDivider(color = SuppDivider)
        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            (0 until 5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..7).forEach { col ->
                        val day = row * 7 + col
                        if (day <= 31) {
                            val isSelected = pending.contains(day)
                            val canSelect = isSelected || pending.size < 4
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSelected) SuppBlue else Color.Transparent)
                                    .clickable(enabled = canSelect) {
                                        pending = if (isSelected) {
                                            pending - day
                                        } else {
                                            (pending + day).sorted()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            canSelect -> SuppText
                                            else -> SuppLabel
                                        }
                                    )
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(44.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (pending.isNotEmpty()) SuppBlue else SuppDisabled)
                .clickable(enabled = pending.isNotEmpty()) { onConfirm(pending) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Confirm",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }
    }
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
    return if (city != null) "$province/$city" else province
}

// Alias for AddrResp used in sheet (avoids import clash)
private typealias AddrResp = com.demo.creditlimit.network.model.request2.AddrResp
private typealias ConfigResp = com.demo.creditlimit.network.model.request2.ConfigResp
