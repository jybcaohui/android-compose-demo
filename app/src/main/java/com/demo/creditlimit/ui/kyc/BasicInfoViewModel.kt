package com.demo.creditlimit.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.model.request2.BasicUpdateReqV2
import com.demo.creditlimit.network.model.request2.ConfigResp
import com.demo.creditlimit.network.model.request2.KycConfigResp
import com.demo.creditlimit.network.repository.ConfigRepository
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Form state ─────────────────────────────────────────────────────────────

data class BasicFormState(
    val panNumber: String = "",
    val maritalStatus: ConfigResp? = null,
    val education: ConfigResp? = null,
    val religion: ConfigResp? = null,
    val expectedLoanAmount: ConfigResp? = null,
    val loanPurpose: ConfigResp? = null,
    val email: String = "",
    val whatsApp: String = "",
    val facebook: String = "",
    val linkedIn: String = ""
) {
    val isPanValid: Boolean
        get() = panNumber.length == 10 &&
            panNumber.matches(Regex("[A-Za-z]{5}[0-9]{4}[A-Za-z]"))

    val isComplete: Boolean
        get() = isPanValid &&
            maritalStatus != null &&
            education != null &&
            religion != null &&
            expectedLoanAmount != null &&
            loanPurpose != null
}

// ── Sheet enum ─────────────────────────────────────────────────────────────

enum class BasicSheet { MARITAL_STATUS, EDUCATION, RELIGION, EXPECTED_LOAN_AMOUNT, LOAN_PURPOSE }

// ── UI state ───────────────────────────────────────────────────────────────

sealed class BasicUiState {
    object Loading : BasicUiState()
    data class Ready(
        val formState: BasicFormState = BasicFormState(),
        val kycConfig: KycConfigResp? = null,
        val optionalExpanded: Boolean = false,
        val activeSheet: BasicSheet? = null,
        val isSubmitting: Boolean = false,
        val errorMsg: String? = null
    ) : BasicUiState()
    object SubmitSuccess : BasicUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class BasicInfoViewModel(
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BasicUiState>(BasicUiState.Loading)
    val uiState: StateFlow<BasicUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        viewModelScope.launch { loadInitialData() }
    }

    private suspend fun loadInitialData() {
        val kycConfig = configRepository.getKycConfig()
        val savedJson = configRepository.loadBasicForm()
        val form = if (savedJson != null) {
            runCatching { gson.fromJson(savedJson, BasicFormState::class.java) }.getOrNull()
                ?: BasicFormState()
        } else {
            BasicFormState()
        }
        _uiState.value = BasicUiState.Ready(kycConfig = kycConfig, formState = form)
    }

    fun openSheet(sheet: BasicSheet) {
        ready()?.let { _uiState.value = it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        ready()?.let { _uiState.value = it.copy(activeSheet = null) }
    }

    fun toggleOptional() {
        ready()?.let { _uiState.value = it.copy(optionalExpanded = !it.optionalExpanded) }
    }

    fun updatePanNumber(pan: String) = updateFormNoClose { it.copy(panNumber = pan.uppercase()) }
    fun updateEmail(v: String) = updateFormNoClose { it.copy(email = v) }
    fun updateWhatsApp(v: String) = updateFormNoClose { it.copy(whatsApp = v) }
    fun updateFacebook(v: String) = updateFormNoClose { it.copy(facebook = v) }
    fun updateLinkedIn(v: String) = updateFormNoClose { it.copy(linkedIn = v) }

    fun selectMaritalStatus(item: ConfigResp) = updateAndSave { it.copy(maritalStatus = item) }
    fun selectEducation(item: ConfigResp) = updateAndSave { it.copy(education = item) }
    fun selectReligion(item: ConfigResp) = updateAndSave { it.copy(religion = item) }
    fun selectExpectedLoanAmount(item: ConfigResp) = updateAndSave { it.copy(expectedLoanAmount = item) }
    fun selectLoanPurpose(item: ConfigResp) = updateAndSave { it.copy(loanPurpose = item) }

    fun submit() {
        val ready = ready() ?: return
        val form = ready.formState
        if (!form.isComplete) return
        _uiState.value = ready.copy(isSubmitting = true, errorMsg = null)

        viewModelScope.launch {
            val req = BasicUpdateReqV2().apply {
                panNumber = form.panNumber
                maritalStatus = form.maritalStatus?.eneum
                education = form.education?.eneum
                religion = form.religion?.eneum
                expectedLoanAmount = form.expectedLoanAmount?.eneum ?: 0L
                loanPurpose = form.loanPurpose?.eneum
                email = form.email.ifBlank { null }
                whatsAppNumber = form.whatsApp.ifBlank { null }
                facebookAccount = form.facebook.ifBlank { null }
                linkedInAccount = form.linkedIn.ifBlank { null }
            }
            userRepository.submitBasicInfo(req).collect { result ->
                when (result) {
                    is NetworkResult.Success, is NetworkResult.Empty -> {
                        configRepository.clearBasicForm()
                        _uiState.value = BasicUiState.SubmitSuccess
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

    private fun ready(): BasicUiState.Ready? = _uiState.value as? BasicUiState.Ready

    // Updates form without closing sheet (for text inputs)
    private fun updateFormNoClose(transform: (BasicFormState) -> BasicFormState) {
        val r = ready() ?: return
        val newForm = transform(r.formState)
        _uiState.value = r.copy(formState = newForm)
        viewModelScope.launch { configRepository.saveBasicForm(gson.toJson(newForm)) }
    }

    // Updates form and closes sheet (for picker selections)
    private fun updateAndSave(transform: (BasicFormState) -> BasicFormState) {
        val r = ready() ?: return
        val newForm = transform(r.formState)
        _uiState.value = r.copy(formState = newForm, activeSheet = null)
        viewModelScope.launch { configRepository.saveBasicForm(gson.toJson(newForm)) }
    }
}
