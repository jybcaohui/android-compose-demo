package com.demo.creditlimit.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.model.request2.AddrResp
import com.demo.creditlimit.network.model.request2.ConfigResp
import com.demo.creditlimit.network.model.request2.HomeAddress
import com.demo.creditlimit.network.model.request2.KycConfigResp
import com.demo.creditlimit.network.model.request2.SupplementUpdateReqV2
import com.demo.creditlimit.network.model.request2.WorkAddress
import com.demo.creditlimit.network.repository.ConfigRepository
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Form state ─────────────────────────────────────────────────────────────

data class SuppFormState(
    val homeProvince: AddrResp? = null,
    val homeCity: AddrResp? = null,
    val addressProperty: ConfigResp? = null,
    val workType: ConfigResp? = null,
    val workingTime: ConfigResp? = null,
    val industry: ConfigResp? = null,
    val designation: ConfigResp? = null,
    val workProvince: AddrResp? = null,
    val workCity: AddrResp? = null,
    val monthlyIncome: ConfigResp? = null,
    val salaryDays: List<Int> = emptyList()
) {
    // Self-employed / Unemployed → hide 4 work-related fields
    val isWorkConditionalHidden: Boolean
        get() {
            val name = workType?.item?.lowercase() ?: return false
            return name.contains("self-employed") || name.contains("unemployed")
        }

    val isComplete: Boolean
        get() {
            if (homeProvince == null || homeCity == null) return false
            if (addressProperty == null) return false
            if (workType == null) return false
            if (monthlyIncome == null) return false
            if (salaryDays.isEmpty()) return false
            if (!isWorkConditionalHidden) {
                if (workingTime == null || industry == null) return false
            }
            return true
        }
}

// ── Sheet enum ─────────────────────────────────────────────────────────────

enum class SuppSheet {
    HOME_ADDRESS,
    ADDRESS_PROPERTY, WORK_TYPE, WORKING_TIME, INDUSTRY, DESIGNATION,
    WORK_ADDRESS,
    MONTHLY_INCOME, SALARY_DAY
}

// ── UI state ───────────────────────────────────────────────────────────────

sealed class SuppUiState {
    object Loading : SuppUiState()
    data class Ready(
        val formState: SuppFormState = SuppFormState(),
        val kycConfig: KycConfigResp? = null,
        val provinces: List<AddrResp> = emptyList(),
        val homeCities: List<AddrResp> = emptyList(),
        val workCities: List<AddrResp> = emptyList(),
        val isLoadingHomeCities: Boolean = false,
        val isLoadingWorkCities: Boolean = false,
        val activeSheet: SuppSheet? = null,
        val isSubmitting: Boolean = false,
        val errorMsg: String? = null
    ) : SuppUiState()
    object SubmitSuccess : SuppUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class SupplementaryInfoViewModel(
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SuppUiState>(SuppUiState.Loading)
    val uiState: StateFlow<SuppUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        viewModelScope.launch { loadInitialData() }
    }

    private suspend fun loadInitialData() {
        val kycConfig = configRepository.getKycConfig()
        val savedJson = configRepository.loadSuppForm()
        val savedForm = savedJson?.let {
            runCatching { gson.fromJson(it, SuppFormState::class.java) }.getOrNull()
        } ?: SuppFormState()
        _uiState.value = SuppUiState.Ready(
            kycConfig = kycConfig,
            formState = savedForm
        )
    }

    // ── Sheet control ──────────────────────────────────────────────────────

    fun openSheet(sheet: SuppSheet) {
        val ready = ready() ?: return
        _uiState.value = ready.copy(activeSheet = sheet)
        when (sheet) {
            SuppSheet.HOME_ADDRESS -> {
                loadProvinces()
                ready.formState.homeProvince?.let { loadHomeCities(it.code) }
            }
            SuppSheet.WORK_ADDRESS -> {
                loadProvinces()
                ready.formState.workProvince?.let { loadWorkCities(it.code) }
            }
            else -> Unit
        }
    }

    fun closeSheet() {
        ready()?.let { _uiState.value = it.copy(activeSheet = null) }
    }

    // ── Address loading ────────────────────────────────────────────────────

    private fun loadProvinces() {
        val ready = ready() ?: return
        if (ready.provinces.isNotEmpty()) return
        viewModelScope.launch {
            val list = configRepository.getProvinces()
            ready()?.let { _uiState.value = it.copy(provinces = list) }
        }
    }

    private fun loadHomeCities(provinceCode: Long) {
        ready()?.let { _uiState.value = it.copy(homeCities = emptyList(), isLoadingHomeCities = true) }
        viewModelScope.launch {
            val list = configRepository.getCities(provinceCode)
            ready()?.let { _uiState.value = it.copy(homeCities = list, isLoadingHomeCities = false) }
        }
    }

