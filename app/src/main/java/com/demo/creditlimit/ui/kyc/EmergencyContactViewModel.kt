package com.demo.creditlimit.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.model.request2.ConfigResp
import com.demo.creditlimit.network.model.request2.Emerge
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

data class ContactState(
    val name: String = "",
    val phone: String = "",
    val relation: ConfigResp? = null
) {
    // Extract valid 10-digit phone starting from first 6/7/8/9
    val phoneDigits: String
        get() {
            val idx = phone.indexOfFirst { it in "6789" }
            if (idx < 0) return ""
            return phone.substring(idx).filter { it.isDigit() }.take(10)
        }

    val isPhoneValid: Boolean get() = phoneDigits.length == 10
    val isNameValid: Boolean get() = name.isNotBlank() && name.length <= 30
    val isValid: Boolean get() = isNameValid && isPhoneValid && relation != null
}

data class EmergencyFormState(
    val contact1: ContactState = ContactState(),
    val contact2: ContactState = ContactState()
) {
    val isComplete: Boolean get() = contact1.isValid && contact2.isValid

    fun crossValidationError(): String? {
        val p1 = contact1.phoneDigits
        val p2 = contact2.phoneDigits
        if (p1.isNotEmpty() && p1 == p2) return "Two contacts cannot have the same phone number"
        if (contact1.name.isNotBlank() && contact1.name.trim() == contact2.name.trim()) {
            return "Two contacts cannot have the same name"
        }
        return null
    }
}

// ── Sheet enum ─────────────────────────────────────────────────────────────

enum class EmerSheet { RELATION_1, RELATION_2 }

// ── UI state ───────────────────────────────────────────────────────────────

sealed class EmerUiState {
    object Loading : EmerUiState()
    data class Ready(
        val formState: EmergencyFormState = EmergencyFormState(),
        val kycConfig: KycConfigResp? = null,
        val activeSheet: EmerSheet? = null,
        val isSubmitting: Boolean = false,
        val errorMsg: String? = null
    ) : EmerUiState()
    object SubmitSuccess : EmerUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class EmergencyContactViewModel(
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmerUiState>(EmerUiState.Loading)
    val uiState: StateFlow<EmerUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        viewModelScope.launch { loadInitialData() }
    }

    private suspend fun loadInitialData() {
        val kycConfig = configRepository.getKycConfig()
        val savedJson = configRepository.loadEmergencyForm()
        val form = if (savedJson != null) {
            runCatching { gson.fromJson(savedJson, EmergencyFormState::class.java) }.getOrNull()
                ?: defaultForm(kycConfig)
        } else {
            defaultForm(kycConfig)
        }
        _uiState.value = EmerUiState.Ready(kycConfig = kycConfig, formState = form)
    }

    private fun defaultForm(kycConfig: KycConfigResp?): EmergencyFormState {
        val relations = kycConfig?.relation ?: emptyList()
        return EmergencyFormState(
            contact1 = ContactState(relation = relations.firstOrNull { it.eneum == 1L }),
            contact2 = ContactState(relation = relations.firstOrNull { it.eneum == 2L })
        )
    }

    fun openSheet(sheet: EmerSheet) {
        ready()?.let { _uiState.value = it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        ready()?.let { _uiState.value = it.copy(activeSheet = null) }
    }

    fun updateContact1Name(v: String) = updateForm { it.copy(contact1 = it.contact1.copy(name = v.take(30))) }
    fun updateContact1Phone(v: String) = updateForm { it.copy(contact1 = it.contact1.copy(phone = v.filter { c -> c.isDigit() }.take(15))) }
    fun selectRelation1(item: ConfigResp) = updateAndClose { it.copy(contact1 = it.contact1.copy(relation = item)) }

    fun updateContact2Name(v: String) = updateForm { it.copy(contact2 = it.contact2.copy(name = v.take(30))) }
    fun updateContact2Phone(v: String) = updateForm { it.copy(contact2 = it.contact2.copy(phone = v.filter { c -> c.isDigit() }.take(15))) }
    fun selectRelation2(item: ConfigResp) = updateAndClose { it.copy(contact2 = it.contact2.copy(relation = item)) }

    fun submit() {
        val ready = ready() ?: return
        val form = ready.formState
        if (!form.isComplete) return
        val crossErr = form.crossValidationError()
        if (crossErr != null) {
            _uiState.value = ready.copy(errorMsg = crossErr)
            return
        }
        _uiState.value = ready.copy(isSubmitting = true, errorMsg = null)

        viewModelScope.launch {
            val contacts = listOf(
                Emerge().apply {
                    name = form.contact1.name.trim()
                    mobile_number = form.contact1.phoneDigits
                    relation = form.contact1.relation?.eneum ?: 1L
                },
                Emerge().apply {
                    name = form.contact2.name.trim()
                    mobile_number = form.contact2.phoneDigits
                    relation = form.contact2.relation?.eneum ?: 2L
                }
            )
            userRepository.submitEmergencyContacts(contacts).collect { result ->
                when (result) {
                    is NetworkResult.Success, is NetworkResult.Empty -> {
                        configRepository.clearEmergencyForm()
                        _uiState.value = EmerUiState.SubmitSuccess
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

    private fun ready(): EmerUiState.Ready? = _uiState.value as? EmerUiState.Ready

    private fun updateForm(transform: (EmergencyFormState) -> EmergencyFormState) {
        val r = ready() ?: return
        val newForm = transform(r.formState)
        _uiState.value = r.copy(formState = newForm)
        viewModelScope.launch { configRepository.saveEmergencyForm(gson.toJson(newForm)) }
    }

    private fun updateAndClose(transform: (EmergencyFormState) -> EmergencyFormState) {
        val r = ready() ?: return
        val newForm = transform(r.formState)
        _uiState.value = r.copy(formState = newForm, activeSheet = null)
        viewModelScope.launch { configRepository.saveEmergencyForm(gson.toJson(newForm)) }
    }
}