    private fun loadWorkCities(provinceCode: Long) {
        ready()?.let { _uiState.value = it.copy(workCities = emptyList(), isLoadingWorkCities = true) }
        viewModelScope.launch {
            val list = configRepository.getCities(provinceCode)
            ready()?.let { _uiState.value = it.copy(workCities = list, isLoadingWorkCities = false) }
        }
    }

    // ── Field selection ────────────────────────────────────────────────────

    // 在弹框内选省：不关闭弹框，清空市，加载市列表
    fun selectHomeProvinceInSheet(province: AddrResp) {
        val r = ready() ?: return
        val newForm = r.formState.copy(homeProvince = province, homeCity = null)
        _uiState.value = r.copy(formState = newForm)
        loadHomeCities(province.code)
        viewModelScope.launch { configRepository.saveSuppForm(gson.toJson(newForm)) }
    }

    fun selectHomeProvince(province: AddrResp) = selectHomeProvinceInSheet(province)

    fun selectHomeCity(city: AddrResp) = updateAndSave { form ->
        form.copy(homeCity = city)
    }

    fun selectAddressProperty(item: ConfigResp) = updateAndSave { form ->
        form.copy(addressProperty = item)
    }

    fun selectWorkType(item: ConfigResp) = updateAndSave { form ->
        form.copy(
            workType = item,
            workingTime = if (form.workType?.eneum != item.eneum) null else form.workingTime,
            industry = if (form.workType?.eneum != item.eneum) null else form.industry,
            designation = if (form.workType?.eneum != item.eneum) null else form.designation,
            workProvince = if (form.workType?.eneum != item.eneum) null else form.workProvince,
            workCity = if (form.workType?.eneum != item.eneum) null else form.workCity
        )
    }

    fun selectWorkingTime(item: ConfigResp) = updateAndSave { form ->
        form.copy(workingTime = item)
    }

    fun selectIndustry(item: ConfigResp) = updateAndSave { form ->
        form.copy(industry = item)
    }

    fun selectDesignation(item: ConfigResp) = updateAndSave { form ->
        form.copy(designation = item)
    }

    fun selectWorkProvinceInSheet(province: AddrResp) {
        val r = ready() ?: return
        val newForm = r.formState.copy(workProvince = province, workCity = null)
        _uiState.value = r.copy(formState = newForm)
        loadWorkCities(province.code)
        viewModelScope.launch { configRepository.saveSuppForm(gson.toJson(newForm)) }
    }

    fun selectWorkProvince(province: AddrResp) = selectWorkProvinceInSheet(province)

    fun selectWorkCity(city: AddrResp) = updateAndSave { form ->
        form.copy(workCity = city)
    }

    fun selectMonthlyIncome(item: ConfigResp) = updateAndSave { form ->
        form.copy(monthlyIncome = item)
    }

    fun confirmSalaryDays(days: List<Int>) = updateAndSave { form ->
        form.copy(salaryDays = days.sorted())
    }

    // ── Submit ─────────────────────────────────────────────────────────────

    fun submit() {
        val ready = ready() ?: return
        val form = ready.formState
        if (!form.isComplete) return
        _uiState.value = ready.copy(isSubmitting = true, errorMsg = null)

        viewModelScope.launch {
            val req = SupplementUpdateReqV2().apply {
                homeAddress = HomeAddress().apply {
                    province = form.homeProvince?.code?.toString() ?: ""
                    city = form.homeCity?.code?.toString() ?: ""
                }
                addressProperty = form.addressProperty?.eneum ?: 0L
                workType = form.workType?.eneum ?: 0L
                monthlyIncome = form.monthlyIncome?.eneum ?: 0L
                salaryDay = form.salaryDays.joinToString(",")
                if (!form.isWorkConditionalHidden) {
                    workingTime = form.workingTime?.eneum ?: 0L
                    industry = form.industry?.eneum ?: 0L
                    designation = form.designation?.eneum ?: 0L
                    workAddress = WorkAddress().apply {
                        province = form.workProvince?.code?.toString() ?: ""
                        city = form.workCity?.code?.toString() ?: ""
                    }
                }
            }

            userRepository.submitSuppInfo(req).collect { result ->
                when (result) {
                    is NetworkResult.Success, is NetworkResult.Empty -> {
                        configRepository.clearSuppForm()
                        _uiState.value = SuppUiState.SubmitSuccess
                    }
                    is NetworkResult.Error -> {
                        ready()?.let {
                            _uiState.value = it.copy(
                                isSubmitting = false,
                                errorMsg = result.exception.message ?: "Submit failed"
                            )
                        }
                    }
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun clearError() {
        ready()?.let { _uiState.value = it.copy(errorMsg = null) }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun ready(): SuppUiState.Ready? = _uiState.value as? SuppUiState.Ready

    private fun updateAndSave(transform: (SuppFormState) -> SuppFormState) {
        val r = ready() ?: return
        val newForm = transform(r.formState)
        _uiState.value = r.copy(formState = newForm, activeSheet = null)
        viewModelScope.launch {
            configRepository.saveSuppForm(gson.toJson(newForm))
        }
    }
}
